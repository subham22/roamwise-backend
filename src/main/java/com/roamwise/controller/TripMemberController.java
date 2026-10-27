package com.roamwise.controller;

import com.roamwise.dto.InviteMemberRequest;
import com.roamwise.dto.TripMemberResponse;
import com.roamwise.dto.TripResponse;
import com.roamwise.dto.VoteSummary;
import com.roamwise.entity.*;
import com.roamwise.repository.ActivityRepository;
import com.roamwise.repository.ActivityVoteRepository;
import com.roamwise.repository.TripMemberRepository;
import com.roamwise.repository.TripRepository;
import com.roamwise.service.EmailService;
import com.roamwise.service.TripAccessService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Group trips: invite people by email, list/remove members, and vote on activities. */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class TripMemberController {

    private final TripRepository tripRepository;
    private final TripMemberRepository memberRepository;
    private final ActivityRepository activityRepository;
    private final ActivityVoteRepository voteRepository;
    private final TripAccessService tripAccess;
    private final EmailService emailService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @GetMapping("/trips/{tripId}/members")
    public List<TripMemberResponse> members(@PathVariable Integer tripId, @AuthenticationPrincipal User currentUser) {
        Trip trip = findTrip(tripId);
        tripAccess.require(trip, currentUser.getId(), TripRole.VIEWER);
        List<TripMemberResponse> result = new ArrayList<>();
        result.add(new TripMemberResponse(null, trip.getUser().getEmail(), TripRole.OWNER.name()));
        memberRepository.findByTripOrderByCreatedAtAsc(trip)
                .forEach(m -> result.add(new TripMemberResponse(m.getId(), m.getEmail(), m.getRole().name())));
        return result;
    }

    @PostMapping("/trips/{tripId}/members")
    public ResponseEntity<TripMemberResponse> invite(@PathVariable Integer tripId,
                                                     @Valid @RequestBody InviteMemberRequest request,
                                                     @AuthenticationPrincipal User currentUser) throws MessagingException {
        Trip trip = findTrip(tripId);
        tripAccess.require(trip, currentUser.getId(), TripRole.OWNER);
        if (request.role() == TripRole.OWNER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invite people as editor or viewer");
        }
        String email = request.email().trim().toLowerCase();
        if (email.equalsIgnoreCase(trip.getUser().getEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You already own this trip");
        }

        // re-inviting the same person just updates their role, without another email
        TripMember member = memberRepository.findByTripAndEmail(trip, email).orElse(null);
        boolean isNew = member == null;
        if (isNew) {
            member = new TripMember();
            member.setTrip(trip);
            member.setEmail(email);
            member.setCreatedAt(LocalDateTime.now());
        }
        member.setRole(request.role());
        TripMember saved = memberRepository.save(member);

        if (isNew) {
            emailService.sendTripInvite(email, trip.getDestination(), frontendUrl + "/app/trips/" + trip.getTripId(),
                    currentUser.getEmail(), request.role() == TripRole.EDITOR);
        }
        return ResponseEntity.status(isNew ? HttpStatus.CREATED : HttpStatus.OK)
                .body(new TripMemberResponse(saved.getId(), saved.getEmail(), saved.getRole().name()));
    }

    /** The owner can remove anyone; a member can remove themselves (leave the trip). */
    @DeleteMapping("/trips/{tripId}/members/{memberId}")
    public ResponseEntity<Void> remove(@PathVariable Integer tripId, @PathVariable Integer memberId,
                                       @AuthenticationPrincipal User currentUser) {
        Trip trip = findTrip(tripId);
        TripRole role = tripAccess.require(trip, currentUser.getId(), TripRole.VIEWER);
        TripMember member = memberRepository.findById(memberId)
                .filter(m -> m.getTrip().getTripId().equals(trip.getTripId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        boolean leavingSelf = member.getEmail().equalsIgnoreCase(currentUser.getEmail());
        if (role != TripRole.OWNER && !leavingSelf) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the trip owner can remove people");
        }
        memberRepository.delete(member);
        return ResponseEntity.noContent().build();
    }

    /** Trips other people invited the caller to (summaries, like the dashboard list). */
    @GetMapping("/trips/shared-with-me")
    public List<TripResponse> sharedWithMe(@AuthenticationPrincipal User currentUser) {
        return memberRepository.findByEmail(currentUser.getEmail().toLowerCase()).stream()
                .map(m -> {
                    Trip t = m.getTrip();
                    TripResponse r = new TripResponse();
                    r.setTripId(t.getTripId());
                    r.setDestination(t.getDestination());
                    r.setOrigin(t.getOrigin());
                    r.setStartDate(t.getStartDate());
                    r.setEndDate(t.getEndDate());
                    r.setBudget(t.getBudget());
                    r.setMyRole(m.getRole().name());
                    return r;
                })
                .toList();
    }

    @GetMapping("/trips/{tripId}/votes")
    public Map<Integer, VoteSummary> votes(@PathVariable Integer tripId, @AuthenticationPrincipal User currentUser) {
        Trip trip = findTrip(tripId);
        tripAccess.require(trip, currentUser.getId(), TripRole.VIEWER);
        Map<Integer, int[]> tally = new HashMap<>(); // activityId -> {up, down, mine}
        for (ActivityVote vote : voteRepository.findByActivityDayTrip(trip)) {
            int[] t = tally.computeIfAbsent(vote.getActivity().getId(), k -> new int[3]);
            if (vote.getValue() > 0) t[0]++; else t[1]++;
            if (vote.getUser().getId().equals(currentUser.getId())) t[2] = vote.getValue();
        }
        Map<Integer, VoteSummary> result = new HashMap<>();
        tally.forEach((id, t) -> result.put(id, new VoteSummary(t[0], t[1], t[2])));
        return result;
    }

    public record VoteRequest(int value) {}

    /** value 1 = thumbs up, -1 = thumbs down, 0 = clear my vote. Any member (viewers too) can vote. */
    @PostMapping("/activities/{activityId}/vote")
    @Transactional
    public ResponseEntity<Void> vote(@PathVariable Integer activityId, @RequestBody VoteRequest request,
                                     @AuthenticationPrincipal User currentUser) {
        if (request.value() < -1 || request.value() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vote must be -1, 0 or 1");
        }
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Activity not found"));
        tripAccess.require(activity.getDay().getTrip(), currentUser.getId(), TripRole.VIEWER);

        ActivityVote existing = voteRepository.findByActivityAndUser(activity, currentUser).orElse(null);
        if (request.value() == 0) {
            if (existing != null) voteRepository.delete(existing);
        } else {
            ActivityVote vote = existing != null ? existing : new ActivityVote();
            vote.setActivity(activity);
            vote.setUser(currentUser);
            vote.setValue(request.value());
            voteRepository.save(vote);
        }
        return ResponseEntity.noContent().build();
    }

    private Trip findTrip(Integer tripId) {
        return tripRepository.findById(tripId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found"));
    }
}

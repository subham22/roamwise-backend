package com.roamwise.service;

import com.roamwise.entity.Trip;
import com.roamwise.entity.TripRole;
import com.roamwise.entity.User;
import com.roamwise.repository.TripMemberRepository;
import com.roamwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Single place that decides who may see or change a trip. */
@Service
@RequiredArgsConstructor
public class TripAccessService {

    private final TripMemberRepository memberRepository;
    private final UserRepository userRepository;

    /** The caller's role on the trip, or null if they have no access. */
    public @Nullable TripRole roleOf(Trip trip, Integer userId) {
        if (trip.getUser().getId().equals(userId)) {
            return TripRole.OWNER;
        }
        String email = userRepository.findById(userId).map(User::getEmail).orElse(null);
        if (email == null) {
            return null;
        }
        return memberRepository.findByTripAndEmail(trip, email.toLowerCase())
                .map(member -> member.getRole())
                .orElse(null);
    }

    /** Throws 404 for strangers (don't reveal the trip exists) and 403 for members without enough rights. */
    public TripRole require(Trip trip, Integer userId, TripRole needed) {
        TripRole role = roleOf(trip, userId);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trip not found");
        }
        if (!role.atLeast(needed)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    needed == TripRole.OWNER ? "Only the trip owner can do that" : "You can view this trip but not edit it");
        }
        return role;
    }
}

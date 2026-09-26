package com.roamwise.controller;

import com.roamwise.dto.ActivityResponse;
import com.roamwise.dto.CreateActivityRequest;
import com.roamwise.dto.TripResponse;
import com.roamwise.entity.Trip;
import com.roamwise.entity.User;
import com.roamwise.service.ActivityService;
import com.roamwise.service.DirectionsService;
import com.roamwise.service.PlacesService;
import com.roamwise.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;
    private final DirectionsService directionsService;
    private final PlacesService placesService;
    private final TripService tripService;

    @GetMapping("/shared/trips/{shareToken}")
    public ResponseEntity<TripResponse> getSharedTrip(@PathVariable String shareToken) {
        return ResponseEntity.ok().body(tripService.getSharedTrip(shareToken));
    }


    @GetMapping("/places/photo")
    public ResponseEntity<byte[]> getPhoto(@RequestParam String photoReference) {
        byte[] imageBytes = placesService.getPhoto(photoReference);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .body(imageBytes);
    }

    @GetMapping("/places/autocomplete")
    public ResponseEntity<List<String>> autocomplete(@RequestParam String input) {
        return ResponseEntity.ok(placesService.autocomplete(input));
    }


    @PostMapping("/days/{dayId}/activities")
    public ResponseEntity<ActivityResponse> createActivity(@PathVariable Integer dayId, @Valid @RequestBody CreateActivityRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.createActivity(dayId, request, currentUser.getId()));
    }

    @PutMapping("/days/{dayId}/activities/reorder")
    public ResponseEntity<Void> reorderActivities(@PathVariable Integer dayId, @RequestBody List<Integer> orderedActivityIds, @AuthenticationPrincipal User currentUser) {
        activityService.reorderActivities(dayId, orderedActivityIds, currentUser.getId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/activities/{id}")
    public ResponseEntity<ActivityResponse> updateActivity(@PathVariable Integer id, @Valid @RequestBody CreateActivityRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok().body(activityService.updateActivity(id, request, currentUser.getId()));
    }

    @DeleteMapping("/activities/{id}")
    public ResponseEntity<Void> deleteActivity(@PathVariable Integer id, @AuthenticationPrincipal User currentUser) {
        activityService.deleteActivity(id, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/activities/travel-time")
    public ResponseEntity<Map<String, String>> getTravelTime(
            @RequestParam String origin,
            @RequestParam String destination) {
        String travelTime = directionsService.getTravelTime(origin, destination);
        return ResponseEntity.ok(Map.of("travelTime", travelTime));
    }

}

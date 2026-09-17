package com.roamwise.controller;

import com.roamwise.dto.ActivityResponse;
import com.roamwise.dto.CreateActivityRequest;
import com.roamwise.entity.User;
import com.roamwise.service.ActivityService;
import com.roamwise.service.PlacesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @PostMapping("/days/{dayId}/activities")
    public ResponseEntity<ActivityResponse> createActivity(@PathVariable Integer dayId, @Valid @RequestBody CreateActivityRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activityService.createActivity(dayId, request, currentUser.getId()));
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

}

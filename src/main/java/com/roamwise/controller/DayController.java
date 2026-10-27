package com.roamwise.controller;


import com.roamwise.dto.CreateDayRequest;
import com.roamwise.dto.DayResponse;
import com.roamwise.dto.FeasibilityIssue;
import com.roamwise.entity.Day;
import com.roamwise.entity.User;
import com.roamwise.repository.DayRepository;
import com.roamwise.service.DayService;
import com.roamwise.service.FeasibilityService;
import jakarta.validation.Valid;
import com.roamwise.entity.TripRole;
import com.roamwise.service.TripAccessService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class DayController {

    private  final DayService dayService;
    private final FeasibilityService feasibilityService;
    private final DayRepository dayRepository;
    private final TripAccessService tripAccess;

    @PostMapping("trips/{tripId}/days")
    public ResponseEntity<DayResponse> createDay(@PathVariable Integer tripId, @Valid @RequestBody CreateDayRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED).body(dayService.addDay(tripId, request, currentUser.getId()));
    }

    @PutMapping("days/{dayId}")
    public ResponseEntity<DayResponse> updateDay(@PathVariable Integer dayId, @Valid @RequestBody CreateDayRequest request, @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok().body(dayService.updateDay(dayId, request, currentUser.getId()));
    }

    @DeleteMapping("days/{dayId}")
    public ResponseEntity<Void> deleteDay(@PathVariable Integer dayId, @AuthenticationPrincipal User currentUser) {
    dayService.deleteDay(dayId, currentUser.getId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/days/{dayId}/feasibility")
    public ResponseEntity<List<FeasibilityIssue>> getDayFeasibility(@PathVariable Integer dayId, @AuthenticationPrincipal User currentUser) {
        Day day = dayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found"));

        tripAccess.require(day.getTrip(), currentUser.getId(), TripRole.VIEWER);

        List<FeasibilityIssue> issues = feasibilityService.checkDayFeasibility(day);
        return ResponseEntity.ok(issues);
    }


}
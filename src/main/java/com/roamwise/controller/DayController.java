package com.roamwise.controller;


import com.roamwise.dto.CreateDayRequest;
import com.roamwise.dto.DayResponse;
import com.roamwise.entity.User;
import com.roamwise.service.DayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/")
@RequiredArgsConstructor
public class DayController {

    private  final DayService dayService;

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


}
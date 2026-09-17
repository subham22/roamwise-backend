package com.roamwise.controller;

import com.roamwise.dto.CreateTripRequest;
import com.roamwise.dto.GenerateItineraryRequest;
import com.roamwise.dto.TripResponse;
import com.roamwise.entity.User;
import com.roamwise.service.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
@RequiredArgsConstructor
public class TripController {

    private final TripService tripService;

    @PostMapping
    public ResponseEntity<TripResponse> createTrip(@Valid @RequestBody CreateTripRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TripResponse tripResponse = tripService.createTrip(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(tripResponse);

    }

    @PostMapping("/generate")
    public  ResponseEntity<TripResponse> createAITrip(@Valid @RequestBody GenerateItineraryRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        TripResponse tripResponse = tripService.createAITrip(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(tripResponse);
    }

    @GetMapping
    public ResponseEntity<List<TripResponse>> getAllTrips(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok().body(tripService.getMyTrips(user.getId()));
    }

    @GetMapping("{tripId}")
    public ResponseEntity<TripResponse> getTripById(@PathVariable Integer tripId, @AuthenticationPrincipal User currentUser){
        return ResponseEntity.ok().body(tripService.getTripById(tripId, currentUser.getId()));
    }


}

package com.roamwise.service;

import com.roamwise.dto.*;
import com.roamwise.dto.ai_response.GeneratedItinerary;
import com.roamwise.entity.Activity;
import com.roamwise.entity.Day;
import com.roamwise.entity.Trip;
import com.roamwise.entity.User;
import com.roamwise.repository.TripRepository;
import com.roamwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final ItineraryGenerationService itineraryGenerationService;

    public TripResponse createAITrip(GenerateItineraryRequest request, Integer userId) {

        CreateTripRequest tripRequest = new CreateTripRequest();
        tripRequest.setBudget(request.getBudget());
        tripRequest.setDestination(request.getDestination());
        tripRequest.setOrigin(request.getOrigin());
        tripRequest.setStartDate(request.getStartDate());
        tripRequest.setEndDate(request.getEndDate());

        TripResponse tripResponse = createTrip(tripRequest, userId);
        Trip trip = tripRepository.findById(tripResponse.getTripId()).orElseThrow(() -> new RuntimeException("Trip doesn;t exist"));

        itineraryGenerationService.generateItinerary(trip, request.getInterest());
        return getTripById(trip.getTripId(), userId);

    }

    public TripResponse createTrip(CreateTripRequest request, Integer userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Trip trip = new Trip();
        trip.setUser(user);
        trip.setBudget(request.getBudget());
        trip.setDestination(request.getDestination());
        trip.setOrigin(request.getOrigin());
        trip.setStartDate(request.getStartDate());
        trip.setEndDate(request.getEndDate());
        Trip response = tripRepository.save(trip);

        TripResponse tripResponse = new TripResponse();

        tripResponse.setTripId(response.getTripId());
        tripResponse.setBudget(response.getBudget());
        tripResponse.setDestination(response.getDestination());
        tripResponse.setOrigin(response.getOrigin());
        tripResponse.setStartDate(response.getStartDate());
        tripResponse.setEndDate(response.getEndDate());
        return tripResponse;
    }

    public List<TripResponse> getMyTrips(Integer currentUserId) {
        User user = userRepository.findById(currentUserId).orElseThrow(() -> new RuntimeException("User Doesn't Exist"));
        return tripRepository.findByUser(user).stream().map(this::convertToTripResponse).toList();
    }

    public TripResponse getTripById(Integer tripId, Integer currentUserId) {
    Trip trip = tripRepository.findById(tripId).orElseThrow(() -> new RuntimeException("Trip Doesn't Exist"));
        if (!trip.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Trip doesn't belong to current user");
        }

        TripResponse tripResponse =  convertToTripResponse(trip);
        List<DayResponse> dayResponses = trip.getDays().stream().map(this::convertToDayResponse).toList();

        tripResponse.setDays(dayResponses);
        return tripResponse;

    }

    private TripResponse convertToTripResponse(Trip trip) {
        TripResponse response = new TripResponse();
        response.setTripId(trip.getTripId());
        response.setBudget(trip.getBudget());
        response.setOrigin(trip.getOrigin());
        response.setDestination(trip.getDestination());
        response.setStartDate(trip.getStartDate());
        response.setEndDate(trip.getEndDate());
        return response;
    }

    private DayResponse convertToDayResponse(Day day) {
        DayResponse dayResponse = new DayResponse();
        dayResponse.setId(day.getId());
        dayResponse.setDayDate(day.getDayDate());
        dayResponse.setDayNo(day.getDayNumber());

        List<ActivityResponse> activityResponses = day.getActivities().stream().map(this::converToActivityResponse).toList();
        dayResponse.setActivities(activityResponses);
        return dayResponse;
    }

    private ActivityResponse converToActivityResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setNotes(activity.getNotes());
        response.setName(activity.getName());
        response.setSequence(activity.getSequence());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        return response;
    }


}

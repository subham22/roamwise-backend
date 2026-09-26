package com.roamwise.service;

import com.roamwise.dto.*;
import com.roamwise.dto.accomodation.AccommodationResponse;
import com.roamwise.dto.ai_response.GeneratedItinerary;
import com.roamwise.entity.*;
import com.roamwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TripService {

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final DayRepository dayRepository;
    private final ActivityRepository activityRepository;
    private final ItineraryGenerationService itineraryGenerationService;
    private final JobRepository jobRepository;
    private final AccommodationRepository accommodationRepository;
    private final ReplanProposalRepository replanProposalRepository;
    private final ObjectMapper objectMapper;

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

    public Integer startAIGeneration(GenerateItineraryRequest request, Integer userId) {
        CreateTripRequest tripRequest = new CreateTripRequest();
        tripRequest.setBudget(request.getBudget());
        tripRequest.setDestination(request.getDestination());
        tripRequest.setOrigin(request.getOrigin());
        tripRequest.setStartDate(request.getStartDate());
        tripRequest.setEndDate(request.getEndDate());

        TripResponse tripResponse = createTrip(tripRequest, userId);

        Trip trip = tripRepository.findById(tripResponse.getTripId()).orElseThrow(() -> new RuntimeException("Trip doesnt found"));

        Job job = new Job();
        job.setStatus(JobStatus.PENDING);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        Job savedJob = jobRepository.save(job);
        itineraryGenerationService.generateItineraryAsync(savedJob.getId(), trip, request.getInterest());
        return savedJob.getId();


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

        TripResponse tripResponse = convertToTripResponse(trip);
        List<Day> days = dayRepository.findByTrip(trip); // direct query, bypasses the stale in-memory collection
        List<DayResponse> dayResponses = days.stream().map(this::convertToDayResponse).toList();
        List<Accommodation> accommodations = accommodationRepository.findByTrip(trip);
        List<AccommodationResponse> accommodationResponses = accommodations.stream().map(this::convertToAccomodationResponse).toList();

        tripResponse.setAccommodations(accommodationResponses);
        tripResponse.setDays(dayResponses);
        return tripResponse;
    }

    @Transactional
    public void confirmReplan(Integer proposalId, Integer userId) {

        ReplanProposal proposal = replanProposalRepository.findById(proposalId)
                .orElseThrow(() -> new RuntimeException("Proposal not found"));

        if (!proposal.getTrip().getUser().getId().equals(userId)) {
            throw new RuntimeException("Not your proposal");
        }

        Trip trip = proposal.getTrip();

        // clear existing data before applying the new plan
        List<Day> oldDays = dayRepository.findByTrip(trip);
        for (Day day : oldDays) {
            activityRepository.deleteAll(activityRepository.findByDay(day));
        }
        dayRepository.deleteAll(oldDays);
        accommodationRepository.deleteAll(accommodationRepository.findByTrip(trip));

        List<PlaceResult> places = objectMapper.readValue(proposal.getPlacesJson(), new TypeReference<List<PlaceResult>>() {});
        GeneratedItinerary itinerary = objectMapper.readValue(proposal.getProposedJson(), GeneratedItinerary.class);

        itineraryGenerationService.parseAndSaveItinerary(itinerary, trip, places);

        replanProposalRepository.delete(proposal);
    }


    private TripResponse convertToTripResponse(Trip trip) {
        TripResponse response = new TripResponse();
        response.setTripId(trip.getTripId());
        response.setBudget(trip.getBudget());
        response.setOrigin(trip.getOrigin());
        response.setDestination(trip.getDestination());
        response.setStartDate(trip.getStartDate());
        response.setEndDate(trip.getEndDate());
        response.setBudgetStayPct(trip.getBudgetStayPct());
        response.setBudgetFoodPct(trip.getBudgetFoodPct());
        response.setBudgetActivitiesPct(trip.getBudgetActivitiesPct());
        response.setBudgetTransportPct(trip.getBudgetTransportPct());
        response.setTravelModeSuggestion(trip.getTravelModeSuggestion());
        return response;
    }

    private DayResponse convertToDayResponse(Day day) {
        DayResponse dayResponse = new DayResponse();
        dayResponse.setId(day.getId());
        dayResponse.setDayDate(day.getDayDate());
        dayResponse.setDayNo(day.getDayNumber());

        List<Activity> activities = activityRepository.findByDay(day);
        List<ActivityResponse> activityResponses = activities.stream().map(this::converToActivityResponse).toList();
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
        response.setLatitude(activity.getLatitude());
        response.setLongitude(activity.getLongitude());
        response.setIsHiddenGem(activity.getIsHiddenGem());
        response.setPhotoReference(activity.getPhotoReference());
    response.setReviewSnippet(activity.getReviewSnippet());
        return response;
    }

    private AccommodationResponse convertToAccomodationResponse(Accommodation accommodation) {
        AccommodationResponse accommodationResponse = new AccommodationResponse();
        accommodationResponse.setCheckInDate(accommodation.getCheckInDate());
        accommodationResponse.setCheckOutDate(accommodation.getCheckOutDate());
        accommodationResponse.setHotelName(accommodation.getHotelName());
        accommodationResponse.setPricePerNight(accommodation.getPricePerNight());
        accommodationResponse.setNotes(accommodation.getNotes());
        accommodationResponse.setId(accommodation.getId());
        return accommodationResponse;
    }


    public @Nullable TripResponse getSharedTrip(String sharedToken) {
        Trip trip = tripRepository.findByShareToken(sharedToken).orElseThrow(() -> new RuntimeException("Trip Doesn't Exist"));
        TripResponse tripResponse = convertToTripResponse(trip);
        List<Day> days = dayRepository.findByTrip(trip); // direct query, bypasses the stale in-memory collection
        List<DayResponse> dayResponses = days.stream().map(this::convertToDayResponse).toList();
        List<Accommodation> accommodations = accommodationRepository.findByTrip(trip);
        List<AccommodationResponse> accommodationResponses = accommodations.stream().map(this::convertToAccomodationResponse).toList();

        tripResponse.setAccommodations(accommodationResponses);
        tripResponse.setDays(dayResponses);
        return tripResponse;

    }

    public String generateShareLink(Integer tripId, Integer currentUserId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new RuntimeException("Trip doesn't exist"));

        if (!trip.getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Trip doesn't belong to current user");
        }

        if (trip.getShareToken() == null) {
            trip.setShareToken(UUID.randomUUID().toString());
            tripRepository.save(trip);
        }

        return frontendUrl + "/shared/trips/" + trip.getShareToken();
    }
}

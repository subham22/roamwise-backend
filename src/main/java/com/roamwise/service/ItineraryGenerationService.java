package com.roamwise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roamwise.dto.ActivityResponse;
import com.roamwise.dto.DayResponse;
import com.roamwise.dto.PlaceResult;
import com.roamwise.dto.TripResponse;
import com.roamwise.dto.accomodation.AccommodationResponse;
import com.roamwise.dto.accomodation.GeneratedAccommodation;
import com.roamwise.dto.ai_response.GeneratedActivity;
import com.roamwise.dto.ai_response.GeneratedDay;
import com.roamwise.dto.ai_response.GeneratedItinerary;
import com.roamwise.entity.*;
import com.roamwise.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ItineraryGenerationService {

    private final ObjectMapper objectMapper;
    private final PlacesService placesService;
    private final OpenAiService openAiService;
    private final DayRepository dayRepository;
    private final ActivityRepository activityRepository;
    private final JobRepository jobRepository;
    private  final WeatherService weatherService;
    private final AccommodationRepository accommodationRepository;
    private final TripRepository tripRepository;


    @Async
    public void generateItineraryAsync(Integer jobId, Trip trip, String interests) {
        Job job = jobRepository.findById(jobId).orElseThrow(() -> new RuntimeException("Job doesnt exist"));
        job.setStatus(JobStatus.RUNNING);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
       try {
           generateItinerary(trip, interests);
           job.setStatus(JobStatus.DONE);
           job.setResultTripId(trip.getTripId());
       } catch(Exception e) {
           job.setStatus(JobStatus.FAILED);
           job.setErrorMessage(e.getMessage());
       }
       job.setUpdatedAt(LocalDateTime.now());
       jobRepository.save(job);

    }

    private String formatWeatherForPrompt(Trip trip) {
        StringBuilder sb = new StringBuilder();
        LocalDate current = trip.getStartDate();
        while (!current.isAfter(trip.getEndDate())) {
            String weather = weatherService.getWeather(current, trip.getDestination());
            sb.append("- ").append(current).append(": ").append(weather).append("\n");
            current = current.plusDays(1);
        }
        return sb.toString();
    }

    public List<String> diffItineraries(TripResponse oldTrip, GeneratedItinerary newTrip) {
        List<String> changes = new ArrayList<>();
        Set<String> oldNames = oldTrip.getDays().stream()
                .flatMap(d -> d.getActivities().stream())
                        .map(ActivityResponse:: getName)
                        .collect(Collectors.toSet());

        Set<String> newNames = newTrip.days().stream()
                .flatMap(d -> d.activities().stream())
                .map(GeneratedActivity::name)
                .collect(Collectors.toSet());

        for (String removed: oldNames) {
            if (!newNames.contains(removed)) changes.add("Removed: " + removed);
        }

        for (String added : newNames) {
            if (!oldNames.contains(added)) changes.add("Added: " + added);
        }

        Set<String> oldHotels = oldTrip.getAccommodations().stream()
                .map(AccommodationResponse::getHotelName)
                .collect(Collectors.toSet());
        Set<String> newHotels = newTrip.accommodations().stream()
                .map(GeneratedAccommodation::hotelName)
                .collect(Collectors.toSet());

        for (String removed : oldHotels) {
            if (!newHotels.contains(removed)) changes.add("Accommodation changed from: " + removed);
        }
        for (String added : newHotels) {
            if (!oldHotels.contains(added)) changes.add("Accommodation changed to: " + added);
        }

        // travel mode — only worth flagging if it's genuinely different text
        if (oldTrip.getTravelModeSuggestion() != null
                && !oldTrip.getTravelModeSuggestion().equals(newTrip.travelModeSuggestion())) {
            changes.add("Travel suggestion updated: " + newTrip.travelModeSuggestion());
        }
        return changes;


    }

    public String buildReplanPrompt(TripResponse trip, Integer budgetDelta, String interest) {
        StringBuilder currentItinerary = new StringBuilder();
        for (DayResponse day : trip.getDays()) {
            currentItinerary.append("Day ").append(day.getDayNo()).append(":\n");
            for (ActivityResponse activity : day.getActivities()) {
                currentItinerary.append("  - ").append(activity.getStartTime()).append("-").append(activity.getEndTime())
                        .append(" ").append(activity.getName()).append("\n");
            }
        }

        String direction = budgetDelta < 0 ? "reduce" : "increase";
        return "Here is the current itinerary:\n" + currentItinerary
                + "\nThe traveler wants to " + direction + " the total cost by roughly ₹" + Math.abs(budgetDelta) + " and Traveller have interest in " + interest
                + ". Suggest which activities or accommodation to swap or adjust to hit this target, while keeping as much of the original plan as possible. "
                + "Respond with the SAME JSON structure as before, with your adjustments applied.";
    }

    @Transactional
    public void parseAndSaveItinerary(GeneratedItinerary itinerary, Trip trip, List<PlaceResult> places) {
        if (itinerary.travelModeSuggestion() != null) {
            trip.setTravelModeSuggestion(itinerary.travelModeSuggestion());
        }

        if (itinerary.budgetBreakdown() != null) {
            trip.setBudgetFoodPct(itinerary.budgetBreakdown().food());
            trip.setBudgetActivitiesPct(itinerary.budgetBreakdown().activities());
            trip.setBudgetStayPct(itinerary.budgetBreakdown().stay());
            trip.setBudgetTransportPct(itinerary.budgetBreakdown().transport());
            tripRepository.save(trip);
        }

        for (GeneratedAccommodation accommodation: itinerary.accommodations()) {
            Accommodation accommodation1 = new Accommodation();

            try {
                accommodation1.setCheckInDate(LocalDate.parse(accommodation.checkInDate()));
                accommodation1.setCheckOutDate(LocalDate.parse(accommodation.checkOutDate()));
            } catch (Exception e) {
                continue; // skip this malformed accommodation rather than failing the whole save
            }
            accommodation1.setHotelName(accommodation.hotelName());
            accommodation1.setPricePerNight(accommodation.pricePerNight());
            accommodation1.setNotes(accommodation.notes());
            accommodation1.setTrip(trip);
            accommodationRepository.save(accommodation1);
        }

        for (GeneratedDay generatedDay : itinerary.days()) {
            LocalDate dayDate = trip.getStartDate().plusDays(generatedDay.dayNumber() - 1L);

            Day dayEntity = new Day();
            dayEntity.setDayDate(dayDate);
            dayEntity.setDayNumber(generatedDay.dayNumber());
            dayEntity.setTrip(trip);
            Day savedDay = dayRepository.save(dayEntity);


            for (GeneratedActivity generatedActivity : generatedDay.activities()) {
                Activity activityEntity = new Activity();
                activityEntity.setName(generatedActivity.name());
                activityEntity.setStartTime(LocalTime.parse(generatedActivity.startTime()));
                activityEntity.setEndTime(LocalTime.parse(generatedActivity.endTime()));
                activityEntity.setNotes(generatedActivity.notes());
                activityEntity.setSequence(generatedActivity.sequence());
                activityEntity.setIsHiddenGem(generatedActivity.isHiddenGem());
                activityEntity.setDay(savedDay);
                activityEntity.setReviewSnippet(generatedActivity.reviewSnippet());

                PlaceResult match = findMatchingPlace(generatedActivity.name(), places);
                if (match != null) {
                    activityEntity.setLatitude(match.latitude());
                    activityEntity.setLongitude(match.longitude());
                    activityEntity.setPhotoReference(match.photoReference());
                }
                activityRepository.save(activityEntity);
            }
        }

    }

    private GeneratedItinerary parseWithRetry(String userPrompt, int maxAttempts) {
        String currentPrompt = userPrompt;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String rawJson = openAiService.generateItinerary(currentPrompt);
            try {
                GeneratedItinerary itinerary = objectMapper.readValue(rawJson, GeneratedItinerary.class);
                validateItinerary(itinerary);
                return itinerary;
            } catch (Exception e) {
                lastError = e;
                currentPrompt = userPrompt + "\n\nYour previous response was invalid: " + e.getMessage()
                        + ". Please respond again with the COMPLETE, valid JSON matching the required structure.";
            }
        }

        throw new RuntimeException("Failed to generate valid itinerary after " + maxAttempts + " attempts: "
                + (lastError != null ? lastError.getMessage() : "unknown error"));
    }

    private void validateItinerary(GeneratedItinerary itinerary) {
        if (itinerary.days() == null || itinerary.days().isEmpty()) {
            throw new RuntimeException("No days generated");
        }
        for (GeneratedAccommodation acc : itinerary.accommodations()) {
            try {
                LocalDate.parse(acc.checkInDate());
                LocalDate.parse(acc.checkOutDate());
            } catch (Exception e) {
                throw new RuntimeException("Accommodation has invalid date: " + acc.checkInDate() + "/" + acc.checkOutDate());
            }
        }
        for (GeneratedDay day : itinerary.days()) {
            if (day.activities() == null || day.activities().isEmpty()) {
                throw new RuntimeException("Day " + day.dayNumber() + " has no activities");
            }
            for (GeneratedActivity activity : day.activities()) {
                if (activity.name() == null || activity.startTime() == null || activity.endTime() == null) {
                    throw new RuntimeException("Activity missing required fields: " + activity.name());
                }
            }
        }
    }


    private PlaceResult findMatchingPlace(String activityName, List<PlaceResult> places) {
        return places.stream()
                .filter(place -> place.name().toLowerCase().contains(activityName.toLowerCase())
                        || activityName.toLowerCase().contains(place.name().toLowerCase()))
                .findFirst()
                .orElse(null);
    }

    public void generateItinerary(Trip trip, String interests) {
        List<PlaceResult> places = placesService.getPlacesForItinerary(trip.getDestination(), interests);
        String placesText = formatPlacesForPrompt(places);

        long numberOfDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;

        String userPrompt =
                "Build a "
                        + numberOfDays
                        + "-day itinerary for "
                        + trip.getDestination()
                        + ".\n"
                        + "Budget: "
                        + trip.getBudget()
                        + "\n"
                        + "Interests: "
                        + interests
                        + "\n\n"
                        + "Weather forecast for each day:\n"
                        + formatWeatherForPrompt(trip)
                        + "\n"
                        + "If a day's weather is rainy or poor, prefer indoor activities for that day.\n\n"
                        + "Include hotels or stay as well depending upon the no of days trip and budget in accommodations, Do not include hotel check-in, check-out, or accommodation details as activities inside \"days\". Put all lodging information only in \"accommodations\".\n\n"
                        + "For accommodations, suggest a category of stay (e.g. \"mid-range boutique hotel\", \"budget guesthouse\", \"luxury resort\") with a realistic price range for that category and destination — do NOT name a specific real hotel brand with an invented exact price.\n\n"
                        + "Provide a rough budget breakdown as percentages (stay, food, activities, transport) that sum to 100%, based on the trip budget and destination.\n\n"
                        + "Include 1-2 'hidden gem' suggestions among the activities — lesser-known places from the list provided, not just the most famous ones.\n\n"
                        + "If relevant, briefly mention a suggested mode of travel (train, bus, flight) for reaching "
                        + trip.getDestination()
                        + " from "
                        + trip.getOrigin()
                        + " as part of the trip notes — this is a general suggestion, not based on real-time schedules or prices.\n\n"
                        + "For \"name\", always use the exact, specific name of a real place — never a generic description like \"Lunch at a local café\" or an instructional phrase. Every activity must reference an identifiable, named place, ideally one from the provided real-places list.\n\n"
                        + "If the destination spans multiple distinct locations (e.g., different islands, cities, or areas within a region), plan realistic inter-location travel and ensure accommodations match each leg of the trip separately.\n\n"
                        + "If the trip involves visiting multiple distinct locations (e.g., different islands, cities, or regions), you MUST provide a separate accommodation entry for each distinct location, with checkInDate/checkOutDate matching exactly when the traveler is in that location. Do not provide a single accommodation covering multiple different locations if the itinerary moves between them.\n\n"
                        + "Here are real places you may use:\n"
                        + placesText;

        GeneratedItinerary rawJson = parseWithRetry(userPrompt, 3);
        parseAndSaveItinerary(rawJson, trip, places);
    }

    private String formatPlacesForPrompt(List<PlaceResult> places) {
        StringBuilder sb = new StringBuilder();
        for (PlaceResult place : places) {
            sb.append("- ").append(place.name())
                    .append(", ").append(place.address())
                    .append(" (rating: ").append(place.rating()).append(")\n");
            if (place.reviewSnippets() != null && !place.reviewSnippets().isEmpty()) {
                sb.append(" | Reviews: ").append(String.join(" / ", place.reviewSnippets()));
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

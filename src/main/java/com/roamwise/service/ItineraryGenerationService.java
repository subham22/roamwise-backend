package com.roamwise.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roamwise.dto.PlaceResult;
import com.roamwise.dto.ai_response.GeneratedActivity;
import com.roamwise.dto.ai_response.GeneratedDay;
import com.roamwise.dto.ai_response.GeneratedItinerary;
import com.roamwise.entity.Activity;
import com.roamwise.entity.Day;
import com.roamwise.entity.Trip;
import com.roamwise.repository.ActivityRepository;
import com.roamwise.repository.DayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@RequiredArgsConstructor
public class ItineraryGenerationService {

    private final ObjectMapper objectMapper;
    private final PlacesService placesService;
    private final OpenAiService openAiService;
    private final DayRepository dayRepository;
    private final ActivityRepository activityRepository;


    public void parseAndSaveItinerary(String json, Trip trip) {
        GeneratedItinerary itinerary;
        try {
            itinerary = objectMapper.readValue(json, GeneratedItinerary.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse AI response: " + e.getMessage());
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
                activityEntity.setDay(savedDay);
                activityRepository.save(activityEntity);
            }
        }
    }

    public void generateItinerary(Trip trip, String interests) {
        List<PlaceResult> places = placesService.getPlacesForItinerary(trip.getDestination(), interests);
        String placesText = formatPlacesForPrompt(places);

        long numberOfDays = ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate()) + 1;

        String userPrompt = "Build a " + numberOfDays + "-day itinerary for " + trip.getDestination() + ".\n"
                + "Budget: " + trip.getBudget() + "\n"
                + "Interests: " + interests + "\n\n"
                + "Here are real places you may use:\n" + placesText;

        String rawJson = openAiService.generateItinerary(userPrompt);
        parseAndSaveItinerary(rawJson, trip);
    }

    private String formatPlacesForPrompt(List<PlaceResult> places) {
        StringBuilder sb = new StringBuilder();
        for (PlaceResult place : places) {
            sb.append("- ").append(place.name())
                    .append(", ").append(place.address())
                    .append(" (rating: ").append(place.rating()).append(")\n");
        }
        return sb.toString();
    }
}

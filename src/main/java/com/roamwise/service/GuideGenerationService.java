package com.roamwise.service;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roamwise.dto.GeneratedGuide;
import com.roamwise.dto.PlaceResult;
import com.roamwise.dto.accomodation.GeneratedAccommodation;
import com.roamwise.dto.ai_response.GeneratedActivity;
import com.roamwise.dto.ai_response.GeneratedDay;
import com.roamwise.dto.ai_response.GeneratedItinerary;
import com.roamwise.entity.guide.Guide;
import com.roamwise.entity.guide.GuideActivity;
import com.roamwise.entity.guide.GuideDay;
import com.roamwise.entity.guide.Status;
import com.roamwise.repository.guide.GuideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GuideGenerationService {

    private final ObjectMapper objectMapper;
    private final PlacesService placesService;
    private final OpenAiService openAiService;
    private  final ItineraryGenerationService itineraryGenerationService;
    private final GuideRepository guideRepository;

    public void generateGuide(String destination, int numberOfDays) throws JsonProcessingException {
       List<PlaceResult> places =  placesService.searchPlaces("top attractions and things to do in " + destination);
        String userPrompt = "Write a %d-day travel guide for %s. For 'name', always use the exact real place name, never a generic description. Include 1-2 hidden-gem activities. Here are real places you may use: %s"
                .formatted(numberOfDays, destination, itineraryGenerationService.formatPlacesForPrompt(places));
       GeneratedGuide guide = parseWithRetry(userPrompt, 3);

       Guide newGuide = new Guide();
        String slug = destination.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "") + "-" + numberOfDays + "-days";
        newGuide.setDestination(destination);
       newGuide.setDays(convertToGeneratedDay(guide, newGuide, places));
       newGuide.setStatus(Status.DRAFT);
       newGuide.setTitle("Trip to " + destination + " for " + numberOfDays + " days");
       newGuide.setMetaDescription("Trip to " + destination + " for " + numberOfDays + " days");
        newGuide.setSlug(slug);
        newGuide.getDays().stream().flatMap(d -> d.getActivities().stream())
                        .map(GuideActivity::getPhotoReference)
                                .filter(ref -> ref != null)
                                        .findFirst()
                                                .ifPresent(newGuide::setHeroImageUrl);

       guideRepository.save(newGuide);

    }

    private List<GuideDay> convertToGeneratedDay(GeneratedGuide guide, Guide newGuide, List<PlaceResult> places) {
        List<GeneratedDay> days = guide.days();
        List<GuideDay> guideDays = new ArrayList<>();
        for(Integer i =0; i< days.size(); i++) {
            GuideDay day = new GuideDay();
            day.setDayNo(i + 1);
            day.setDayLabel("Day " + (i+1));
            day.setActivities(convertToGeneratedActivity(days.get(i), day, places));
            day.setGuide(newGuide);
            guideDays.add(day);

        }
        return guideDays;
    }

    private List<GuideActivity> convertToGeneratedActivity(GeneratedDay day, GuideDay newDay, List<PlaceResult> places) {
        List<GeneratedActivity> generatedActivities = day.activities();
        List<GuideActivity> guideActivities = new ArrayList<>();

        for (GeneratedActivity activity: generatedActivities) {
            GuideActivity a1 = new GuideActivity();
            a1.setName(activity.name());
            a1.setIsHiddenGem(activity.isHiddenGem());
            a1.setEndTime(LocalTime.parse(activity.endTime()));
            a1.setStartTime(LocalTime.parse(activity.startTime()));
            a1.setReviewSnippet(activity.reviewSnippet());
            a1.setSequence(activity.sequence());
            a1.setNotes(activity.notes());
            a1.setGuideDay(newDay);
            PlaceResult match = itineraryGenerationService.findMatchingPlace(activity.name(), places);
            if (match != null) {
                a1.setLatitude(match.latitude());
                a1.setLongitude(match.longitude());
                a1.setPhotoReference(match.photoReference());
            }
            guideActivities.add(a1);

        }

        return guideActivities;
    }

    private GeneratedGuide parseWithRetry(String userPrompt, int maxAttempts) {
        String currentPrompt = userPrompt;
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            String rawJson = openAiService.generateGuide(currentPrompt);
            try {
                GeneratedGuide guide = objectMapper.readValue(rawJson, GeneratedGuide.class);
                validateItinerary(guide);
                return guide;
            } catch (Exception e) {
                lastError = e;
                currentPrompt = userPrompt + "\n\nYour previous response was invalid: " + e.getMessage()
                        + ". Please respond again with the COMPLETE, valid JSON matching the required structure.";
            }
        }

        throw new RuntimeException("Failed to generate valid itinerary after " + maxAttempts + " attempts: "
                + (lastError != null ? lastError.getMessage() : "unknown error"));
    }

    private void validateItinerary(GeneratedGuide guide) {
        if (guide.days() == null || guide.days().isEmpty()) {
            throw new RuntimeException("No days generated");
        }

        for (GeneratedDay day : guide.days()) {
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
}

package com.roamwise.service;


import com.roamwise.dto.FeasibilityIssue;
import com.roamwise.entity.Activity;
import com.roamwise.entity.Day;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeasibilityService {

    private final DirectionsService directionsService;
    private final WeatherService weatherService;

    public List<FeasibilityIssue> checkDayFeasibility(Day day) {
        List<FeasibilityIssue> issues = new ArrayList<>();
        List<Activity> activities = day.getActivities().stream()
                .sorted(Comparator.comparing(Activity::getSequence))
                .toList();

        String weather = weatherService.getWeather(day.getDayDate(), day.getTrip().getDestination());
        boolean weatherKnown = !weather.equals("Forecast not available yet");
        boolean rainy = weatherKnown && weather.toLowerCase().contains("rain");

        for (int i = 1; i < activities.size(); i++) {
            Activity prev = activities.get(i - 1);
            Activity curr = activities.get(i);

            long gapMinutes = Duration.between(prev.getEndTime(), curr.getStartTime()).toMinutes();
            if (gapMinutes > 0 && gapMinutes <= 60) {
                String travelTime = directionsService.getTravelTime(prev.getName(), curr.getName());
                issues.add(new FeasibilityIssue(curr.getId(), "Only " + gapMinutes + " min between " + prev.getName() + " and " + curr.getName()
                        + " (travel: " + travelTime + ") — this may be too tight."));
            }

            if (rainy && (curr.getNotes() == null || !curr.getNotes().toLowerCase().contains("indoor"))) {
                issues.add(new FeasibilityIssue(curr.getId(), "Rain predicted for this day — \"" + curr.getName() + "\" may be affected if it's outdoors."));
            }
        }

        return issues;
    }
}

package com.roamwise.service;

import com.roamwise.dto.ActivityResponse;
import com.roamwise.dto.CreateActivityRequest;
import com.roamwise.entity.Activity;
import com.roamwise.entity.Day;
import com.roamwise.repository.ActivityRepository;
import com.roamwise.repository.DayRepository;
import com.roamwise.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ActivityService {

    private final UserRepository userRepository;
    private final DayRepository dayRepository;
    private final ActivityRepository activityRepository;

    public ActivityResponse createActivity(Integer dayId, CreateActivityRequest request, Integer cuurentUserId) {
        Day day = dayRepository.findById(dayId).orElseThrow(() -> new RuntimeException("Day not found, Activity can't be added"));
        if (!day.getTrip().getUser().getId().equals(cuurentUserId)) {
            throw new RuntimeException("Day for this trip doesn't belong to current user");
        }
        Activity activity = this.createActivityEntity(request);
        activity.setDay(day);
        Activity savedActivity = activityRepository.save(activity);
        return convertToActivityResponse(savedActivity);
    }

    public void reorderActivities(Integer dayId, List<Integer> orderedActivityIds, Integer currentUserId) {
        List<Activity> activities = new ArrayList<>();

        for (int i =0; i < orderedActivityIds.size(); i++) {
            Activity activity = activityRepository.findById(orderedActivityIds.get(i)).orElseThrow(() -> new RuntimeException("Activity Doesn't Exist"));
            if (!activity.getDay().getTrip().getUser().getId().equals(currentUserId)) {
                throw new RuntimeException("Activity doesn't belong to this user");
            }

            activities.add(activity);

        }

        List<LocalTime[]> timeSlots = activities.stream().map(a -> new LocalTime[]{a.getStartTime(), a.getEndTime()})
                        .sorted(Comparator.comparing(slot -> slot[0]))
                                .toList();



        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            activity.setSequence(i + 1);
            activity.setStartTime(timeSlots.get(i)[0]);
            activity.setEndTime(timeSlots.get(i)[1]);
            activityRepository.save(activity);
        }

    }

    public ActivityResponse updateActivity(Integer activityId, CreateActivityRequest request, Integer currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("No Activity Found"));

        if (!activity.getDay().getTrip().getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Activity doesn't belong to this user");
        }

        activity.setName(request.getName());
        activity.setSequence(request.getSequence());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setNotes(request.getNotes());

        Activity savedActivity = activityRepository.save(activity); // same id → UPDATE
        return convertToActivityResponse(savedActivity);
    }

    public void deleteActivity(Integer activityId, Integer currentUserId) {
        Activity activity = activityRepository.findById(activityId)
                .orElseThrow(() -> new RuntimeException("No Activity Found"));

        if (!activity.getDay().getTrip().getUser().getId().equals(currentUserId)) {
            throw new RuntimeException("Activity doesn't belong to this user");
        }

        activityRepository.delete(activity);
    }

    private Activity createActivityEntity(CreateActivityRequest request) {
        Activity activity = new Activity();
        activity.setName(request.getName());
        activity.setSequence(request.getSequence());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setNotes(request.getNotes());
        return  activity;
    }



    private ActivityResponse convertToActivityResponse(Activity activity) {
        ActivityResponse response = new ActivityResponse();
        response.setId(activity.getId());
        response.setName(activity.getName());
        response.setSequence(activity.getSequence());
        response.setStartTime(activity.getStartTime());
        response.setEndTime(activity.getEndTime());
        response.setNotes(activity.getNotes());
        return response;
    }
}

package com.roamwise.service;


import com.roamwise.dto.CreateDayRequest;
import com.roamwise.dto.DayResponse;
import com.roamwise.entity.Day;
import com.roamwise.entity.Trip;
import com.roamwise.repository.DayRepository;
import com.roamwise.repository.TripRepository;
import com.roamwise.entity.TripRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DayService {

    private final DayRepository dayRepository;
    private final TripRepository tripRepository;
    private final TripAccessService tripAccess;

  public DayResponse addDay(Integer tripId, CreateDayRequest request, Integer currentUserId) {
    Trip trip =
        tripRepository
            .findById(tripId)
            .orElseThrow(() -> new RuntimeException("Trip Doesn't Exist!!"));
        tripAccess.require(trip, currentUserId, TripRole.EDITOR);

      Day day = new Day();
      day.setDayDate(request.getDayDate());
      day.setDayNumber(request.getDayNo());
      day.setTrip(trip);
      Day savedDay = dayRepository.save(day);

      DayResponse dayResponse = new DayResponse();
      dayResponse.setId(savedDay.getId());
      dayResponse.setDayDate(savedDay.getDayDate());
      dayResponse.setDayNo(savedDay.getDayNumber());
      return dayResponse;

  }

  public DayResponse updateDay(Integer dayId, CreateDayRequest createDayRequest, Integer currentUserId) {
      Day day = dayRepository.findById(dayId).orElseThrow(() -> new RuntimeException("Day doesn't exsit"));
      tripAccess.require(day.getTrip(), currentUserId, TripRole.EDITOR);

      day.setDayNumber(createDayRequest.getDayNo());
      day.setDayDate(createDayRequest.getDayDate());
      Day savedDay = dayRepository.save(day);

      DayResponse dayResponse = new DayResponse();
      dayResponse.setId(savedDay.getId());
      dayResponse.setDayDate(savedDay.getDayDate());
      dayResponse.setDayNo(savedDay.getDayNumber());
      return dayResponse;
  }

  public void deleteDay(Integer dayId, Integer currentUserId) {
      Day day = dayRepository.findById(dayId).orElseThrow(() -> new RuntimeException("Day doesn't exsit"));
      tripAccess.require(day.getTrip(), currentUserId, TripRole.EDITOR);

      dayRepository.delete(day);
  }


}

package com.roamwise.dto;

import com.roamwise.dto.accomodation.AccommodationResponse;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class TripResponse {

    private Integer tripId;
    private String origin;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer budget;
    private List<DayResponse> days;
    private List<AccommodationResponse> accommodations;
    private String budgetStayPct;
    private String budgetFoodPct;
    private String budgetActivitiesPct;
    private String budgetTransportPct;
    private String travelModeSuggestion;
}

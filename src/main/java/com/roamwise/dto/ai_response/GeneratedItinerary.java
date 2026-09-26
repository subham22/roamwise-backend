package com.roamwise.dto.ai_response;

import com.roamwise.dto.accomodation.GeneratedAccommodation;
import com.roamwise.dto.accomodation.GeneratedBudgetBreakdown;

import java.util.List;

public record GeneratedItinerary(String travelModeSuggestion, List<GeneratedAccommodation> accommodations, List<GeneratedDay> days, GeneratedBudgetBreakdown budgetBreakdown) {}

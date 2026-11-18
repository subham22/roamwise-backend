package com.roamwise.dto.guide;

import java.util.List;

/** Practical travel info shown under a guide's itinerary; stored as JSON on Guide.travelInfo. */
public record GuideTravelInfo(String overview, String bestTimeToVisit, String howToReach, String gettingAround,
                              String budget, List<String> tips, List<Faq> faqs) {

    public record Faq(String question, String answer) {}
}

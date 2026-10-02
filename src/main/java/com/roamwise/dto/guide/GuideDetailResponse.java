package com.roamwise.dto.guide;

import java.time.LocalTime;
import java.util.List;

public record GuideDetailResponse(String slug, String title, String destination,
                                  String metaDescription, String heroImageUrl,
                                  List<GuideDayResponse> days) {

    public record GuideDayResponse(Integer dayNo, String dayLabel, List<GuideActivityResponse> activities) {}

    public record GuideActivityResponse(String name, LocalTime startTime, LocalTime endTime, String notes,
                                        Integer sequence, Boolean isHiddenGem, String reviewSnippet,
                                        Double latitude, Double longitude, String photoReference) {}
}
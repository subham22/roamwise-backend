package com.roamwise.dto.guide;

public record GuideSummaryResponse(String slug, String title, String destination,
                                   String metaDescription, String heroImageUrl) {}

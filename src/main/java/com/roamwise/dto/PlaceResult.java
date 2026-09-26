package com.roamwise.dto;

import java.util.List;

public record PlaceResult(String name, String address, Double rating, Double latitude, Double longitude, String photoReference, List<String> reviewSnippets) {}
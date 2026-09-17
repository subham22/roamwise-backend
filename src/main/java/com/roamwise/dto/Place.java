package com.roamwise.dto;

import java.util.List;

public record Place(DisplayName displayName, String formattedAddress, Double rating, List<String> types) {}

package com.roamwise.dto.weather;

import java.util.List;

public record ForecastResponse(List<ForecastEntry> list) {}

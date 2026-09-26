package com.roamwise.dto.weather;

import java.util.List;

public record ForecastEntry(String dt_txt, Main main, List<WeatherDescription> weather) {}

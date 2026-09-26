package com.roamwise.service;


import com.roamwise.dto.weather.ForecastEntry;
import com.roamwise.dto.weather.ForecastResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class WeatherService {

    @Value("${openweather.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    @Cacheable(value = "weather", key= "#city + '-' + #date")
    public String getWeather(LocalDate date, String city) {
        ForecastResponse response = restClient.get()
                .uri("https://api.openweathermap.org/data/2.5/forecast?q={city}&appid={key}", city, apiKey)
                .retrieve()
                .body(ForecastResponse.class);

        if (response == null || response.list() == null) {
            return "Forecast not available";
        }
        String dateString = date.toString();

        Optional<ForecastEntry> match = response.list().stream().filter(entry -> entry.dt_txt().startsWith(dateString)).findFirst();

        if (match.isEmpty()) return "Forecast not available yet";

        ForecastEntry entry = match.get();
        String description = entry.weather().get(0).description();
        double tempCelsius = entry.main().temp() - 273.15; // Kelvin to Celsius

        return String.format("%s, %.1f°C", description, tempCelsius);
    }
}

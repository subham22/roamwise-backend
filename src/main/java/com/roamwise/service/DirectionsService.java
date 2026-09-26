package com.roamwise.service;


import com.roamwise.dto.direction.DirectionsResponse;
import com.roamwise.dto.direction.Route;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Service
public class DirectionsService {

    @Value("${google.places.api.key}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    @Cacheable(value = "directions", key = "#origin + '-' + #destination")
    public String getTravelTime(String origin, String destination) {
        DirectionsResponse response  = restClient.get()
                .uri("https://maps.googleapis.com/maps/api/directions/json?origin={origin}&destination={destination}&key={key}", origin, destination, apiKey)
                .retrieve()
                .body(DirectionsResponse.class);
        System.out.println(response);

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return "Travel time unavailable";
        }

        Route firstRoute = response.routes().get(0);
        if (firstRoute.legs() == null || firstRoute.legs().isEmpty()) {
            return "Travel time unavailable";
        }

        return firstRoute.legs().get(0).duration().text();
    }
}

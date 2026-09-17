package com.roamwise.service;


import com.roamwise.dto.Place;
import com.roamwise.dto.PlaceResult;
import com.roamwise.dto.PlacesResponse;
import com.roamwise.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlacesService {

    @Value("${google.places.api.key}")
    private String apiKey;


    private final OpenAiService openAiService;
    private final ItineraryGenerationService itineraryGenerationService;

    private final RestClient restClient = RestClient.create();

    public List<PlaceResult> searchPlaces(String query) {
        PlacesResponse response = restClient.post()
                .uri("https://places.googleapis.com/v1/places:searchText")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.rating,places.types")
                .body(Map.of("textQuery", query))
                .retrieve()
                .body(PlacesResponse.class);

        if (response == null || response.places() == null) {
            return List.of();
        }

        return response.places().stream()
                .map(place -> new PlaceResult(
                        place.displayName().text(),
                        place.formattedAddress(),
                        place.rating()
                ))
                .toList();
    }

    public List<PlaceResult> getPlacesForItinerary(String destination, String interests) {
        String query = "top " + interests + " places to visit in " + destination;
        return searchPlaces(query);
    }


}

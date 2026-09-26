package com.roamwise.service;


import com.roamwise.dto.PhotoMediaResponse;
import com.roamwise.dto.Place;
import com.roamwise.dto.PlaceResult;
import com.roamwise.dto.PlacesResponse;
import com.roamwise.dto.autocomplete.AutocompleteResponse;
import com.roamwise.entity.Trip;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlacesService {

    @Value("${google.places.api.key}")
    private String apiKey;


    private final RestClient restClient = RestClient.create();


    public List<String> autocomplete(String input) {
        Map<String, Object> body = Map.of("input", input);

        AutocompleteResponse response = restClient.post()
                .uri("https://places.googleapis.com/v1/places:autocomplete")
                .header("X-Goog-Api-Key", apiKey)
                .body(body)
                .retrieve()
                .body(AutocompleteResponse.class);
        if (response == null || response.suggestions()  == null ) {
            return List.of();
        }

        return response.suggestions().stream().map(s -> s.placePrediction().text().text()).toList();
    }

    @Cacheable(value="places", key = "#query")
    public List<PlaceResult> searchPlaces(String query) {
        PlacesResponse response = restClient.post()
                .uri("https://places.googleapis.com/v1/places:searchText")
                .header("X-Goog-Api-Key", apiKey)
                .header("X-Goog-FieldMask", "places.displayName,places.formattedAddress,places.rating,places.types,places.location,places.photos,places.reviews")
                .body(Map.of("textQuery", query))
                .retrieve()
                .body(PlacesResponse.class);

        if (response == null || response.places() == null) {
            return List.of();
        }
        System.out.println(response.places().stream().map(l -> l.location()).toList());
        return response.places().stream()
                .map(place -> new PlaceResult(
                        place.displayName().text(),
                        place.formattedAddress(),
                        place.rating(),
                        place.location()  != null ? place.location().latitude() : null,
                        place.location()  != null ? place.location().longitude() : null,
                        (place.photos() != null && !place.photos().isEmpty()) ? place.photos().get(0).name() : null,
                        place.reviews() != null
                                ? place.reviews().stream()
                                .filter(r -> r.text() != null && r.text().text() != null)
                                .limit(2)
                                .map(r -> r.text().text())
                                .toList()
                                : List.of()
                ))
                .toList();
    }

    public List<PlaceResult> getPlacesForItinerary(String destination, String interests) {
        String query = "top " + interests + " places to visit in " + destination;
        return searchPlaces(query);
    }

    @Cacheable(value = "photos", key = "#photoReference")
    public byte[] getPhoto(String photoReference) {
        String url = "https://places.googleapis.com/v1/" + photoReference + "/media?maxWidthPx=400&key=" + apiKey;

        PhotoMediaResponse mediaResponse = restClient.get()
                .uri(url)
                .retrieve()
                .body(PhotoMediaResponse.class);

        return restClient.get()
                .uri(mediaResponse.photoUri())
                .retrieve()
                .body(byte[].class);
    }

}

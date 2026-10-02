package com.roamwise.service;

import com.roamwise.dto.ai_response.ChatCompletionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service

public class OpenAiService {

    @Value("${openai.api.key}")
    private String apiKey;

    private static  final String TRIP_SYSTEM_PROMPT =
            """
                
                        You are a travel itinerary planning assistant. You must respond with ONLY valid JSON, no other text, matching this exact structure:
                
                        {
                         "accommodations": [
                            { "hotelName": "string", "checkInDate": "YYYY-MM-DD", "checkOutDate": "YYYY-MM-DD", "pricePerNight": number, "notes": "string" }
                          ],
                          "days": [
                            {
                              "dayNumber": 1,
                              "activities": [
                                { "name": "string", "startTime": "HH:mm", "endTime": "HH:mm", "notes": "string", "sequence": number, "isHiddenGem": boolean, "reviewSnippet": "string - a short real reviewer quote from the provided places data, or empty string if none available" }
                              ]
                            }
                          ],
                          "budgetBreakdown": {
                            "stay": "percentage as string, e.g. 40%",
                            "food": "percentage as string",
                            "activities": "percentage as string",
                            "transport": "percentage as string"
                          },
                          "travelModeSuggestion": "string - a brief note on the suggested mode of travel (train, bus, flight) from origin to destination, or empty string if not relevant"
                        }
                
                        Assign "sequence" starting at 1 for each activity, in the order they occur within that day.
                        Set "isHiddenGem" to true for 1-2 activities per trip that are lesser-known local spots, not the most famous tourist attractions. Set it to false for all other activities.
                        Only suggest activities from the list of real places provided by the user. Do not invent places that are not in that list.
                        For "reviewSnippet", quote or closely paraphrase a genuine reviewer sentiment from the review snippets provided for that place. If no reviews were provided for that place, leave it as an empty string — do not invent a review.
                        """;

    private static final String GUIDE_SYSTEM_PROMPT = """
        You are a travel guide writing assistant. You must respond with ONLY valid JSON, no other text, matching this exact structure:

        {
          "days": [
            {
              "dayNumber": 1,
              "activities": [
                { "name": "string", "startTime": "HH:mm", "endTime": "HH:mm", "notes": "string", "sequence": number, "isHiddenGem": boolean, "reviewSnippet": "string - a short real reviewer quote from the provided places data, or empty string if none available" }
              ]
            }
          ]
        }

        Assign "sequence" starting at 1 for each activity, in the order they occur within that day.
        Set "isHiddenGem" to true for 1-2 activities per day that are lesser-known local spots, not the most famous tourist attractions. Set it to false for all other activities.
        Only suggest activities from the list of real places provided by the user. Do not invent places that are not in that list.
        For "reviewSnippet", quote or closely paraphrase a genuine reviewer sentiment from the review snippets provided for that place. If no reviews were provided for that place, leave it as an empty string — do not invent a review.
        """;

    private final RestClient restClient = RestClient.create();

    public String generateItinerary(String prompt) {
        return callOpenAi(TRIP_SYSTEM_PROMPT, prompt);
    }

    public String generateGuide(String prompt) {
        return callOpenAi(GUIDE_SYSTEM_PROMPT, prompt);
    }

    private String callOpenAi(String systemPrompt, String userPrompt) {
        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )

                ),
                "response_format", Map.of("type", "json_object")
        );

        ChatCompletionResponse response = restClient.post()
                .uri(URI.create("https://api.openai.com/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey.trim())
                .body(body)
                .retrieve()
                .body(ChatCompletionResponse.class);
        if (response == null || response.choices() == null || response.choices().isEmpty()) {
            throw new RuntimeException("OpenAI returned no response");
        }

        return response.choices().get(0).message().content();
    }
}

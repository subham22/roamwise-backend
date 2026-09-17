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

    private final RestClient restClient = RestClient.create();

    public String generateItinerary(String prompt) {

        String systemPrompt = """

            You are a travel itinerary planning assistant. You must respond with ONLY valid JSON, no other text, matching this exact structure:
            
            {
              "days": [
                {
                  "dayNumber": 1,
                  "activities": [
                    { "name": "string", "startTime": "HH:mm", "endTime": "HH:mm", "notes": "string" }
                  ]
                }
              ]
            }
            
            Only suggest activities from the list of real places provided by the user. Do not invent places that are not in that list.
""";

        Map<String, Object> body = Map.of(
                "model", "gpt-4o-mini",
                "messages", List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                            "role", "user",
                            "content", prompt
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

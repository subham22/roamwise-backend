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

  private static final String GUIDE_SYSTEM_PROMPT =
      """
        You are a travel guide writing assistant. You must respond with ONLY valid JSON, no other text, matching this exact structure:

        {
        "metaDescription": "string",
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
        "metaDescription": 140 to 160 characters, naming 2 or 3 specific places from the itinerary you wrote, and mentioning timings and a map. Make no claims that aren’t in the itinerary.
        Mention the destination and number of days naturally, for example ‘this 3-day Goa itinerary’.” That way the description repeats the keyword without stuffing it.
        """;

    private static final String GUIDE_INFO_SYSTEM_PROMPT =
      """
        You write the practical-information section of an Indian travel guide. Respond with ONLY valid JSON matching:

        {
          "overview": "string, 90-130 words: what the place is like and who this itinerary suits, naming places from the itinerary",
          "bestTimeToVisit": "string, 50-80 words: best months, weather by season, festivals worth timing a trip around",
          "howToReach": "string, 60-90 words: nearest airport and railway station by name, road access from nearby big cities",
          "gettingAround": "string, 40-70 words: local transport options and rough local fares",
          "budget": "string, 50-80 words: approximate per-person daily cost in INR (₹) for budget, mid-range and comfortable travellers",
          "tips": ["4 to 6 specific, practical tips, one sentence each"],
          "faqs": [ { "question": "string", "answer": "string, 30-60 words" } ]
        }

        Write 5 faqs that real travellers search for, e.g. "Is N days enough for X?", "What is the best time to visit X?", "How much does a trip to X cost?", "How do I reach X?", plus one specific to the destination.
        Write in plain, friendly English for Indian travellers. Use only facts you are confident are accurate; describe prices as approximate. Do not invent hotel or restaurant names.
        """;

    private final RestClient restClient = RestClient.create();

    public String generateGuideInfo(String prompt) {
        return callOpenAi(GUIDE_INFO_SYSTEM_PROMPT, prompt);
    }

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

package com.roamwise.dto.ai_response;

import java.util.List;

public record ChatCompletionResponse(List<Choice> choices) {}

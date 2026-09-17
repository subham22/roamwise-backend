package com.roamwise.dto.ai_response;

import java.util.List;

public record GeneratedDay(int dayNumber, List<GeneratedActivity> activities) {}

package com.roamwise.dto;

import com.roamwise.dto.ai_response.GeneratedDay;

import java.util.List;

public record GeneratedGuide(List<GeneratedDay> days) {}

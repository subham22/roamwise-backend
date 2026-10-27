package com.roamwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatEditRequest(
        @NotBlank(message = "Tell us what to change")
        @Size(max = 500, message = "Keep the request under 500 characters")
        String message) {}

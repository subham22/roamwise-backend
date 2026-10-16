package com.roamwise.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TurnstileResponse(boolean success, String hostname,
                         @JsonProperty("error-codes") List<String> errorCodes) {}

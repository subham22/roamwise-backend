package com.roamwise.dto;

/** id is null for the owner row. */
public record TripMemberResponse(Integer id, String email, String role) {}

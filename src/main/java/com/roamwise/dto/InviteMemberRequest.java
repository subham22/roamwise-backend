package com.roamwise.dto;

import com.roamwise.entity.TripRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InviteMemberRequest(
        @NotBlank @Email(message = "Enter a valid email") String email,
        @NotNull TripRole role) {}

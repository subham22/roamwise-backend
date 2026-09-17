package com.roamwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateTripRequest {

    @NotBlank(message = "Origin can't be blank")
    private String origin;

    @NotBlank(message = "Destination can't be blank")
    private String destination;

    @NotNull(message = "Start Date can't be null")
    private LocalDate startDate;

    @NotNull(message = "End Date can't be null")
    private LocalDate endDate;

    @NotNull @Positive(message = "Budget should be more then 0")
    private Integer budget;
}

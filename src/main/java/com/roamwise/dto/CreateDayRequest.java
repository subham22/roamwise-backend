package com.roamwise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateDayRequest {

    @NotNull(message = "Day Date can't be null")
    private LocalDate dayDate;

    @NotNull(message = "Day no can't be null")
    @Positive
    private Integer dayNo;
}

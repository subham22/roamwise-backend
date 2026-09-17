package com.roamwise.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DayResponse {

    private Integer id;
    private LocalDate dayDate;
    private Integer dayNo;
    private List<ActivityResponse> activities;
}

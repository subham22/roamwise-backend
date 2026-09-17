package com.roamwise.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CreateActivityRequest {

    @NotBlank(message = "Name can't be blank")
    private String name;

    @NotNull(message = "Start Time can't be null")
    private LocalTime startTime;

    @NotNull(message = "End Time can't be null")
    private LocalTime endTime;

    @NotNull(message = "Sequence can't be null")
    private Integer sequence;

    private String notes;
}

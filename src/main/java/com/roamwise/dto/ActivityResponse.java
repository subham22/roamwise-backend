package com.roamwise.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Setter
@Getter
public class ActivityResponse {

    private Integer id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer sequence;
    private String notes;
}

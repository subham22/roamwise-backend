package com.roamwise.dto;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JobStatusResponse {
    private Integer jobId;
    private String status;
    private Integer resultTripId;
    private String errorMessage;
}
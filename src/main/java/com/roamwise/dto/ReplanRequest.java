package com.roamwise.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReplanRequest {

    @NotNull(message = "Budget can't be blank")
    private Integer budgetDelta;

    private String interest;
}

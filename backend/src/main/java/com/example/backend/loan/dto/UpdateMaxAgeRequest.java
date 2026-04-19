package com.example.backend.loan.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMaxAgeRequest {
    @NotNull
    @Min(18)
    @Max(120)
    private Integer value;
}
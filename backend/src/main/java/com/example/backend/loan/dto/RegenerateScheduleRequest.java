package com.example.backend.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegenerateScheduleRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal interestMargin;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal baseInterestRate;

    @NotNull
    @Min(1)
    private Integer loanPeriodMonths;
}
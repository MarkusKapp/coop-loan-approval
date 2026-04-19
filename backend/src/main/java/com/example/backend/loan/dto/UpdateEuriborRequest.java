package com.example.backend.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateEuriborRequest {

    @NotNull
    @DecimalMin("0.000")
    private BigDecimal value;
}


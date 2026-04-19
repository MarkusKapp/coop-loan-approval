package com.example.backend.loan.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLoanApplicationRequest {

    @NotBlank
    @Size(max = 32)
    private String firstName;

    @NotBlank
    @Size(max = 32)
    private String lastName;

    @NotBlank
    @Pattern(regexp = "\\d{11}")
    private String personalCode;

    @NotNull
    @Min(6)
    @Max(360)
    private Integer loanPeriodMonths;

    @NotNull
    @DecimalMin(value = "0.000")
    private BigDecimal interestMargin;

    @NotNull
    @DecimalMin(value = "0.000")
    private BigDecimal baseInterestRate;

    @NotNull
    @DecimalMin(value = "5000.00")
    private BigDecimal loanAmount;
}


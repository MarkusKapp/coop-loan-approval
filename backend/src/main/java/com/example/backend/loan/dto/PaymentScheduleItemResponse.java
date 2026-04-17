package com.example.backend.loan.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentScheduleItemResponse {

    private Integer paymentNumber;
    private LocalDate paymentDate;
    private BigDecimal principal;
    private BigDecimal interest;
    private BigDecimal totalPayment;
}


package com.example.backend.loan.dto;

import com.example.backend.loan.LoanStatus;
import com.example.backend.loan.RejectionReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanApplicationResponse {

    private UUID id;
    private LoanStatus status;
    private RejectionReason rejectionReason;
    private LocalDate birthDate;
    private List<PaymentScheduleItemResponse> paymentSchedule;
}





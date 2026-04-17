package com.example.backend.loan.service;

import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanPaymentScheduleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentScheduleService {

    private static final int MONEY_SCALE = 2;
    private final LoanPaymentScheduleMapper loanPaymentScheduleMapper;

    public List<LoanPaymentSchedule> buildAnnuitySchedule(
            UUID loanApplicationId,
            BigDecimal loanAmount,
            BigDecimal interestMargin,
            BigDecimal baseInterestRate,
            int periodMonths,
            LocalDate firstPaymentDate
    ) {
        BigDecimal monthlyRate = interestMargin
                .add(baseInterestRate)
                .divide(BigDecimal.valueOf(1200), 16, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calculateMonthlyPayment(loanAmount, monthlyRate, periodMonths);
        BigDecimal remaining = loanAmount;

        List<LoanPaymentSchedule> result = new ArrayList<>(periodMonths);

        for (int i = 1; i <= periodMonths; i++) {
            BigDecimal interest;
            BigDecimal principal;

            if (i == periodMonths) {
                // Final payment is adjusted to clear the remaining balance exactly.
                interest = remaining.multiply(monthlyRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                principal = remaining.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
                interest = remaining.multiply(monthlyRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                principal = monthlyPayment.subtract(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                if (principal.compareTo(remaining) > 0) {
                    principal = remaining.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
                }
            }

            BigDecimal totalPayment = principal.add(interest).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            remaining = remaining.subtract(principal).max(BigDecimal.ZERO);

            LoanPaymentSchedule item = loanPaymentScheduleMapper.toEntity(
                    loanApplicationId,
                    i,
                    firstPaymentDate.plusMonths(i - 1L),
                    principal,
                    interest,
                    totalPayment
            );
            result.add(item);
        }

        return result;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.HALF_UP);
        }

        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();

        double payment = p * r / (1 - Math.pow(1 + r, -months));
        return BigDecimal.valueOf(payment).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}



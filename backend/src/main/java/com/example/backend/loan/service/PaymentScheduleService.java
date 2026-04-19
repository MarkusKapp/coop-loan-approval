package com.example.backend.loan.service;

import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanPaymentScheduleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentScheduleService {

    private static final int MONEY_SCALE = 2;
    private final LoanPaymentScheduleMapper loanPaymentScheduleMapper;

    public List<LoanPaymentSchedule> buildAnnuitySchedule(LoanApplication application, LocalDate currentDate) {
        return buildAnnuitySchedule(
                application.getId(),
                application.getLoanAmount(),
                application.getInterestMargin(),
                application.getBaseInterestRate(),
                application.getLoanPeriodMonths(),
                currentDate
        );
    }

    public List<LoanPaymentSchedule> buildAnnuitySchedule(
            UUID loanApplicationId,
            BigDecimal loanAmount,
            BigDecimal interestMargin,
            BigDecimal baseInterestRate,
            int periodMonths,
            LocalDate firstPaymentDate
    ) {
        log.debug(
                "Building annuity schedule for application {} (amount={}, margin={}, baseRate={}, months={}, firstPaymentDate={})",
                loanApplicationId,
                loanAmount,
                interestMargin,
                baseInterestRate,
                periodMonths,
                firstPaymentDate
        );
        BigDecimal monthlyRate = interestMargin
                .add(baseInterestRate)
                .divide(BigDecimal.valueOf(1200), 16, RoundingMode.HALF_UP);

        BigDecimal monthlyPayment = calculateMonthlyPayment(loanAmount, monthlyRate, periodMonths);
        BigDecimal remaining = loanAmount;

        List<LoanPaymentSchedule> result = new ArrayList<>(periodMonths);

        for (int i = 1; i <= periodMonths; i++) {
            BigDecimal interest = remaining.multiply(monthlyRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            BigDecimal principal;

            if (i == periodMonths) {
                principal = remaining.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
            } else {
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

        log.info("Generated {} payment schedule entries for application {}", result.size(), loanApplicationId);
        log.debug("Schedule generation finished with remaining balance {}", remaining);

        return result;
    }

    private BigDecimal calculateMonthlyPayment(BigDecimal principal, BigDecimal monthlyRate, int months) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Monthly rate is zero, using simple division for payment calculation");
            return principal.divide(BigDecimal.valueOf(months), MONEY_SCALE, RoundingMode.HALF_UP);
        }

        double p = principal.doubleValue();
        double r = monthlyRate.doubleValue();

        double payment = p * r / (1 - Math.pow(1 + r, -months));
        log.debug("Calculated monthly payment for principal {} over {} months", principal, months);
        return BigDecimal.valueOf(payment).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }
}

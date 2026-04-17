package com.example.backend;

import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanPaymentScheduleMapper;
import com.example.backend.loan.service.PaymentScheduleService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentScheduleServiceTest {

    private final LoanPaymentScheduleMapper loanPaymentScheduleMapper =
            (loanApplicationId, paymentNumber, paymentDate, principal, interest, totalPayment) ->
                    LoanPaymentSchedule.builder()
                            .id(UUID.randomUUID())
                            .loanApplicationId(loanApplicationId)
                            .paymentNumber(paymentNumber)
                            .paymentDate(paymentDate)
                            .principal(principal)
                            .interest(interest)
                            .totalPayment(totalPayment)
                            .build();

    private final PaymentScheduleService service = new PaymentScheduleService(loanPaymentScheduleMapper);

    @Test
    void buildAnnuityScheduleGeneratesExpectedCountAndDates() {
        UUID id = UUID.randomUUID();
        LocalDate firstPaymentDate = LocalDate.of(2026, 5, 17);

        List<LoanPaymentSchedule> schedule = service.buildAnnuitySchedule(
                id,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(1.001),
                BigDecimal.valueOf(1.234),
                12,
                firstPaymentDate
        );

        assertEquals(12, schedule.size());
        assertEquals(firstPaymentDate, schedule.getFirst().getPaymentDate());
        assertEquals(firstPaymentDate.plusMonths(11), schedule.getLast().getPaymentDate());
        assertTrue(schedule.getLast().getPrincipal().compareTo(BigDecimal.ZERO) > 0);
    }
}



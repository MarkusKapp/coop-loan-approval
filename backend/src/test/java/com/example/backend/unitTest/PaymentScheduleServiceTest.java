package com.example.backend.unitTest;

import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanPaymentScheduleMapper;
import com.example.backend.loan.service.PaymentScheduleService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentScheduleServiceTest {

    @Mock
    private LoanPaymentScheduleMapper loanPaymentScheduleMapper;

    @InjectMocks
    private PaymentScheduleService service;

    @Test
    void buildAnnuityScheduleWithZeroInterestSplitsPrincipalEvenly() {
        stubMapperToEntity();
        UUID id = UUID.randomUUID();
        LocalDate firstPaymentDate = LocalDate.of(2026, 5, 17);

        List<LoanPaymentSchedule> schedule = service.buildAnnuitySchedule(
                id,
                BigDecimal.valueOf(1200),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                12,
                firstPaymentDate
        );

        assertEquals(12, schedule.size());
        assertEquals(firstPaymentDate, schedule.getFirst().getPaymentDate());
        assertEquals(firstPaymentDate.plusMonths(11), schedule.getLast().getPaymentDate());

        for (int i = 0; i < schedule.size(); i++) {
            LoanPaymentSchedule row = schedule.get(i);
            assertEquals(i + 1, row.getPaymentNumber());
            assertEquals(firstPaymentDate.plusMonths(i), row.getPaymentDate());
            assertMoneyEquals(BigDecimal.valueOf(100), row.getPrincipal());
            assertMoneyEquals(BigDecimal.ZERO, row.getInterest());
            assertMoneyEquals(BigDecimal.valueOf(100), row.getTotalPayment());
        }

        assertMoneyEquals(BigDecimal.valueOf(1200), total(schedule, LoanPaymentSchedule::getPrincipal));
        assertMoneyEquals(BigDecimal.ZERO, total(schedule, LoanPaymentSchedule::getInterest));
        verify(loanPaymentScheduleMapper, times(12)).toEntity(any(), any(), any(), any(), any(), any());
    }

    @Test
    void buildAnnuityScheduleRoundsPaymentsAndClearsFinalBalance() {
        stubMapperToEntity();
        UUID id = UUID.randomUUID();
        LocalDate firstPaymentDate = LocalDate.of(2026, 5, 17);

        List<LoanPaymentSchedule> schedule = service.buildAnnuitySchedule(
                id,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(2.10),
                BigDecimal.valueOf(4.00),
                36,
                firstPaymentDate
        );

        assertEquals(36, schedule.size());
        assertEquals(firstPaymentDate, schedule.getFirst().getPaymentDate());
        assertEquals(firstPaymentDate.plusMonths(35), schedule.getLast().getPaymentDate());

        assertTrue(schedule.getFirst().getInterest().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(schedule.getLast().getPrincipal().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(schedule.getLast().getTotalPayment().compareTo(schedule.getFirst().getTotalPayment()) >= 0);

        BigDecimal totalPrincipal = total(schedule, LoanPaymentSchedule::getPrincipal);
        BigDecimal totalInterest = total(schedule, LoanPaymentSchedule::getInterest);

        assertMoneyEquals(BigDecimal.valueOf(10000), totalPrincipal);
        assertTrue(totalInterest.compareTo(BigDecimal.ZERO) > 0);

        schedule.forEach(row -> assertMoneyEquals(row.getPrincipal().add(row.getInterest()), row.getTotalPayment()));
        verify(loanPaymentScheduleMapper, times(36)).toEntity(any(), any(), any(), any(), any(), any());
    }

    @Test
    void buildAnnuityScheduleWithZeroMonthsThrowsArithmeticException() {
        UUID id = UUID.randomUUID();
        LocalDate firstPaymentDate = LocalDate.of(2026, 5, 17);

        assertThrows(ArithmeticException.class, () -> service.buildAnnuitySchedule(
                id,
                BigDecimal.valueOf(1200),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                firstPaymentDate
        ));
    }

    private void stubMapperToEntity() {
        when(loanPaymentScheduleMapper.toEntity(any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> LoanPaymentSchedule.builder()
                        .id(UUID.randomUUID())
                        .loanApplicationId(invocation.getArgument(0))
                        .paymentNumber(invocation.getArgument(1))
                        .paymentDate(invocation.getArgument(2))
                        .principal(invocation.getArgument(3))
                        .interest(invocation.getArgument(4))
                        .totalPayment(invocation.getArgument(5))
                        .build());
    }

    private static BigDecimal total(List<LoanPaymentSchedule> schedule, java.util.function.Function<LoanPaymentSchedule, BigDecimal> extractor) {
        return schedule.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private static void assertMoneyEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(actual.setScale(2, java.math.RoundingMode.HALF_UP)));
    }
}



package com.example.backend.loan.service;

import com.example.backend.EstonianPersonalCodeValidator;
import com.example.backend.loan.LoanStatus;
import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.RegenerateScheduleRequest;
import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanApplicationMapper;
import com.example.backend.loan.mapper.LoanApplicationResponseMapper;
import com.example.backend.loan.repository.LoanApplicationRepository;
import com.example.backend.loan.repository.LoanPaymentScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceRegenerateTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;
    @Mock
    private LoanPaymentScheduleRepository loanPaymentScheduleRepository;
    @Mock
    private PaymentScheduleService paymentScheduleService;
    @Mock
    private LoanApplicationMapper loanApplicationMapper;
    @Mock
    private LoanApplicationResponseMapper loanApplicationResponseMapper;
    @Mock
    private EstonianPersonalCodeValidator personalCodeValidator;

    @InjectMocks
    private LoanApplicationService loanApplicationService;

    private UUID applicationId;
    private LoanApplication application;
    private RegenerateScheduleRequest request;

    @BeforeEach
    void setUp() {
        applicationId = UUID.randomUUID();

        application = new LoanApplication();
        application.setId(applicationId);
        application.setStatus(LoanStatus.IN_REVIEW);
        application.setLoanAmount(new BigDecimal("10000.00"));
        application.setInterestMargin(new BigDecimal("2.00"));
        application.setBaseInterestRate(new BigDecimal("3.00"));
        application.setLoanPeriodMonths(12);

        request = new RegenerateScheduleRequest();
        request.setInterestMargin(new BigDecimal("1.50"));
        request.setBaseInterestRate(new BigDecimal("3.50"));
        request.setLoanPeriodMonths(24);
    }

    @Test
    void regenerateSchedule_success_returnsResponseWithSchedule() {
        List<LoanPaymentSchedule> newSchedule = List.of(new LoanPaymentSchedule(), new LoanPaymentSchedule());
        LoanApplicationResponse expectedResponse = LoanApplicationResponse.builder()
                .id(applicationId)
                .status(LoanStatus.IN_REVIEW)
                .build();

        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(application)).thenReturn(application);
        when(paymentScheduleService.buildAnnuitySchedule(any(LoanApplication.class), any(LocalDate.class)))
                .thenReturn(newSchedule);
        when(loanApplicationResponseMapper.toResponse(application, newSchedule)).thenReturn(expectedResponse);

        LoanApplicationResponse result = loanApplicationService.regenerateSchedule(applicationId, request);

        assertThat(result).isEqualTo(expectedResponse);
    }

    @Test
    void regenerateSchedule_success_deletesOldScheduleBeforeSavingNew() {
        List<LoanPaymentSchedule> newSchedule = List.of(new LoanPaymentSchedule());

        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(application)).thenReturn(application);
        when(paymentScheduleService.buildAnnuitySchedule(any(LoanApplication.class), any(LocalDate.class)))
                .thenReturn(newSchedule);
        when(loanApplicationResponseMapper.toResponse(any(), any())).thenReturn(new LoanApplicationResponse());

        loanApplicationService.regenerateSchedule(applicationId, request);

        InOrder inOrder = inOrder(loanPaymentScheduleRepository);
        inOrder.verify(loanPaymentScheduleRepository).deleteByLoanApplicationId(applicationId);
        inOrder.verify(loanPaymentScheduleRepository).saveAll(newSchedule);
    }

    @Test
    void regenerateSchedule_success_updatesApplicationParameters() {
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(application)).thenReturn(application);
        when(paymentScheduleService.buildAnnuitySchedule(any(LoanApplication.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(loanApplicationResponseMapper.toResponse(any(), any())).thenReturn(new LoanApplicationResponse());

        loanApplicationService.regenerateSchedule(applicationId, request);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationRepository).save(captor.capture());

        LoanApplication saved = captor.getValue();
        assertThat(saved.getInterestMargin()).isEqualByComparingTo(new BigDecimal("1.50"));
        assertThat(saved.getBaseInterestRate()).isEqualByComparingTo(new BigDecimal("3.50"));
        assertThat(saved.getLoanPeriodMonths()).isEqualTo(24);
    }

    @Test
    void regenerateSchedule_success_buildsScheduleWithUpdatedParams() {
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));
        when(loanApplicationRepository.save(application)).thenReturn(application);
        when(paymentScheduleService.buildAnnuitySchedule(any(LoanApplication.class), any(LocalDate.class)))
                .thenReturn(List.of());
        when(loanApplicationResponseMapper.toResponse(any(), any())).thenReturn(new LoanApplicationResponse());

        loanApplicationService.regenerateSchedule(applicationId, request);

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(paymentScheduleService).buildAnnuitySchedule(captor.capture(), any(LocalDate.class));

        LoanApplication passedToSchedule = captor.getValue();
        assertThat(passedToSchedule.getInterestMargin()).isEqualByComparingTo(request.getInterestMargin());
        assertThat(passedToSchedule.getBaseInterestRate()).isEqualByComparingTo(request.getBaseInterestRate());
        assertThat(passedToSchedule.getLoanPeriodMonths()).isEqualTo(request.getLoanPeriodMonths());
    }

    @Test
    void regenerateSchedule_applicationNotFound_throwsEntityNotFoundException() {
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanApplicationService.regenerateSchedule(applicationId, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining(applicationId.toString());

        verifyNoInteractions(loanPaymentScheduleRepository, paymentScheduleService, loanApplicationResponseMapper);
    }

    @ParameterizedTest
    @EnumSource(value = LoanStatus.class, names = {"IN_REVIEW"}, mode = EnumSource.Mode.EXCLUDE)
    void regenerateSchedule_wrongStatus_throwsIllegalStateException(LoanStatus status) {
        application.setStatus(status);
        when(loanApplicationRepository.findById(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> loanApplicationService.regenerateSchedule(applicationId, request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(status.name());

        verifyNoInteractions(loanPaymentScheduleRepository, paymentScheduleService, loanApplicationResponseMapper);
    }
}
package com.example.backend.loan.service;

import com.example.backend.EstonianPersonalCodeValidator;
import com.example.backend.loan.LoanStatus;
import com.example.backend.loan.LoanStatusConstants;
import com.example.backend.loan.RejectionReason;
import com.example.backend.loan.dto.CreateLoanApplicationRequest;
import com.example.backend.loan.dto.DecisionResponse;
import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.RejectLoanApplicationRequest;
import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanApplicationMapper;
import com.example.backend.loan.mapper.LoanApplicationResponseMapper;
import com.example.backend.loan.repository.LoanApplicationRepository;
import com.example.backend.loan.repository.LoanPaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanPaymentScheduleRepository loanPaymentScheduleRepository;
    private final PaymentScheduleService paymentScheduleService;
    private final EstonianPersonalCodeValidator personalCodeValidator;
    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanApplicationResponseMapper loanApplicationResponseMapper;
    @Value("${loan.customer.max-age:70}")
    private int maxCustomerAge;

    @Transactional
    public LoanApplicationResponse createApplication(CreateLoanApplicationRequest request) {
        if (!personalCodeValidator.isValid(request.getPersonalCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Estonian personal code");
        }

        if (loanApplicationRepository.existsByPersonalCodeAndStatusIn(
                request.getPersonalCode(),
                LoanStatusConstants.ACTIVE_STATUSES
        )) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already has an active application");
        }

        LocalDate birthDate = personalCodeValidator.extractBirthDate(request.getPersonalCode());
        LoanApplication application = loanApplicationMapper.toNewEntity(request, birthDate);

        try {
            application = loanApplicationRepository.save(application);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already has an active application", ex);
        }

        int age = Period.between(birthDate, LocalDate.now()).getYears();
        if (age > maxCustomerAge) {
            application.setStatus(LoanStatus.REJECTED);
            application.setRejectionReason(RejectionReason.CUSTOMER_TOO_OLD);
            LoanApplication rejected = loanApplicationRepository.save(application);
            return loanApplicationResponseMapper.toResponse(rejected, Collections.emptyList());
        }

        List<LoanPaymentSchedule> schedule = paymentScheduleService.buildAnnuitySchedule(
                application.getId(),
                application.getLoanAmount(),
                application.getInterestMargin(),
                application.getBaseInterestRate(),
                application.getLoanPeriodMonths(),
                LocalDate.now().plusMonths(1)
        );
        loanPaymentScheduleRepository.saveAll(schedule);

        application.setStatus(LoanStatus.IN_REVIEW);
        LoanApplication inReview = loanApplicationRepository.save(application);
        return loanApplicationResponseMapper.toResponse(inReview, schedule);
    }

    @Transactional
    public DecisionResponse approve(UUID id) {
        LoanApplication application = findById(id);
        if (application.getStatus() != LoanStatus.IN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only IN_REVIEW applications can be approved");
        }

        application.setStatus(LoanStatus.APPROVED);
        application.setRejectionReason(null);
        LoanApplication saved = loanApplicationRepository.save(application);
        return new DecisionResponse(saved.getId(), saved.getStatus(), saved.getRejectionReason());
    }

    @Transactional
    public DecisionResponse reject(UUID id, RejectLoanApplicationRequest request) {
        LoanApplication application = findById(id);
        if (application.getStatus() != LoanStatus.IN_REVIEW) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only IN_REVIEW applications can be rejected");
        }

        application.setStatus(LoanStatus.REJECTED);
        application.setRejectionReason(request.getReason());
        LoanApplication saved = loanApplicationRepository.save(application);
        return new DecisionResponse(saved.getId(), saved.getStatus(), saved.getRejectionReason());
    }

    private LoanApplication findById(UUID id) {
        return loanApplicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found"));
    }

}






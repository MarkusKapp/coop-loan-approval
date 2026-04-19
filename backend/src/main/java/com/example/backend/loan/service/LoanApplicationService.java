package com.example.backend.loan.service;

import com.example.backend.EstonianPersonalCodeValidator;
import com.example.backend.loan.LoanStatus;
import com.example.backend.loan.LoanStatusConstants;
import com.example.backend.loan.dto.CreateLoanApplicationRequest;
import com.example.backend.loan.dto.DecisionResponse;
import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.RegenerateScheduleRequest;
import com.example.backend.loan.dto.RejectLoanApplicationRequest;
import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanApplicationMapper;
import com.example.backend.loan.mapper.LoanApplicationResponseMapper;
import com.example.backend.loan.repository.LoanApplicationRepository;
import com.example.backend.loan.repository.LoanPaymentScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationService {

    private static final String CUSTOMER_TOO_OLD = "CUSTOMER_TOO_OLD";

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanPaymentScheduleRepository loanPaymentScheduleRepository;
    private final PaymentScheduleService paymentScheduleService;
    private final EstonianPersonalCodeValidator personalCodeValidator;
    private final LoanApplicationMapper loanApplicationMapper;
    private final LoanApplicationResponseMapper loanApplicationResponseMapper;
    @Value("${loan.customer.max-age:70}")
    private int maxCustomerAge;

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getAllApplications() {
        log.debug("Loading all loan applications with schedules");
        List<LoanApplication> applications = loanApplicationRepository.findAllByOrderByCreatedAtDesc();
        log.debug("Loaded {} loan applications from database", applications.size());
        return mapApplicationsWithSchedules(applications);
    }

    @Transactional(readOnly = true)
    public List<LoanApplicationResponse> getInReviewApplications() {
        log.debug("Loading IN_REVIEW applications with schedules");
        List<LoanApplication> applications = loanApplicationRepository.findByStatusOrderByCreatedAtDesc(LoanStatus.IN_REVIEW);
        log.debug("Loaded {} IN_REVIEW applications from database", applications.size());
        return mapApplicationsWithSchedules(applications);
    }


    // Helper method for not getting N + 1 problem, instead its always 2 queries.
    private List<LoanApplicationResponse> mapApplicationsWithSchedules(List<LoanApplication> applications) {
        if (applications.isEmpty()) {
            log.debug("No applications found, skipping schedule lookup");
            return List.of();
        }

        List<UUID> applicationIds = applications.stream()
                .map(LoanApplication::getId)
                .toList();

        Map<UUID, List<LoanPaymentSchedule>> schedulesByApplicationId = loanPaymentScheduleRepository
                .findByLoanApplicationIdInOrderByLoanApplicationIdAscPaymentNumberAsc(applicationIds)
                .stream()
                .collect(Collectors.groupingBy(LoanPaymentSchedule::getLoanApplicationId));


        return applications.stream()
                .map(application -> loanApplicationResponseMapper.toResponse(
                        application,
                        schedulesByApplicationId.getOrDefault(application.getId(), List.of())
                ))
                .toList();
    }

    @Transactional
    public LoanApplicationResponse createApplication(CreateLoanApplicationRequest request) {
        log.debug("Starting loan application creation workflow");
        if (!personalCodeValidator.isValid(request.getPersonalCode())) {
            log.warn("Rejected create request: invalid personal code format/checksum");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid Estonian personal code");
        }

        if (loanApplicationRepository.existsByPersonalCodeAndStatusIn(
                request.getPersonalCode(),
                LoanStatusConstants.ACTIVE_STATUSES
        )) {
            log.warn("Rejected create request: customer already has active application");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already has an active application");
        }

        LocalDate currentDate = LocalDate.now();
        LocalDate birthDate = personalCodeValidator.extractBirthDate(request.getPersonalCode());
        LoanApplication application = loanApplicationMapper.toNewEntity(request, birthDate);

        try {
            application = loanApplicationRepository.save(application);
            log.info("Persisted new loan application {} with initial status {}", application.getId(), application.getStatus());
        } catch (DataIntegrityViolationException ex) {
            log.warn("Create request failed due to active application uniqueness conflict", ex);
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Customer already has an active application", ex);
        }

        int age = Period.between(birthDate, currentDate).getYears();
        log.debug("Calculated applicant age={} for application {}", age, application.getId());
        if (age > maxCustomerAge) {
            application.setStatus(LoanStatus.REJECTED);
            application.setRejectionReason(CUSTOMER_TOO_OLD);
            LoanApplication rejected = loanApplicationRepository.save(application);
            log.info("Auto-rejected application {} due to age {} > max {}", rejected.getId(), age, maxCustomerAge);
            return loanApplicationResponseMapper.toResponse(rejected, Collections.emptyList());
        }

        List<LoanPaymentSchedule> schedule;
        try {
            schedule = paymentScheduleService.buildAnnuitySchedule(application, currentDate);
        } catch (RuntimeException ex) {
            log.error("Failed generating payment schedule for application {}", application.getId(), ex);
            throw ex;
        }

        loanPaymentScheduleRepository.saveAll(schedule);
        log.debug("Saved {} payment schedule rows for application {}", schedule.size(), application.getId());

        application.setStatus(LoanStatus.IN_REVIEW);
        LoanApplication inReview = loanApplicationRepository.save(application);
        log.info("Application {} moved to status {}", inReview.getId(), inReview.getStatus());
        return loanApplicationResponseMapper.toResponse(inReview, schedule);
    }

    @Transactional
    public LoanApplicationResponse regenerateSchedule(UUID id, RegenerateScheduleRequest request) {
        LoanApplication application = loanApplicationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan application not found: " + id));

        if (application.getStatus() != LoanStatus.IN_REVIEW) {
            throw new IllegalStateException(
                    "Schedule regeneration is only allowed for applications in IN_REVIEW status, " +
                            "but current status is: " + application.getStatus()
            );
        }

        log.info("Regenerating payment schedule for application {}", id);

        // Kustuta vana graafik
        loanPaymentScheduleRepository.deleteByLoanApplicationId(id);

        // Uuenda parameetrid
        application.setInterestMargin(request.getInterestMargin());
        application.setBaseInterestRate(request.getBaseInterestRate());
        application.setLoanPeriodMonths(request.getLoanPeriodMonths());
        loanApplicationRepository.save(application);

        // Genereeri uus graafik
        List<LoanPaymentSchedule> schedule = paymentScheduleService.buildAnnuitySchedule(
                application, LocalDate.now()
        );
        loanPaymentScheduleRepository.saveAll(schedule);

        log.info("Schedule regenerated with {} entries for application {}", schedule.size(), id);

        return loanApplicationResponseMapper.toResponse(application, schedule);
    }

    @Transactional
    public DecisionResponse approve(UUID id) {
        LoanApplication application = findById(id);
        if (application.getStatus() != LoanStatus.IN_REVIEW) {
            log.warn("Approve rejected for application {} because status is {}", id, application.getStatus());
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
            log.warn("Reject denied for application {} because status is {}", id, application.getStatus());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only IN_REVIEW applications can be rejected");
        }

        String manualReason = request.getReason().trim();
        application.setStatus(LoanStatus.REJECTED);
        application.setRejectionReason(manualReason);
        LoanApplication saved = loanApplicationRepository.save(application);
        log.info("Application {} rejected manually", saved.getId());
        log.debug("Manual rejection reason for application {}: {}", saved.getId(), manualReason);
        return new DecisionResponse(saved.getId(), saved.getStatus(), saved.getRejectionReason());
    }

    private LoanApplication findById(UUID id) {
        return loanApplicationRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Loan application {} was not found", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Loan application not found");
                });
    }

}


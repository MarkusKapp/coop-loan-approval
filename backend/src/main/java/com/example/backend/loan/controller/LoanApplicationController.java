package com.example.backend.loan.controller;

import com.example.backend.loan.dto.CreateLoanApplicationRequest;
import com.example.backend.loan.dto.DecisionResponse;
import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.RegenerateScheduleRequest;
import com.example.backend.loan.dto.RejectLoanApplicationRequest;
import com.example.backend.loan.exception.ApiErrorResponse;
import com.example.backend.loan.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loan Applications", description = "Endpoints for creating and reviewing loan applications")
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @GetMapping
    @Operation(summary = "Get all loan applications")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applications returned successfully")
    })
    public List<LoanApplicationResponse> getAll() {
        log.info("Fetching all loan applications");
        List<LoanApplicationResponse> applications = loanApplicationService.getAllApplications();
        log.debug("Fetched {} loan applications", applications.size());
        return applications;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan application by ID with payment schedule")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application returned successfully"),
            @ApiResponse(responseCode = "404", description = "Loan application not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public LoanApplicationResponse getById(@PathVariable UUID id) {
        log.info("Fetching loan application {}", id);
        return loanApplicationService.getApplicationById(id);
    }

    @GetMapping("/in-review")
    @Operation(summary = "Get IN_REVIEW loan applications for manual review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "In-review applications returned successfully")
    })
    public List<LoanApplicationResponse> getInReview() {
        log.info("Fetching IN_REVIEW applications");
        List<LoanApplicationResponse> applications = loanApplicationService.getInReviewApplications();
        log.debug("Fetched {} IN_REVIEW applications", applications.size());
        return applications;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a loan application and run automatic workflow")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Loan application created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or personal code is invalid",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Customer already has an active application",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public LoanApplicationResponse create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        log.info("Received create loan application request");
        LoanApplicationResponse response = loanApplicationService.createApplication(request);
        log.info("Created loan application {} with status {}", response.getId(), response.getStatus());
        return response;
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve loan application in review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan application approved successfully"),
            @ApiResponse(responseCode = "404", description = "Loan application not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Only IN_REVIEW applications can be approved",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DecisionResponse approve(@PathVariable UUID id) {
        log.info("Received approve request for application {}", id);
        DecisionResponse response = loanApplicationService.approve(id);
        log.info("Application {} approved with final status {}", id, response.getStatus());
        return response;
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject loan application in review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Loan application rejected successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for rejection payload",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Loan application not found",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Only IN_REVIEW applications can be rejected",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public DecisionResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectLoanApplicationRequest request) {
        log.info("Received reject request for application {}", id);
        DecisionResponse response = loanApplicationService.reject(id, request);
        log.info("Application {} rejected with reason '{}'", id, response.getRejectionReason());
        return response;
    }

    @PutMapping("/{id}/regenerate-schedule")
    @Operation(summary = "Regenerate payment schedule for application in review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Payment schedule regenerated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for regeneration payload",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected error during regeneration",
                    content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    public LoanApplicationResponse regenerateSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody RegenerateScheduleRequest request) {
        log.info("Received regenerate schedule request for application {}", id);
        LoanApplicationResponse response = loanApplicationService.regenerateSchedule(id, request);
        log.info("Schedule regenerated for application {} with status {}", id, response.getStatus());
        return response;
    }
}

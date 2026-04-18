package com.example.backend.loan.controller;

import com.example.backend.loan.dto.CreateLoanApplicationRequest;
import com.example.backend.loan.dto.DecisionResponse;
import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.RejectLoanApplicationRequest;
import com.example.backend.loan.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/loan-applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    @GetMapping
    @Operation(summary = "Get all loan applications")
    public List<LoanApplicationResponse> getAll() {
        return loanApplicationService.getAllApplications();
    }

    @GetMapping("/in-review")
    @Operation(summary = "Get IN_REVIEW loan applications for manual review")
    public List<LoanApplicationResponse> getInReview() {
        return loanApplicationService.getInReviewApplications();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a loan application and run automatic workflow")
    public LoanApplicationResponse create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        return loanApplicationService.createApplication(request);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve loan application in review")
    public DecisionResponse approve(@PathVariable UUID id) {
        return loanApplicationService.approve(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject loan application in review")
    public DecisionResponse reject(@PathVariable UUID id, @Valid @RequestBody RejectLoanApplicationRequest request) {
        return loanApplicationService.reject(id, request);
    }
}

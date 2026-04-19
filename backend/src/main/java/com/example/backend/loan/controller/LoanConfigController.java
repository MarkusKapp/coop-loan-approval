package com.example.backend.loan.controller;

import com.example.backend.loan.dto.UpdateMaxAgeRequest;
import com.example.backend.loan.dto.UpdateEuriborRequest;
import com.example.backend.loan.entity.LoanConfig;
import com.example.backend.loan.service.LoanConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loan-config")
@RequiredArgsConstructor
@Slf4j
public class LoanConfigController {

    private final LoanConfigService loanConfigService;

    @GetMapping
    @Operation(summary = "Get all loan configuration parameters")
    public List<LoanConfig> getAll() {
        return loanConfigService.getAll();
    }


    @PutMapping("/euribor")
    @Operation(summary = "Update 6M Euribor value in database")
    public LoanConfig updateEuribor(@RequestBody @Valid UpdateEuriborRequest request) {
        return loanConfigService.setEuribor(request.getValue());
    }

    @PutMapping("/max-age")
    @Operation(summary = "Update maximum customer age")
    public LoanConfig updateMaxAge(@RequestBody @Valid UpdateMaxAgeRequest request) {
        return loanConfigService.setMaxCustomerAge(request.getValue());
    }
}
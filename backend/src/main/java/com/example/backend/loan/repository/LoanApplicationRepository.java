package com.example.backend.loan.repository;

import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.LoanStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.UUID;

public interface LoanApplicationRepository extends JpaRepository<LoanApplication, UUID> {

	boolean existsByPersonalCodeAndStatusIn(String personalCode, Collection<LoanStatus> statuses);
}




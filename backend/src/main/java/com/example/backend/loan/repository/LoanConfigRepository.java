package com.example.backend.loan.repository;

import com.example.backend.loan.entity.LoanConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanConfigRepository extends JpaRepository<LoanConfig, String> {
}
package com.example.backend.loan.repository;

import com.example.backend.loan.entity.LoanPaymentSchedule;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface LoanPaymentScheduleRepository extends JpaRepository<LoanPaymentSchedule, UUID> {

    List<LoanPaymentSchedule> findByLoanApplicationIdOrderByPaymentNumberAsc(UUID loanApplicationId);

    List<LoanPaymentSchedule> findByLoanApplicationIdInOrderByLoanApplicationIdAscPaymentNumberAsc(
            Collection<UUID> loanApplicationIds
    );
}




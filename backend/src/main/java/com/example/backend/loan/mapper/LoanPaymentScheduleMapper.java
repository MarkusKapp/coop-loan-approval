package com.example.backend.loan.mapper;

import com.example.backend.loan.entity.LoanPaymentSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanPaymentScheduleMapper {

    @Mapping(target = "id", expression = "java(java.util.UUID.randomUUID())")
    LoanPaymentSchedule toEntity(
            UUID loanApplicationId,
            Integer paymentNumber,
            LocalDate paymentDate,
            BigDecimal principal,
            BigDecimal interest,
            BigDecimal totalPayment
    );
}


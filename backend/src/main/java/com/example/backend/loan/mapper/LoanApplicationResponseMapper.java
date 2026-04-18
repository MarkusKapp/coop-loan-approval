package com.example.backend.loan.mapper;

import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.PaymentScheduleItemResponse;
import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.entity.LoanPaymentSchedule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanApplicationResponseMapper {

    @Mapping(target = "id", source = "app.id")
    @Mapping(target = "firstName", source = "app.firstName")
    @Mapping(target = "lastName", source = "app.lastName")
    @Mapping(target = "personalCode", source = "app.personalCode")
    @Mapping(target = "loanPeriodMonths", source = "app.loanPeriodMonths")
    @Mapping(target = "interestMargin", source = "app.interestMargin")
    @Mapping(target = "baseInterestRate", source = "app.baseInterestRate")
    @Mapping(target = "loanAmount", source = "app.loanAmount")
    @Mapping(target = "status", source = "app.status")
    @Mapping(target = "rejectionReason", source = "app.rejectionReason")
    @Mapping(target = "birthDate", source = "app.birthDate")
    @Mapping(target = "paymentSchedule", source = "schedule")
    LoanApplicationResponse toResponse(LoanApplication app, List<LoanPaymentSchedule> schedule);

    PaymentScheduleItemResponse toPaymentScheduleItemResponse(LoanPaymentSchedule item);
}




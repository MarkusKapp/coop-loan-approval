package com.example.backend.loan.mapper;

import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.dto.CreateLoanApplicationRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDate;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface LoanApplicationMapper {

    @Mapping(target = "birthDate", source = "birthDate")
    @Mapping(target = "status", expression = "java(com.example.backend.loan.LoanStatus.STARTED)")
    LoanApplication toNewEntity(CreateLoanApplicationRequest request, LocalDate birthDate);
}





package com.example.backend.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "loan_payment_schedule")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanPaymentSchedule {

	@Id
	private UUID id;

	@Column(name = "loan_application_id", nullable = false)
	private UUID loanApplicationId;

	@Column(name = "payment_number", nullable = false)
	private Integer paymentNumber;

	@Column(name = "payment_date", nullable = false)
	private LocalDate paymentDate;

	@Column(name = "principal", nullable = false, precision = 12, scale = 2)
	private BigDecimal principal;

	@Column(name = "interest", nullable = false, precision = 12, scale = 2)
	private BigDecimal interest;

	@Column(name = "total_payment", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalPayment;
}




package com.example.backend.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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

	@NotNull
	@Column(name = "loan_application_id", nullable = false)
	private UUID loanApplicationId;

	@NotNull
	@Min(1)
	@Column(name = "payment_number", nullable = false)
	private Integer paymentNumber;

	@NotNull
	@Column(name = "payment_date", nullable = false)
	private LocalDate paymentDate;

	@NotNull
	@DecimalMin("0.00")
	@Column(name = "principal", nullable = false, precision = 12, scale = 2)
	private BigDecimal principal;

	@NotNull
	@DecimalMin("0.00")
	@Column(name = "interest", nullable = false, precision = 12, scale = 2)
	private BigDecimal interest;

	@NotNull
	@DecimalMin("0.00")
	@Column(name = "total_payment", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalPayment;
}




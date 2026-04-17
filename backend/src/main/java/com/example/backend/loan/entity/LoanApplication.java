package com.example.backend.loan.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.backend.loan.LoanStatus;
import com.example.backend.loan.RejectionReason;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_application")
@Getter
@Setter
@NoArgsConstructor
public class LoanApplication {

	@Id
	private UUID id;

	@Column(name = "first_name", nullable = false, length = 32)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 32)
	private String lastName;

	@Column(name = "personal_code", nullable = false, length = 11)
	private String personalCode;

	@Column(name = "birth_date")
	private LocalDate birthDate;

	@Column(name = "loan_period_months", nullable = false)
	private Integer loanPeriodMonths;

	@Column(name = "interest_margin", nullable = false, precision = 5, scale = 3)
	private BigDecimal interestMargin;

	@Column(name = "base_interest_rate", nullable = false, precision = 5, scale = 3)
	private BigDecimal baseInterestRate;

	@Column(name = "loan_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal loanAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private LoanStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "rejection_reason", length = 50)
	private RejectionReason rejectionReason;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	@PrePersist
	void prePersist() {
		LocalDateTime now = LocalDateTime.now();
		if (id == null) {
			id = UUID.randomUUID();
		}
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void preUpdate() {
		updatedAt = LocalDateTime.now();
	}
}






package com.example.backend.loan;

import java.util.EnumSet;
import java.util.Set;

public final class LoanStatusConstants {

	public static final Set<LoanStatus> ACTIVE_STATUSES = EnumSet.of(LoanStatus.STARTED, LoanStatus.IN_REVIEW);

	private LoanStatusConstants() {
	}
}


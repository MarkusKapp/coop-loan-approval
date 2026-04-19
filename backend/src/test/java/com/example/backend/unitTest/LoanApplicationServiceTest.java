package com.example.backend.unitTest;

import com.example.backend.loan.service.LoanApplicationService;
import com.example.backend.loan.service.PaymentScheduleService;
import com.example.backend.personalCodeTest.TestPersonalCodeFactory;
import com.example.backend.loan.LoanStatus;
import com.example.backend.loan.dto.CreateLoanApplicationRequest;
import com.example.backend.loan.dto.LoanApplicationResponse;
import com.example.backend.loan.dto.PaymentScheduleItemResponse;
import com.example.backend.loan.dto.RejectLoanApplicationRequest;
import com.example.backend.loan.entity.LoanApplication;
import com.example.backend.loan.entity.LoanPaymentSchedule;
import com.example.backend.loan.mapper.LoanApplicationMapper;
import com.example.backend.loan.mapper.LoanApplicationResponseMapper;
import com.example.backend.loan.repository.LoanApplicationRepository;
import com.example.backend.loan.repository.LoanPaymentScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceTest {

	private static final LocalDate VALID_BIRTH_DATE = LocalDate.of(1990, 1, 15);
	private static final LocalDate TOO_OLD_BIRTH_DATE = LocalDate.of(1940, 3, 11);

	@Mock
	private LoanApplicationRepository loanApplicationRepository;

	@Mock
	private LoanPaymentScheduleRepository loanPaymentScheduleRepository;

	@Mock
	private PaymentScheduleService paymentScheduleService;

	@Mock
	private com.example.backend.EstonianPersonalCodeValidator personalCodeValidator;

	@Mock
	private LoanApplicationMapper loanApplicationMapper;

	@Mock
	private LoanApplicationResponseMapper loanApplicationResponseMapper;

	@InjectMocks
	private LoanApplicationService service;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(service, "maxCustomerAge", 70);
	}

	@Test
	void createApplicationRejectsInvalidPersonalCode() {
		CreateLoanApplicationRequest request = req("123");
		when(personalCodeValidator.isValid(request.getPersonalCode())).thenReturn(false);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createApplication(request));

		assertEquals(400, ex.getStatusCode().value());
		assertEquals("Invalid Estonian personal code", ex.getReason());
		verify(loanApplicationRepository, never()).save(any());
	}

	@Test
	void createApplicationRejectsDuplicateActiveApplication() {
		String code = TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 123);
		CreateLoanApplicationRequest request = req(code);

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(true);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createApplication(request));

		assertEquals(409, ex.getStatusCode().value());
		assertEquals("Customer already has an active application", ex.getReason());
		verify(loanApplicationRepository, never()).save(any());
	}

	@Test
	void createApplicationCreatesInReviewApplicationWithSchedule() {
		String code = TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 111);
		CreateLoanApplicationRequest request = req(code);
		LoanApplication mapped = app(UUID.randomUUID(), code, LoanStatus.STARTED, VALID_BIRTH_DATE, LocalDateTime.now());
		List<LoanPaymentSchedule> schedule = List.of(schedule(mapped.getId(), 1), schedule(mapped.getId(), 2));
		LoanApplicationResponse expectedResponse = response(mapped, LoanStatus.IN_REVIEW, null, schedule.size());

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(false);
		when(personalCodeValidator.extractBirthDate(code)).thenReturn(VALID_BIRTH_DATE);
		when(loanApplicationMapper.toNewEntity(request, VALID_BIRTH_DATE)).thenReturn(mapped);
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(paymentScheduleService.buildAnnuitySchedule(eq(mapped), any(LocalDate.class))).thenReturn(schedule);
		when(loanApplicationResponseMapper.toResponse(mapped, schedule)).thenReturn(expectedResponse);

		LoanApplicationResponse actual = service.createApplication(request);

		assertEquals(LoanStatus.IN_REVIEW, actual.getStatus());
		assertEquals(schedule.size(), actual.getPaymentSchedule().size());
		assertNull(actual.getRejectionReason());
		verify(loanPaymentScheduleRepository).saveAll(schedule);
		verify(loanApplicationRepository, times(2)).save(mapped);
	}

	@Test
	void createApplicationAutoRejectsTooOldApplicant() {
		String code = TestPersonalCodeFactory.personalCode(3, TOO_OLD_BIRTH_DATE, 222);
		CreateLoanApplicationRequest request = req(code);
		LoanApplication mapped = app(UUID.randomUUID(), code, LoanStatus.STARTED, TOO_OLD_BIRTH_DATE, LocalDateTime.now());
		LoanApplicationResponse expectedResponse = response(mapped, LoanStatus.REJECTED, "CUSTOMER_TOO_OLD", 0);

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(false);
		when(personalCodeValidator.extractBirthDate(code)).thenReturn(TOO_OLD_BIRTH_DATE);
		when(loanApplicationMapper.toNewEntity(request, TOO_OLD_BIRTH_DATE)).thenReturn(mapped);
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(loanApplicationResponseMapper.toResponse(mapped, List.of())).thenReturn(expectedResponse);

		LoanApplicationResponse actual = service.createApplication(request);

		assertEquals(LoanStatus.REJECTED, actual.getStatus());
		assertEquals("CUSTOMER_TOO_OLD", actual.getRejectionReason());
		verify(paymentScheduleService, never()).buildAnnuitySchedule(any(LoanApplication.class), any(LocalDate.class));
		verify(loanPaymentScheduleRepository, never()).saveAll(anyList());
	}

	@Test
	void createApplicationMapsDataIntegrityViolationToConflict() {
		String code = TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 223);
		CreateLoanApplicationRequest request = req(code);
		LoanApplication mapped = app(UUID.randomUUID(), code, LoanStatus.STARTED, VALID_BIRTH_DATE, LocalDateTime.now());

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(false);
		when(personalCodeValidator.extractBirthDate(code)).thenReturn(VALID_BIRTH_DATE);
		when(loanApplicationMapper.toNewEntity(request, VALID_BIRTH_DATE)).thenReturn(mapped);
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenThrow(new DataIntegrityViolationException("duplicate active"));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.createApplication(request));

		assertEquals(409, ex.getStatusCode().value());
		assertEquals("Customer already has an active application", ex.getReason());
		verifyNoInteractions(paymentScheduleService);
		verify(loanPaymentScheduleRepository, never()).saveAll(anyList());
	}

	@Test
	void createApplicationPropagatesScheduleGenerationFailure() {
		String code = TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 224);
		CreateLoanApplicationRequest request = req(code);
		LoanApplication mapped = app(UUID.randomUUID(), code, LoanStatus.STARTED, VALID_BIRTH_DATE, LocalDateTime.now());

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(false);
		when(personalCodeValidator.extractBirthDate(code)).thenReturn(VALID_BIRTH_DATE);
		when(loanApplicationMapper.toNewEntity(request, VALID_BIRTH_DATE)).thenReturn(mapped);
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(paymentScheduleService.buildAnnuitySchedule(eq(mapped), any(LocalDate.class))).thenThrow(new IllegalStateException("schedule generation failed"));

		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> service.createApplication(request));

		assertEquals("schedule generation failed", ex.getMessage());
		verify(loanApplicationRepository, times(1)).save(mapped);
		verify(loanPaymentScheduleRepository, never()).saveAll(anyList());
	}

	@Test
	void approveAndRejectUpdateStatuses() {
		LoanApplication inReview = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 333), LoanStatus.IN_REVIEW, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(inReview.getId())).thenReturn(Optional.of(inReview));
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var approved = service.approve(inReview.getId());

		assertEquals(LoanStatus.APPROVED, approved.getStatus());
		assertNull(approved.getRejectionReason());

		LoanApplication another = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 334), LoanStatus.IN_REVIEW, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(another.getId())).thenReturn(Optional.of(another));

		var rejected = service.reject(another.getId(), RejectLoanApplicationRequest.builder().reason("  too risky  ").build());

		assertEquals(LoanStatus.REJECTED, rejected.getStatus());
		assertEquals("too risky", rejected.getRejectionReason());
	}

	@Test
	void approveRejectsMissingAndRejectRejectsNonReviewApplications() {
		UUID missingId = UUID.randomUUID();
		when(loanApplicationRepository.findById(missingId)).thenReturn(Optional.empty());

		ResponseStatusException notFound = assertThrows(ResponseStatusException.class, () -> service.approve(missingId));
		assertEquals(404, notFound.getStatusCode().value());

		LoanApplication approved = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 335), LoanStatus.APPROVED, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(approved.getId())).thenReturn(Optional.of(approved));

		ResponseStatusException conflict = assertThrows(ResponseStatusException.class,
				() -> service.reject(approved.getId(), RejectLoanApplicationRequest.builder().reason("nope").build()));
		assertEquals(409, conflict.getStatusCode().value());
	}

	@Test
	void approveRejectsNonInReviewApplications() {
		LoanApplication approved = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 336), LoanStatus.APPROVED, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(approved.getId())).thenReturn(Optional.of(approved));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.approve(approved.getId()));

		assertEquals(409, ex.getStatusCode().value());
		assertEquals("Only IN_REVIEW applications can be approved", ex.getReason());
	}

	@Test
	void rejectMissingApplicationReturnsNotFound() {
		UUID missingId = UUID.randomUUID();
		when(loanApplicationRepository.findById(missingId)).thenReturn(Optional.empty());

		ResponseStatusException ex = assertThrows(ResponseStatusException.class,
				() -> service.reject(missingId, RejectLoanApplicationRequest.builder().reason("missing").build()));

		assertEquals(404, ex.getStatusCode().value());
		assertEquals("Loan application not found", ex.getReason());
	}

	@Test
	void rejectWithNullReasonThrowsNullPointerException() {
		LoanApplication inReview = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 337), LoanStatus.IN_REVIEW, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(inReview.getId())).thenReturn(Optional.of(inReview));

		NullPointerException ex = assertThrows(NullPointerException.class,
				() -> service.reject(inReview.getId(), RejectLoanApplicationRequest.builder().reason(null).build()));

		assertTrue(ex.getMessage() == null || ex.getMessage().contains("trim"));
		verify(loanApplicationRepository, never()).save(any(LoanApplication.class));
	}

	@Test
	void rejectWithBlankReasonStoresTrimmedEmptyReason() {
		LoanApplication inReview = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 338), LoanStatus.IN_REVIEW, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(inReview.getId())).thenReturn(Optional.of(inReview));
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var response = service.reject(inReview.getId(), RejectLoanApplicationRequest.builder().reason("   ").build());

		assertEquals(LoanStatus.REJECTED, response.getStatus());
		assertEquals("", response.getRejectionReason());
	}

	@Test
	void createApplicationPropagatesExtractBirthDateFailure() {
		String code = TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 339);
		CreateLoanApplicationRequest request = req(code);

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(false);
		when(personalCodeValidator.extractBirthDate(code)).thenThrow(new IllegalArgumentException("bad birth date"));

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.createApplication(request));

		assertEquals("bad birth date", ex.getMessage());
		verify(loanApplicationRepository, never()).save(any(LoanApplication.class));
		verifyNoInteractions(paymentScheduleService);
	}

	@Test
	void createApplicationPropagatesScheduleSaveFailureAndSkipsFinalStatusTransition() {
		String code = TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 340);
		CreateLoanApplicationRequest request = req(code);
		LoanApplication mapped = app(UUID.randomUUID(), code, LoanStatus.STARTED, VALID_BIRTH_DATE, LocalDateTime.now());
		List<LoanPaymentSchedule> schedule = List.of(schedule(mapped.getId(), 1));

		when(personalCodeValidator.isValid(code)).thenReturn(true);
		when(loanApplicationRepository.existsByPersonalCodeAndStatusIn(eq(code), any())).thenReturn(false);
		when(personalCodeValidator.extractBirthDate(code)).thenReturn(VALID_BIRTH_DATE);
		when(loanApplicationMapper.toNewEntity(request, VALID_BIRTH_DATE)).thenReturn(mapped);
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(paymentScheduleService.buildAnnuitySchedule(eq(mapped), any(LocalDate.class))).thenReturn(schedule);
		when(loanPaymentScheduleRepository.saveAll(schedule)).thenThrow(new RuntimeException("db write failed"));

		RuntimeException ex = assertThrows(RuntimeException.class, () -> service.createApplication(request));

		assertEquals("db write failed", ex.getMessage());
		verify(loanApplicationRepository, times(1)).save(mapped);
		verify(loanApplicationResponseMapper, never()).toResponse(any(LoanApplication.class), anyList());
	}

	@Test
	void approveTwiceReturnsConflictOnSecondCall() {
		LoanApplication inReview = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 341), LoanStatus.IN_REVIEW, VALID_BIRTH_DATE, LocalDateTime.now());
		when(loanApplicationRepository.findById(inReview.getId())).thenReturn(Optional.of(inReview));
		when(loanApplicationRepository.save(any(LoanApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

		var firstResponse = service.approve(inReview.getId());
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.approve(inReview.getId()));

		assertEquals(LoanStatus.APPROVED, firstResponse.getStatus());
		assertEquals(409, ex.getStatusCode().value());
		assertEquals("Only IN_REVIEW applications can be approved", ex.getReason());
		verify(loanApplicationRepository, times(1)).save(inReview);
	}

	@Test
	void getAllAndInReviewApplicationsMapWithSchedules() {
		LoanApplication first = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 401), LoanStatus.IN_REVIEW, VALID_BIRTH_DATE, LocalDateTime.now());
		LoanApplication second = app(UUID.randomUUID(), TestPersonalCodeFactory.personalCode(3, VALID_BIRTH_DATE, 402), LoanStatus.APPROVED, VALID_BIRTH_DATE, LocalDateTime.now().minusDays(1));

		List<LoanPaymentSchedule> allSchedules = List.of(schedule(first.getId(), 1), schedule(second.getId(), 1), schedule(first.getId(), 2));
		when(loanApplicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(first, second));
		when(loanApplicationRepository.findByStatusOrderByCreatedAtDesc(LoanStatus.IN_REVIEW)).thenReturn(List.of(first));
		when(loanPaymentScheduleRepository.findByLoanApplicationIdInOrderByLoanApplicationIdAscPaymentNumberAsc(any())).thenReturn(allSchedules);
		when(loanApplicationResponseMapper.toResponse(eq(first), anyList())).thenAnswer(invocation -> response(first, first.getStatus(), null, ((List<?>) invocation.getArgument(1)).size()));
		when(loanApplicationResponseMapper.toResponse(eq(second), anyList())).thenAnswer(invocation -> response(second, second.getStatus(), null, ((List<?>) invocation.getArgument(1)).size()));

		List<LoanApplicationResponse> all = service.getAllApplications();
		List<LoanApplicationResponse> inReview = service.getInReviewApplications();

		assertEquals(2, all.size());
		assertEquals(2, all.get(0).getPaymentSchedule().size());
		assertEquals(1, all.get(1).getPaymentSchedule().size());
		assertEquals(1, inReview.size());
		assertEquals(first.getId(), inReview.get(0).getId());
	}

	@Test
	void getAllAndInReviewApplicationsReturnEmptyWithoutScheduleLookup() {
		when(loanApplicationRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());
		when(loanApplicationRepository.findByStatusOrderByCreatedAtDesc(LoanStatus.IN_REVIEW)).thenReturn(List.of());

		List<LoanApplicationResponse> all = service.getAllApplications();
		List<LoanApplicationResponse> inReview = service.getInReviewApplications();

		assertTrue(all.isEmpty());
		assertTrue(inReview.isEmpty());
		verify(loanPaymentScheduleRepository, never()).findByLoanApplicationIdInOrderByLoanApplicationIdAscPaymentNumberAsc(any());
	}

	private static CreateLoanApplicationRequest req(String code) {
		return CreateLoanApplicationRequest.builder()
				.firstName("Mark")
				.lastName("Cooper")
				.personalCode(code)
				.loanPeriodMonths(6)
				.interestMargin(BigDecimal.valueOf(2.10))
				.baseInterestRate(BigDecimal.valueOf(4.00))
				.loanAmount(BigDecimal.valueOf(12000))
				.build();
	}

	private static LoanApplication app(UUID id, String code, LoanStatus status, LocalDate birthDate, LocalDateTime createdAt) {
		LoanApplication application = new LoanApplication();
		application.setId(id);
		application.setFirstName("Mark");
		application.setLastName("Cooper");
		application.setPersonalCode(code);
		application.setBirthDate(birthDate);
		application.setLoanPeriodMonths(6);
		application.setInterestMargin(BigDecimal.valueOf(2.10));
		application.setBaseInterestRate(BigDecimal.valueOf(4.00));
		application.setLoanAmount(BigDecimal.valueOf(12000));
		application.setStatus(status);
		application.setCreatedAt(createdAt);
		application.setUpdatedAt(createdAt);
		return application;
	}

	private static LoanPaymentSchedule schedule(UUID applicationId, int paymentNumber) {
		return LoanPaymentSchedule.builder()
				.id(UUID.randomUUID())
				.loanApplicationId(applicationId)
				.paymentNumber(paymentNumber)
				.paymentDate(LocalDate.now().plusMonths(paymentNumber - 1L))
				.principal(BigDecimal.valueOf(100))
				.interest(BigDecimal.valueOf(10))
				.totalPayment(BigDecimal.valueOf(110))
				.build();
	}

	private static LoanApplicationResponse response(LoanApplication app, LoanStatus status, String rejectionReason, int items) {
		return LoanApplicationResponse.builder()
				.id(app.getId())
				.status(status)
				.rejectionReason(rejectionReason)
				.paymentSchedule(java.util.stream.IntStream.range(0, items)
						.mapToObj(i -> new PaymentScheduleItemResponse())
						.toList())
				.build();
	}
}





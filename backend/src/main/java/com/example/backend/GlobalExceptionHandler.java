package com.example.backend;

import com.example.backend.api.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
		List<String> details = ex.getBindingResult().getFieldErrors().stream()
				.map(error -> error.getField() + ": " + Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid request."))
				.toList();

		log.warn("Validation error on {}: {}", request.getRequestURI(), details);
		return ResponseEntity.badRequest().body(ApiErrorResponse.of(
				HttpStatus.BAD_REQUEST,
				details.isEmpty() ? "Invalid request." : "Validation failed.",
				request.getRequestURI(),
				details
		));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpServletRequest request) {
		log.warn("Malformed or unreadable request body on {}", request.getRequestURI());
		return ResponseEntity.badRequest().body(ApiErrorResponse.of(
				HttpStatus.BAD_REQUEST,
				"Malformed or missing request body.",
				request.getRequestURI(),
				List.of()
		));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
		HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
		String message = Objects.requireNonNullElse(ex.getReason(), "Request failed.");

		if (status.is4xxClientError()) {
			log.warn("{} on {}: {}", status.value(), request.getRequestURI(), message);
		} else {
			log.error("{} on {}: {}", status.value(), request.getRequestURI(), message, ex);
		}

		return ResponseEntity.status(status).body(ApiErrorResponse.of(status, message, request.getRequestURI(), List.of()));
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
		log.warn("Data integrity violation on {}", request.getRequestURI(), ex);
		HttpStatus status = HttpStatus.CONFLICT;
		return ResponseEntity.status(status).body(ApiErrorResponse.of(
				status,
				"Data integrity violation.",
				request.getRequestURI(),
				List.of()
		));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
		String message = "Invalid value for parameter '" + ex.getName() + "'.";
		log.warn("Type mismatch on {}: {}", request.getRequestURI(), message);
		return ResponseEntity.badRequest().body(ApiErrorResponse.of(
				HttpStatus.BAD_REQUEST,
				message,
				request.getRequestURI(),
				List.of()
		));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
		List<String> details = ex.getConstraintViolations().stream()
				.map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
				.toList();

		log.warn("Constraint violation on {}: {}", request.getRequestURI(), details);
		return ResponseEntity.badRequest().body(ApiErrorResponse.of(
				HttpStatus.BAD_REQUEST,
				"Validation failed.",
				request.getRequestURI(),
				details
		));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex, HttpServletRequest request) {
		log.error("Unexpected error on {}", request.getRequestURI(), ex);
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(status).body(ApiErrorResponse.of(
				status,
				"An unexpected error occurred.",
				request.getRequestURI(),
				List.of()
		));
	}
}


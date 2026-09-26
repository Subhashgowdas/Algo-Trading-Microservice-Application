package com.algotrade.authservice.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

import com.algotrade.common.exception.EmailSendException;
import com.algotrade.common.exception.RateLimitExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(RateLimitExceededException.class)
	public ResponseEntity<Map<String, Object>> handleRateLimit(RateLimitExceededException ex) {
		return buildResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage());
	}

	@ExceptionHandler(EmailSendException.class)
	public ResponseEntity<Map<String, Object>> handleEmailSend(EmailSendException ex) {
		log.error("Email send failed", ex);
		return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Email service temporarily unavailable");
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fieldErrors = new HashMap<>();
		ex.getBindingResult().getFieldErrors().forEach(error ->
		fieldErrors.put(error.getField(), error.getDefaultMessage()));
		Map<String, Object> body = new HashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("status", HttpStatus.BAD_REQUEST.value());
		body.put("error", "Validation failed");
		body.put("fields", fieldErrors);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
		log.error("Unhandled exception", ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
	}

	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<Map<String, Object>> handleInvalidRefreshToken(InvalidRefreshTokenException ex) {
		return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
	}

	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
		return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	@ExceptionHandler(UsernameNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleUsernameNotFound(UsernameNotFoundException ex) {
		return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password");
	}

	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<Map<String, Object>> handleEmailExists(EmailAlreadyExistsException ex) {
		return buildResponse(HttpStatus.CONFLICT, ex.getMessage());
	}

	@ExceptionHandler(InvalidOtpException.class)
	public ResponseEntity<Map<String, Object>>handelInvalidOtpException(InvalidOtpException ex){
		return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
	}

	@ExceptionHandler(AsyncRequestNotUsableException.class)
	public ResponseEntity<Map<String, Object>> handleAsyncRequestNotUsable(AsyncRequestNotUsableException ex) {
		log.warn("Client disconnected before response could be sent: {}", ex.getMessage());
		return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
	}

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
		Map<String, Object> body = new HashMap<>();
		body.put("timestamp", LocalDateTime.now());
		body.put("status", status.value());
		body.put("error", status.getReasonPhrase());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
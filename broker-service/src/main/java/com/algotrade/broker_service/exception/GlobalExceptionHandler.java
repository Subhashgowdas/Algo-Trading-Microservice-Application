package com.algotrade.broker_service.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AdapterNotExistException.class)
	public ResponseEntity<Map<String, Object>> handleEmailSend(AdapterNotExistException ex) {
		return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, "Email service temporarily unavailable");
	}

	private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
		Map<String, Object> body = new HashMap<>();
		body.put("status", status.value());
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
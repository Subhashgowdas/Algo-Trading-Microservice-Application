package com.algotrade.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterInitiateRequest(
		@NotBlank(message = "Email is required")
		@Email(message =  "Invalid email format")
		String email
		) {}

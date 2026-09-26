package com.algotrade.authservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterCompleteRequest(
		@NotBlank @Email 
		String email,
		
		@NotBlank @Size (min = 6, max = 6)
		String otp,
		
		@NotBlank @Size (min = 8,message = "Password must be at least 8 characters") 
		String password
		) {}

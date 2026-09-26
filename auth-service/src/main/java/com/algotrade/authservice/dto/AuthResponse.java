package com.algotrade.authservice.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType
		) {}

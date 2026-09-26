package com.algotrade.broker_service.dto;

public record BrokerCredentialResponse(
		boolean hasAccessToken,
		String authorizationUrl
		) {}

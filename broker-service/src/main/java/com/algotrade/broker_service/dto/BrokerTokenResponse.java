package com.algotrade.broker_service.dto;

public record BrokerTokenResponse(
		String accessToken,
		Long expiryEpochMillis
		) {}

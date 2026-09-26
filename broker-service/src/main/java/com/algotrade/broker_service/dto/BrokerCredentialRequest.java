package com.algotrade.broker_service.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO sent by the frontend when the user wants to connect a broker.
 * Only the broker name and the plain text API keys are required.
 * 
 */
public record BrokerCredentialRequest(
		@NotBlank(message = "Broker name is required")
		String brokerName,

		@NotBlank(message = "API key is required")
		String apiKey,

		@NotBlank(message = "API secret is required")
		String apiSecret

		) {}
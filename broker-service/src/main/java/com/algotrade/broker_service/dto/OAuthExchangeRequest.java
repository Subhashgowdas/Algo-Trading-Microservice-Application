package com.algotrade.broker_service.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthExchangeRequest(

		@NotBlank(message = "Broker name is required") 
		String brokerName,

		@NotBlank(message = "Request token is required") 
		String requestToken
		) {}

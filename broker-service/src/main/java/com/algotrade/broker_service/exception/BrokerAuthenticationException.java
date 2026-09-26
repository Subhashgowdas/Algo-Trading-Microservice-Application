package com.algotrade.broker_service.exception;

public class BrokerAuthenticationException extends BrokerException {

	public BrokerAuthenticationException(ErrorCode errorCode, String userMessage) {
		super(errorCode, userMessage);
	}

	public BrokerAuthenticationException(ErrorCode errorCode, String userMessage,
			String brokerErrorCode, String brokerRawMessage) {
		super(errorCode, userMessage, brokerErrorCode, brokerRawMessage, null, null);
	}
}

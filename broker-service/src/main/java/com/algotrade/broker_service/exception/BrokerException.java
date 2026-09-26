package com.algotrade.broker_service.exception;

/**
 * Enterprise grade exception thrown by broker adapters on any failure.
 * <p>
 * It carries a canonical {@link ErrorCode}, a human readable message, and
 * optionally the raw broker response for debugging.
 */
public class BrokerException extends RuntimeException{

	private final ErrorCode errorCode;
	private final String brokerErrorCode; 	// Broker's internal code (e.g., "DH-902", "UDAPI1179")
	private final String brokerRawMessage;
	private final String correlationId;		// ID to track async/multi-leg order failures
	private final Integer httpStatusCode;	// Raw HTTP status code (429, 500, etc.)

	public BrokerException(ErrorCode errorCode, String userMessage) {
		this(errorCode, userMessage,null,null,null,null);
	}

	public BrokerException(ErrorCode errorCode, String userMessage,String brokerErrorCode, String brokerRawMessage, String correlationId,
			Integer httpStatusCode) {
		super(userMessage);
		this.errorCode = errorCode;
		this.brokerErrorCode = brokerErrorCode;
		this.brokerRawMessage = brokerRawMessage;
		this.correlationId = correlationId;
		this.httpStatusCode = httpStatusCode;
	}

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public String getBrokerErrorCode() {
		return brokerErrorCode;
	}

	public String getBrokerRawMessage() {
		return brokerRawMessage;
	}

	public String getCorrelationId() {
		return correlationId;
	}

	public Integer getHttpStatusCode() {
		return httpStatusCode;
	}

	@Override
	public String toString() {
		return String.format(
				"BrokerException[code=%s, brokerCode='%s', http=%d, correlationId='%s', message='%s', brokerRaw='%s']",
				errorCode, brokerErrorCode, httpStatusCode, correlationId, getMessage(), brokerRawMessage
				);
	}
}

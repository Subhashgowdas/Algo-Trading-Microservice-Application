package com.algotrade.broker_service.model;

public record OrderResponse(
		String orderId,				// broker assigned internal order ID
		String exchangeOrderId,		// exchange-assigned order ID (null if rejected by broker)
		String correlationId,		// custom tag/unique ID sent in the request to map responses
		String parentOrderId,		// links child orders in multi-leg setups (Bracket/Cover orders)
		String status,				// "OPEN", "COMPLETE", "PART_TRADED", "REJECTED", "CANCELLED"
		int filledQuantity,			// quantity successfully executed
		int pendingQuantity,		// remaining quantity waiting to be filled
		double averagePrice,		// average execution price of filled quantity

		String errorCode,			// programmatic error code (e.g., "RMS_MARGIN_EXCEEDED")
		String message,				// human-readable rejection reason or broker note

		long orderTimestamp,		// epoch millis when broker received the API request
		long exchangeTimestamp		// epoch millis when exchange registered the order

		) {}

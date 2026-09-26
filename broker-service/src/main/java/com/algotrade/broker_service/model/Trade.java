package com.algotrade.broker_service.model;

/** 
 * Broker-agnostic executed trade (fill). 
 * Represents a single execution leg of an order.
 */
public record Trade(
		String tradeId,					// Exchange generated trade ID
		String orderId,					// Broker's internal unique order ID
		String exchangeOrderId,			// Exchange-generated order ID

		String symbol,					// e.g., "TATA"
		String exchange,				// "NSE", "BSE", "NFO", "MCX"
		String instrumentToken,			// Broker's numerical identifier for the instrument

		String transactionType,			// "BUY" or "SELL"
		String product,					// Margin product used (e.g., "CNC", "MIS", "NRML")

		int filledQuantity,				// Quantity filled in this specific trade
		double averagePrice,			// Price at which this specific quantity was executed

		long orderTimestamp,			// epoch millis when the original order was registered by the API
		long exchangeTimestamp,			// epoch millis when the order was registered by the exchange
		long fillTimestamp				// epoch millis when this specific trade was executed at the exchange

		) {}

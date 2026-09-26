package com.algotrade.broker_service.model;

/** 
 * Broker-agnostic real-time quote snapshot. 
 */
public record Quote(	

		String symbol,					// e.g., "GOOGLE"
		String exchange,				// "NSE", "BSE", "NFO", "MCX"
		String instrumentToken,			// Broker's numerical ID (crucial for mapping and websockets)

		// Price and Volume
		double lastTradedPrice,			// Last traded price
		int lastTradedQuantity,			// Quantity of the last actual trade
		long volume,					// Total traded volume for the day
		double averagePrice,			// Volume Weighted Average Price (VWAP / ATP)

		// Market Depth (Top of the book vs Overall)
		double bid,						// Best buy price
		double ask,						// Best sell price
		int bidVolume,					// Pending buy quantity at the best bid
		int askVolume,					// Pending sell quantity at the best ask
		int totalBuyQuantity,			// Total buy quantity pending across the entire order book
		int totalSellQuantity,			// Total sell quantity pending across the entire order book

		// OHLC
		double open,					
		double high,
		double low,
		double close,					// Previous day's close

		// Price Changes and Exchange Limits
		double change,					// Absolute price change from previous close
		double changePercent,			// Percentage price change
		double lowerCircuitLimit,		// Maximum allowed downward price movement
		double upperCircuitLimit,		// Maximum allowed upward price movement

		// Derivatives (F&O) Data
		double openInterest,			// Total number of outstanding F&O contracts

		// Timestamps
		long lastTradeTime,				// Epoch millis of the last actual trade
		long exchangeTimestamp			// Epoch millis of the overall quote snapshot


		) {}

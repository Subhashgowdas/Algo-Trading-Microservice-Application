package com.algotrade.broker_service.model;

public record OrderRequest(
		String symbol, 	            // e.g. "RELIANCE"
		String exchange,			// "NSE", "BSE", "NFO"
		String orderType,			// "MARKET", "LIMIT", "SL", "SL-M"
		String transactionType,     // "BUY", "SELL"
		String variety,				// "regular", "amo", "co", "iceberg"
		int quantity,				// stocks quantity , 10, 20 etc..
		double price,				// 0 for MARKET orders price 1873.3
		double triggerPrice,		// for SL, SL-M, CO orders
		String product,				// "CNC", "MIS", "NRML"
		String validity,			// "DAY", "IOC", "TTL"
		int validityTtl,			// lifespan in minutes for TTL validity orders
		int discloseQuantity,		// Quantity to disclose publicly
		int marketProtection,		// Market protection percentage
		String tag,					// optional user reference

		// Advanced order fields
		double targetPrice,			// Square-off value
		double stopLossPrice,		// Stop-loss value
		double trailingStoploss,	// Trailing stop-loss value
		
	    // Iceberg order fields
		int icebergLegs,			// Total number of legs (2 to 50)
		int icebergQuantity			// Split quantity for each leg
		) {}

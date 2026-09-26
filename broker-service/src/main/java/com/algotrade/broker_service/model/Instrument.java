package com.algotrade.broker_service.model;

/** 
 * Broker-agnostic master instrument entry. 
 * Represents a tradable asset and its exchange properties.
 */

public record Instrument(

		String symbol,				// e.g., "RELIANCE" or "NIFTY24MAY22000CE"
		String exchange,			// e.g., "NSE", "BSE", "NFO", "MCX"
		String name,				// Name of the company or underlying asset

		// Critical Identifiers
		String instrumentToken,		// Broker's numerical ID (required for WebSockets and API mapping)
		String exchangeToken,		// Exchange's internal token ID
		String isin,				// International Securities Identification Number (for equities)

		// Market Segments & Types
		String segment,				// e.g., "NSE_EQ", "NFO-OPT", "BSE"
		String instrumentType,		// e.g., "EQ" (Equity), "FUT" (Futures), "CE" (Call), "PE" (Put)

		// Trading Parameters
		int lotSize,				// Quantity in a single lot (Changed from double to int)
		double tickSize,			// Minimum price movement (e.g., 0.05)

		// Derivative (F&O) Specifics
		String expiry,				// Expiry date of the contract
		double strike				// Strike price for options (0.0 for equities/futures)

		) {}

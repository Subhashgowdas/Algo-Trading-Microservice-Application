package com.algotrade.broker_service.exception;

/**
 * Enterprise grade canonical error codes for all broker interactions.
 * <p>
 * Each code maps to a specific failure category that is independent of any
 * particular broker. Adapters translate broker specific errors into these
 * codes, allowing the rest of the platform to react consistently.
 */
public enum ErrorCode {

	// ---------- Order Placement & Validation ----------
	ORDER_REJECTED,

	PRICE_OUT_OF_RANGE,

	INSUFFICIENT_MARGIN,

	SYMBOL_NOT_FOUND,

	INVALID_QUANTITY,

	INVALID_PRODUCT,

	MODIFICATION_NOT_ALLOWED,

	/** The order size exceeds the exchange freeze limit; requires iceberg/slicing */
	FREEZE_QUANTITY_EXCEEDED,

	/** The trading segment (e.g., NFO, MCX) is not enabled for the user's account */
	SEGMENT_NOT_ENABLED,

	/** Broker RMS blocked buying far Out-Of-The-Money (OTM) options */
	OUT_OF_MONEY_BLOCKED,

	// ---------- Position & RMS ----------
	POSITION_CONVERSION_FAILED,

	POSITION_NOT_FOUND,

	RMS_BLOCKED,

	/** CDSL TPIN/OTP authorisation is required to sell delivery holdings (typically HTTP 428) */
	CDSL_AUTHORISATION_REQUIRED,

	// ---------- SEBI & Compliance ----------
	/** API request did not originate from the SEBI-mandated whitelisted Static IP */
	IP_NOT_WHITELISTED,

	/** Algo order firing exceeded exchange limits (e.g., > 10 or 20 orders/sec) */
	ALGO_RATE_LIMIT_EXCEEDED,

	// ---------- GTT / Conditional Orders ----------
	GTT_REJECTED,

	GTT_CANCELLATION_FAILED,

	// ---------- Connectivity & Rate Limiting ----------
	/** Standard API rate-limit error (HTTP 429) */
	RATE_LIMIT_EXCEEDED,

	BROKER_UNAVAILABLE,

	NETWORK_TIMEOUT,

	// ---------- Authentication ----------
	AUTHENTICATION_FAILED,

	/** The daily access token has expired (typically resets at 6:00 AM) */
	SESSION_EXPIRED, 

	/** Invalid TOTP or 2FA failure during automated login */
	INVALID_TOTP,

	// ---------- Data ----------
	QUOTE_NOT_AVAILABLE,

	HISTORICAL_DATA_UNAVAILABLE,

	// ---------- General ----------
	INTERNAL_ERROR,
	
	NOT_FOUND

}

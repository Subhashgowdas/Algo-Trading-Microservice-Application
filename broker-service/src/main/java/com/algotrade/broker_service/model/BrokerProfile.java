package com.algotrade.broker_service.model;

import java.util.List;

/** 
 * Broker account profile. 
 * Represents the user's trading account details, active segments, and compliance statuses.
 */

public record BrokerProfile(

		String clientId,					// Unique Client Code (UCC)
		String brokerName,					// "ZERODHA", "DHAN", "UPSTOX"
		String name,						// User's registered name
		String email,						// User's registered email
		String phone,						// User's registered phone number
		String userType,					// "individual", "retail"

		// Trading Capabilities
		List<String> enableExchanges,		// ["NSE", "BSE", "NFO", "MCX"]
		List<String> enableProducts,		// ["CNC", "MIS", "NRML"]
		List<String> enableOrderTypes,		//["MARKET", "LIMIT", "SL", "SL-M"]

		// Compliance and Authorization Flags
		boolean paoOrDdpiStatus,			// Power of Attorney / DDPI status (true if enabled)
		boolean isMtfEnabled,				// Margin Trading Facility consent status

		// API and Network Security (SEBI Algo Requirements)
		String primaryStaticIp,				// Registered primary static IP for order routing
		String secondaryStaticIp,			// Registered secondary/fallback static IP
		boolean isDataApiActive				// Status of live market data subscription

		) {}

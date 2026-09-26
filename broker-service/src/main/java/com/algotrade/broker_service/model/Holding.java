package com.algotrade.broker_service.model;

/** 
 * Broker-agnostic holding response.
 * Represents a long-term delivery holding in the Demat account.
 */

public record Holding(
		String symbol,				// e.g., "COMPANY Name"
		String exchange,			// "NSE", "BSE"
		String instrumentToken,		// Broker's internal ID for websocket subscriptions (e.g., "738561")
		String isin,				// International Securities Identification Number (e.g., "INE002A01018")

		// Quantity Breakdown
		int totalQuantity,			// Total aggregate quantity
		int dpQuantity,				// Fully settled quantity residing in the Demat account
		int t1Quantity,				// Unsettled quantity (bought recently, pending delivery)
		int collateralQuantity,		// Quantity pledged to the broker for F&O/Intraday margin
		int authorisedQuantity,		// Quantity authorised via CDSL eDIS for selling

		// Pricing & PnL
		double averagePrice,		// Average buying price of the holding
		double lastPrice,			// Last Traded Price (LTP)
		double closePrice,			// Previous day's closing price
		double profitAndLoss,		// Total Profit and Loss
		double dayChange,			// Absolute price change today
		double dayChangePercentage 	// Percentage price change today

		) {}

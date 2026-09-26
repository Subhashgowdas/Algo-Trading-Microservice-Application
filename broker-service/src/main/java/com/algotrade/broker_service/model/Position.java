package com.algotrade.broker_service.model;

/** 
 * Broker-agnostic position response. 
 * Represents a consolidated view of an open or closed position in the portfolio.
 */

public record Position(
		String symbol,						// e.g., "RELIANCE" or "NIFTY24MAY22000CE"
		String exchange,					// "NSE", "BSE", "NFO", "MCX"
		String instrumentToken,				// "NSE", "BSE", "NFO", "MCX"
		String product,						// "CNC" (Delivery), "MIS" (Intraday), "NRML" (Carry-forward)
		String positionType,				// "LONG", "SHORT", "CLOSED"

		// Quantity Tracking
		int netQuantity,					// Current open quantity (negative if short)
		int buyQuantity,					// Total quantity bought today + carried forward
		int sellQuantity,					// Total quantity sold today
		int carryForwardQuantity,			// Quantity carried over from previous trading sessions
		int dayBuyQuantity,					// Quantity bought specifically today
		int daySellQuantity,				// Quantity sold specifically today

		// Price Tracking
		double averagePrice,				// Net average price of the open position
		double buyAveragePrice,				// Average price for all buy legs
		double sellAveragePrice,			// Average price for all sell legs
		double lastPrice,					// Last Traded Price (LTP) to calculate live M2M
		double closePrice,					// Previous day's closing price

		// Profit and Loss (PnL)
		double profitAndLoss,				// Total net returns (Realized + Unrealized)
		double realizedProfitAndLoss,		// Profit/Loss booked from squared-off quantities
		double unrealizedProfitAndLoss,		// Standing Profit/Loss for the current open netQuantity

		// F&O Specifics
		int multiplier						// Lot size multiplier for derivative contracts (default 1 for equity)
		) {}

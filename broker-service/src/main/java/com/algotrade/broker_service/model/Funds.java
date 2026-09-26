package com.algotrade.broker_service.model;

/** 
 * Broker agnostic account funds and margin details. 
 * Handles cash, collateral, and granular margin utilization.
 */
public record Funds(
		
   // Core Cash Balances
	
	double availableMargin,				// Total usable balance for trading (Cash + Collateral)
	double availableCash,				// Raw cash available (excluding collateral)
	double withdrawableBalance,			// Cleared cash that can be withdrawn to the bank today
	double openingBalance,				// Start of day (SOD) limit/balance
	
    // Collateral and Additions
	double payinAmount,					// Cash transferred into the trading account today
	double collateralAmount,			// Margin received against pledged shares/ETFs/Mutual Funds
	double adhocMargin,					// Additional temporary margin provided by the broker
	double receivableAmount,			// Credit received from selling delivery holdings (usually 80% on T-day)

    // Margin Utilization (Debits)
	double totalUtilisedMargin,			// Total funds currently blocked/utilised across all segments
	double spanMargin,					// Exchange-mandated SPAN margin blocked for open F&O positions
	double exposureMargin,				// Broker-mandated exposure margin blocked for open F&O positions
	double deliveryMargin,				// Margin blocked when you sell securities from your Demat
	double optionPremium,				// Premium received from shorting options (can offset margins)

    // Mark to Market (M2M) Profit & Loss
	double realizedM2m,					// Booked intraday profit/loss for the day
	double unrealizedM2m				// Un-booked (open position) intraday profit/loss

	) {}

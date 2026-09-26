package com.algotrade.broker_service.brokerAdapter;

import java.util.List;

import com.algotrade.broker_service.dto.BrokerTokenResponse;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.model.BrokerCredentials;
import com.algotrade.broker_service.model.BrokerProfile;
import com.algotrade.broker_service.model.Funds;
import com.algotrade.broker_service.model.Holding;
import com.algotrade.broker_service.model.Instrument;
import com.algotrade.broker_service.model.OrderRequest;
import com.algotrade.broker_service.model.OrderResponse;
import com.algotrade.broker_service.model.Position;
import com.algotrade.broker_service.model.Quote;
import com.algotrade.broker_service.model.Trade;

/**
 * Enterprise grade interface that every broker integration must implement.
 * <p>
 * Each method corresponds to a trading operation.
 * The adapter is stateless – credentials are supplied per‑call and never stored.
 */

public interface BrokerAdapter {

	/**
	 * @return standardised broker name
	 */
	String getBrokerName();

	String getAuthorizationUrl(String apiKey, String redirectUri);

	BrokerTokenResponse exchangeRequestToken(String requestToken,
			String apiKey,
			String apiSecret,
			String redirectUri) throws BrokerException;

	// ---------- Core Order Operations ----------

	/**
	 * Place a new order.
	 */
	OrderResponse placeOrder(OrderRequest request, BrokerCredentials credentials);

	/**
	 * Cancel an existing order.
	 */
	OrderResponse cancelOrder(String orderId,  BrokerCredentials credentials);

	/**
	 * Modify an open order.
	 */
	default OrderResponse modifyOrder(String orderId, OrderRequest request,  BrokerCredentials credentials) {
		throw new UnsupportedOperationException(getBrokerName() + " does not support order modification");
	}

	// ---------- Advanced Order Operations (GTT / Forever) ----------

	/**
	 * Place a Good-Till-Triggered (GTT) or Forever order.
	 */
	default OrderResponse placeGTTOrder(OrderRequest request,  BrokerCredentials credentials) {
		throw new UnsupportedOperationException(getBrokerName() + "does not support GTT placement");
	}

	/**
	 * Cancel an active GTT order.
	 */
	default OrderResponse cancelGTTOrder(String gttId,  BrokerCredentials credentials) {
		throw new UnsupportedOperationException(getBrokerName() + "does not support GTT cancelletion");
	}

	// ---------- Position / Holding / Funds ----------

	/**
	 * Retrieve current day positions (open + closed).
	 */
	List<Position> getPositions( BrokerCredentials credentials);

	/**
	 * Convert an open position's product type (e.g., from Intraday/MIS to Delivery/CNC).
	 */
	default boolean convertPosition(String symbol,String exchange, String transactionType,String oldProduct,String newProduct, int quantity,BrokerCredentials credentials) {
		throw new UnsupportedOperationException(getBrokerName() + "does not support position conversion");
	}

	/**
	 * Retrieve long term delivery holdings.
	 */
	List<Holding> getHoldings( BrokerCredentials credentials);

	/**
	 * Retrieve account fund & margin details.
	 */
	Funds getFunds( BrokerCredentials credentials);

	// ---------- Trade & Order History ----------

	/**
	 * Retrieve all executed trades for the current day.
	 */
	List<Trade> getTrades( BrokerCredentials credentials);

	/**
	 * Retrieve the history/lifecycle of a specific order by its broker ID.
	 */
	default List<OrderResponse> getOrderHistory(String orderId,  BrokerCredentials credentials){
		throw new UnsupportedOperationException(getBrokerName() + "does not support individual order history retrieval");
	}


	// ---------- Market Data & Instruments ----------

	/**
	 * Get a snapshot quote for a single symbol.
	 */
	Quote getQuote(String symbol,  BrokerCredentials credentials);

	/**
	 * Get the list of all tradable instruments (master contract list).
	 */
	List<Instrument> getInstruments(BrokerCredentials credentials);

	/**
	 * Retrieve the broker account profile (client ID, enabled segments, IP whitelisting statuses).
	 */
	BrokerProfile getProfile(BrokerCredentials credentials);

	/**
	 * Get historical OHLC candle data for backtesting or indicator initialization.
	 */
	//	default List<Candel> getHistoricalData(String symbol, String exchange, String interval, long fromTimestamp, long toTimestamp,  BrokerCredentials credentials){
	//		throw new UnsupportedOperationException(getBrokerName() + "does not support historical data retrieval");
	//	}
	//

	/**
	 * Get real-time Option Chain data (Greeks, IV, OI) for a specific underlying asset.
	 */
	//	default OptionChain getOptionChain(String symbol, String expiry, BrokerCredentials credentials) {
	//		throw new UnsupportedOperationException(getBrokerName() + "does not support Option Chain retrieval");
	//	}

}

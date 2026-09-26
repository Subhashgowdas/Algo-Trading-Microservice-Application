package com.algotrade.broker_service.service;

import java.util.List;

import com.algotrade.broker_service.model.BrokerProfile;
import com.algotrade.broker_service.model.Funds;
import com.algotrade.broker_service.model.Holding;
import com.algotrade.broker_service.model.Instrument;
import com.algotrade.broker_service.model.OrderRequest;
import com.algotrade.broker_service.model.OrderResponse;
import com.algotrade.broker_service.model.Position;
import com.algotrade.broker_service.model.Quote;
import com.algotrade.broker_service.model.Trade;

public interface BrokerService {

	 OrderResponse placeOrder(String brokerName, OrderRequest request);

	 OrderResponse cancelOrder(String brokerName, String orderId);

	 List<Position> getPositions(String brokerName);

	 List<Holding> getHoldings(String brokerName);

	 Funds getFunds(String brokerName);

	 Quote getQuote(String brokerName, String symbol);

	 List<Trade> getTrades(String brokerName);

	 List<Instrument> getInstruments(String brokerName);

	 BrokerProfile getProfile(String brokerName);

}

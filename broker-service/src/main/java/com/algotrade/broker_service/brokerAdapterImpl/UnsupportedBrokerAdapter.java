package com.algotrade.broker_service.brokerAdapterImpl;

import java.util.List;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
import com.algotrade.broker_service.dto.BrokerTokenResponse;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.exception.ErrorCode;
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

import lombok.extern.slf4j.Slf4j;


@Slf4j
public class UnsupportedBrokerAdapter implements BrokerAdapter{

	@Override
	public String getBrokerName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getAuthorizationUrl(String apiKey, String redirectUri) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
    public BrokerTokenResponse exchangeRequestToken(String requestToken, String apiKey, String apiSecret, String redirectUrl) {
        throw new BrokerException(ErrorCode.BROKER_UNAVAILABLE, "Broker integration is currently under development.");
    }

	@Override
	public OrderResponse placeOrder(OrderRequest request, BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OrderResponse cancelOrder(String orderId, BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Position> getPositions(BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Holding> getHoldings(BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Funds getFunds(BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Trade> getTrades(BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Quote getQuote(String symbol, BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Instrument> getInstruments(BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BrokerProfile getProfile(BrokerCredentials credentials) {
		// TODO Auto-generated method stub
		return null;
	}
	
}

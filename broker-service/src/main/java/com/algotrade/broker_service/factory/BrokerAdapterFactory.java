package com.algotrade.broker_service.factory;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
import com.algotrade.broker_service.brokerAdapterImpl.UnsupportedBrokerAdapter;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.exception.ErrorCode;

@Service
public class BrokerAdapterFactory {

	Logger logger  = LoggerFactory.getLogger(BrokerAdapterFactory.class);

	private final Map<String, BrokerAdapter> adapterRegistry;

	public BrokerAdapterFactory(List<BrokerAdapter> adapters) {
		this.adapterRegistry = adapters.stream()
				.collect(Collectors.toUnmodifiableMap(
						adapter -> adapter.getBrokerName(), 
						Function.identity()));
	}

	public BrokerAdapter getAdapter(String brokerName) {
		if(brokerName == null || brokerName.isBlank())
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Broker name must not be null or blank");

		BrokerAdapter adapter = adapterRegistry.get(brokerName.toUpperCase());

		if(adapter == null) {
			logger.warn("No adapter implemented for broker: {}. Utilizing UnsupportedBrokerAdapter fallback.", brokerName);
			return new UnsupportedBrokerAdapter();
		}

		return adapter;
	}

}

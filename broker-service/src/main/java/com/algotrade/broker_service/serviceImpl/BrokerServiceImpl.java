package com.algotrade.broker_service.serviceImpl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.exception.ErrorCode;
import com.algotrade.broker_service.factory.BrokerAdapterFactory;
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
import com.algotrade.broker_service.service.BrokerService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

/**
 * Enterprise grade orchestrator for all broker operations.
 * <p>
 * This service is the single entry point for trading commands and queries.
 * It handles:
 * <ul>
 *   <li>Extracting the authenticated user's email from the security context.</li>
 *   <li>Fetching decrypted broker credentials (via {@link CredentialServiceImpl}).</li>
 *   <li>Selecting the appropriate {@link BrokerAdapter} (via {@link BrokerAdapterFactory}).</li>
 *   <li>Delegating the operation to the adapter.</li>
 * </ul>
 */
@Service
public class BrokerServiceImpl implements BrokerService{

	private static final Logger log = LoggerFactory.getLogger(BrokerServiceImpl.class);
	private static final String BROKER_API_CONFIG = "brokerApi";

	private final CredentialServiceImpl credentialService;
	private final BrokerAdapterFactory adapterFactory;

	public BrokerServiceImpl(CredentialServiceImpl credentialService, BrokerAdapterFactory adapterFactory) {
		this.credentialService = credentialService;
		this.adapterFactory = adapterFactory;
	}

	// ──────────────────────────────────────────────
	// Mutating Operations (CircuitBreaker ONLY - No Retry)
	// ──────────────────────────────────────────────
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	public OrderResponse placeOrder(String brokerName, OrderRequest request) {
		String email = getCurrentUserEmail();
		log.info("Placing order for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.placeOrder(request, credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	public OrderResponse cancelOrder(String brokerName, String orderId) {
		String email = getCurrentUserEmail();
		log.info("Cancelling order {} for user {} via {}", orderId, email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.cancelOrder(orderId, credentials);
	}

	// ──────────────────────────────────────────────
	// Read Operations (Safe for CircuitBreaker & Retry)
	// ──────────────────────────────────────────────
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public List<Position> getPositions(String brokerName) {
		String email = getCurrentUserEmail();
		log.debug("Fetching positions for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getPositions(credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public List<Holding> getHoldings(String brokerName) {
		String email = getCurrentUserEmail();
		log.debug("Fetching holdings for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getHoldings(credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public Funds getFunds(String brokerName) {
		String email = getCurrentUserEmail();
		log.debug("Fetching funds for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getFunds(credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public Quote getQuote(String brokerName, String symbol) {
		String email = getCurrentUserEmail();
		log.debug("Fetching quote for {} via {}", symbol, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getQuote(symbol, credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public List<Trade> getTrades(String brokerName) {
		String email = getCurrentUserEmail();
		log.debug("Fetching trades for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getTrades(credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public List<Instrument> getInstruments(String brokerName) {
		String email = getCurrentUserEmail();
		log.debug("Fetching instruments for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getInstruments(credentials);
	}
	
	@Override
	@CircuitBreaker(name = BROKER_API_CONFIG)
	@Retry(name = BROKER_API_CONFIG)
	public BrokerProfile getProfile(String brokerName) {
		String email = getCurrentUserEmail();
		log.debug("Fetching profile for user {} via {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		BrokerAdapter adapter = adapterFactory.getAdapter(brokerName);
		return adapter.getProfile(credentials);
	}

	// ──────────────────────────────────────────────
	// Private helpers
	// ──────────────────────────────────────────────

	/**
	 * Extracts the authenticated user's email from the current security context.
	 *
	 * @return the user's email (JWT subject)
	 * @throws IllegalStateException if no authentication is present
	 */
	private String getCurrentUserEmail() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null ||!authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
			throw new BrokerException(ErrorCode.AUTHENTICATION_FAILED, "No authenticated user found");
		}
		return authentication.getName();
	}
}
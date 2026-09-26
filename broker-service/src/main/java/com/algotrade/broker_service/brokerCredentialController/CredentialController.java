package com.algotrade.broker_service.brokerCredentialController;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
import com.algotrade.broker_service.dto.BrokerCredentialRequest;
import com.algotrade.broker_service.dto.BrokerCredentialResponse;
import com.algotrade.broker_service.dto.BrokerTokenResponse;
import com.algotrade.broker_service.dto.OAuthExchangeRequest;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.exception.ErrorCode;
import com.algotrade.broker_service.factory.BrokerAdapterFactory;
import com.algotrade.broker_service.model.BrokerCredentials;
import com.algotrade.broker_service.serviceImpl.CredentialServiceImpl;

import jakarta.validation.Valid;

/**
 * Public REST controller for broker credential operations.
 *
 * Both endpoints require a valid user JWT (enforced by Spring Security).
 * The authenticated user’s identity is extracted from the security context
 * and used internally – the frontend never sends a user ID.
 */
@RestController
@RequestMapping("/api/broker/credentials")
public class CredentialController {

	private static final Logger log = LoggerFactory.getLogger(CredentialController.class);
	private final CredentialServiceImpl credentialService;
	private final BrokerAdapterFactory brokerAdapterFactory;

	public CredentialController(CredentialServiceImpl credentialService,BrokerAdapterFactory brokerAdapterFactory) {
		this.credentialService = credentialService;
		this.brokerAdapterFactory = brokerAdapterFactory;
	}

	/**
	 * GET /api/broker/credentials/{brokerName}
	 *
	 * Checks whether broker credentials exist for the authenticated user.
	 *
	 * 200 OK – credentials exist; returns decrypted config.
	 * 404 Not Found – no credentials exist for this broker.
	 *
	 * @param brokerName e.g. "ZERODHA"
	 */
	@GetMapping("/{brokerName}")
	public ResponseEntity<BrokerCredentialResponse> getCredentials(@PathVariable String brokerName) {
		String email = getCurrentUserEmail();

		log.info("Fetching credentials for broker: {} for user: {}", brokerName, email);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		if (credentials == null) {
			log.warn("Credentials not found for broker {} : {}", brokerName, email);
			return ResponseEntity.notFound().build();
		}

		BrokerCredentialResponse response = credentialService.createCredentialResponse(credentials);
		return ResponseEntity.ok(response);
	}

	/**
	 * POST /api/broker/credentials
	 *
	 * Stores new (or updates existing) broker credentials for the authenticated user.
	 *
	 * 201 Created – credentials were stored successfully.
	 *
	 * @param request contains brokerName, apiKey, apiSecret, redirectUrl
	 */
	@PostMapping("/saveCredentials")
	public ResponseEntity<Map<String, String>> saveCredentials(
			@Valid @RequestBody BrokerCredentialRequest request) {

		String email = getCurrentUserEmail();

		log.info("Saving credentials for broker: {}", request.brokerName());

		credentialService.saveCredentials(
				email,
				request.brokerName(),
				request.apiKey(),
				request.apiSecret());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(Map.of("message", "Broker credentials saved successfully"));
	}

	@GetMapping("/redirectUrl/{brokerName}")
	public ResponseEntity<Map<String, String>> getBrokerOuthRedirectUrl(@PathVariable String brokerName) {

		String redirectUrl = credentialService.prepareOuthRedirectUrl(brokerName);

		if(redirectUrl != null) {
			return ResponseEntity.ok(Map.of("redirectUrl", redirectUrl));
		}

		return ResponseEntity.status(HttpStatus.NOT_FOUND).build(); 
	}

	/**
	 * POST /api/broker/credentials/oauth/exchange
	 *
	 * Exchanges a request_token from OAuth popup for an access_token,
	 * persists it to auth-service, and clears active caches.
	 */
	@PostMapping("/oauth/exchange/callback")
	public ResponseEntity<Map<String, String>> generatExchangeAccessToken(@Valid @RequestBody OAuthExchangeRequest request) {
		String email = getCurrentUserEmail();
		String brokerName = request.brokerName().trim().toUpperCase();

		log.info("Exchanging OAuth request token for user: {}, broker: {}", email, brokerName);

		BrokerCredentials credentials = credentialService.getCredentials(email, brokerName);
		if (credentials == null || credentials.apiKey() == null || credentials.apiSecret() == null) {
			throw new BrokerException(ErrorCode.NOT_FOUND, "Broker credentials not configured for " + brokerName);
		}

		BrokerAdapter adapter = brokerAdapterFactory.getAdapter(brokerName);

		BrokerTokenResponse accessToken = adapter.exchangeRequestToken(
				request.requestToken(),
				credentials.apiKey(),
				credentials.apiSecret(),
				credentials.redirectUrl()
				);

		credentialService.updateAccessToken(email, brokerName, accessToken);

		return ResponseEntity.ok(Map.of("message", "Access token generated and stored successfully"));
	}

	private String getCurrentUserEmail() {
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if(authentication == null || !authentication.isAuthenticated())
			throw new IllegalStateException("No authenticated user found in SecurityContext");
		return (String) authentication.getPrincipal();
	}
}
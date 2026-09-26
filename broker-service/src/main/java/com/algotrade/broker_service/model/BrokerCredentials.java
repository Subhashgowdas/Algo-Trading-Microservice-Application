package com.algotrade.broker_service.model;

import java.util.Collections;
import java.util.Map;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;

/**
 * Immutable holder for decrypted broker credentials.
 * <p>
 * This object is created by the {@code CredentialService} after decryption
 * and is passed to every {@link BrokerAdapter} method.
 * It is intentionally a record to guarantee immutability and thread‑safety.
 */

public record BrokerCredentials(

		/** Standardised broker name */
		String brokerName,

		/** User's unique client code / UCC */
		String userId,

		/** Decrypted API key */
		String apiKey,

		/** Decrypted API secret */
		String apiSecret,

		/** Optional TOTP Secret or PIN for headless/automated login flows */
		String authSecret,

		/** OAuth redirect URL */
		String redirectUrl,
		
		/** OAuth access token (may be null for brokers that do not require it) */
		String accessToken,
		
		/** OAuth accessToken Expiry **/
		Long accessTokenExpiry,
		
		/** Broker specific extra fields, never null, unmodifiable */
		Map<String,String> extras

		) {

	/**
	 * Canonical constructor ensures the extras map is immutable.
	 */

	public BrokerCredentials {
        extras = (extras == null) ? Collections.emptyMap() : Map.copyOf(extras);
    }

	/**
	 * Convenience factory for the common case where core fields are needed.
	 */
	public static BrokerCredentials of(String brokerName, String userId, String apiKey,String apiSecret) {
		return new BrokerCredentials(brokerName, userId, apiKey, apiSecret, null, null, null, null,null);
	}

}

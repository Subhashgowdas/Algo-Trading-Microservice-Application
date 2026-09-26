package com.algotrade.broker_service.serviceImpl;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
import com.algotrade.broker_service.dto.BrokerCredentialResponse;
import com.algotrade.broker_service.dto.BrokerTokenResponse;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.exception.ErrorCode;
import com.algotrade.broker_service.factory.BrokerAdapterFactory;
import com.algotrade.broker_service.model.BrokerCredentials;
import com.algotrade.broker_service.security.util.AesGcmEncryptionUtil;
import com.algotrade.broker_service.service.CredentialService;
import com.algotrade.common.cache.CacheService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Enterprise Production-Grade Credential Service.
 * 
 * Architecture:
 * - Local Memory Cache: Caffeine Cache with automated TinyLFU eviction & 5-minute TTL.
 * - Remote Distributed Cache: Redis Cache (via CacheService) - 15-minute cross-instance persistence.
 * - Source of Truth: Auth-Service internal REST API.
 */
@Service
public class CredentialServiceImpl implements CredentialService{

	private static final Logger log = LoggerFactory.getLogger(CredentialServiceImpl.class);

	private static final String CACHE_KEY_PREFIX = "brokerCreds:";
	private static final long LOCAL_CACHE_TTL_MINUTES = 15;
//	private static final long REDIS_CACHE_TTL_SECONDS = 60 * 60;
	private static final int LOCAL_CACHE_MAX_CAPACITY = 500;
	private static final long CLOCK_SKEW_BUFFER_MS = 30000L;

	private final RestTemplate restTemplate;
	private final String authServiceBaseUrl;
	private final String serviceToken;
	private final String redirectBaseUrl;
	private final byte[] encryptionKey;
	private final ObjectMapper objectMapper;
	private final CacheService redisCacheService;
	private final BrokerAdapterFactory brokerAdapterFactory;

	// High-performance, thread-safe Caffeine L1 in-memory cache
	private final Cache<String, BrokerCredentials> inMemoryCache;

	public CredentialServiceImpl(
			@Value("${app.internal.auth-service-base-url}") String authServiceBaseUrl,
			@Value("${app.internal.service-token}") String serviceToken,
			@Value("${app.encryption.secret-key}") String encryptionKeyBase64,
			@Value("${app.brokerOauth.redirect-base-url}") String redirectBaseUrl,
			BrokerAdapterFactory brokerAdapterFactory,
			RestTemplateBuilder restTemplateBuilder,
			ObjectMapper objectMapper,
			CacheService redisCacheService) {

		this.authServiceBaseUrl = Objects.requireNonNull(authServiceBaseUrl, "authServiceBaseUrl must not be null");
		this.serviceToken = Objects.requireNonNull(serviceToken, "serviceToken must not be null");
		this.encryptionKey = Base64.getDecoder().decode(Objects.requireNonNull(encryptionKeyBase64, "encryptionKey must not be null"));
		this.redirectBaseUrl = redirectBaseUrl;
		this.objectMapper = objectMapper;
		this.redisCacheService = redisCacheService;
		this.brokerAdapterFactory = brokerAdapterFactory;

		// Configure resilient REST client with strict timeouts to prevent thread starvation
		this.restTemplate = restTemplateBuilder
				.connectTimeout(Duration.ofSeconds(3))
				.readTimeout(Duration.ofSeconds(5))
				.errorHandler(new DefaultResponseErrorHandler() {
					@Override
					public void handleError(ClientHttpResponse response) throws IOException {
						// Ignore 404s so we don't generate expensive stack traces
						if (response.getStatusCode().value() == 404) {
							return;
						}
						// Throw exceptions for 500s, 401s, etc.
						super.handleError(response);
					}
				})
				.build();

		// Initialize Caffeine Cache with size and time-based eviction policies
		this.inMemoryCache = Caffeine.newBuilder()
				.expireAfterWrite(LOCAL_CACHE_TTL_MINUTES, TimeUnit.MINUTES)
				.maximumSize(LOCAL_CACHE_MAX_CAPACITY)
				.build();
	}

	// ---------------------------------------------------------------
	// Public API
	// ---------------------------------------------------------------

	/**
	 * Fetch decrypted broker credentials for the specified user and broker.
	 * Execution flow: Local In-Memory Cache -> Remote Redis Cache -> Auth-Service API Fallback.
	 */
	@Override
	public BrokerCredentials getCredentials(String email, String brokerName) {

		//		String cacheKey = buildCacheKey(email, brokerName);
		//
		//		// 1. Local Memory Cache Read
		//		BrokerCredentials credentials = inMemoryCache.getIfPresent(cacheKey);
		//
		//		if (credentials == null) {
		//			log.info("Local cache miss for key: {}. Checking Redis...", cacheKey);
		//
		//			// 2. Remote Distributed Cache Read (Redis path)
		//			try {
		//				String encryptedJsonPayload = (String) redisCacheService.get(cacheKey).orElse(null);
		//				if (encryptedJsonPayload != null) {
		//					credentials = deserializeAndDecryptCredentials(encryptedJsonPayload);
		//					log.info("Credentials cache hit (Remote Redis) for key: {}", cacheKey);
		//				}
		//			} catch (Exception e) {
		//				log.error("Redis access failed for cache key {}. Continuing to auth-service fallback.", cacheKey, e);
		//			}
		//
		//			// 3. Downstream Auth-Service Fetch (Source of Truth)
		//			if (credentials == null) {
		//				log.info("Redis cache miss/failure for key: {}. Fetching from Auth Service...", cacheKey);
		//				credentials = fetchCredentialsFromAuthService(email, brokerName);
		//
		//				// Populate Redis only if we just fetched from Auth Service
		//				if (credentials != null) {
		//					try {
		//						String encryptedJsonPayload = serializeAndEncryptCredentials(credentials);
		//						redisCacheService.set(cacheKey, encryptedJsonPayload, REDIS_CACHE_TTL_SECONDS, TimeUnit.SECONDS);
		//					} catch (Exception e) {
		//						log.error("Failed to write credentials to Remote Redis cache for key: {}", cacheKey, e);
		//					}
		//				}
		//			}
		//
		//			// 4. Populate Local Cache (Applies if fetched from Redis OR Auth Service)
		//			if (credentials != null) {
		//				inMemoryCache.put(cacheKey, credentials);
		//				log.info("Populated local cache for key: {}", cacheKey);
		//			}
		//
		//		} else {
		//			log.info("Credentials cache hit (Local Memory) for key: {}", cacheKey);
		//		}

		BrokerCredentials credentials = fetchCredentialsFromAuthService(email, brokerName);

		return credentials;
	}

	/**
	 * Constructs the BrokerCredentialResponse by fetching the corresponding broker adapter
	 * to generate the authorization URL and mask the API secret.
	 */
	public BrokerCredentialResponse createCredentialResponse(BrokerCredentials credentials) {
		
		long currentTime = System.currentTimeMillis();

		if (credentials == null) {
			return null;
		}

		BrokerAdapter adapter = brokerAdapterFactory.getAdapter(credentials.brokerName());

		String authUrl = adapter.getAuthorizationUrl(credentials.apiKey(), credentials.redirectUrl());

		boolean hasToken = credentials.accessToken() != null && !credentials.accessToken().isBlank()
		        && credentials.accessTokenExpiry() != null && credentials.accessTokenExpiry() > (currentTime + CLOCK_SKEW_BUFFER_MS);
		
		return new BrokerCredentialResponse(hasToken, authUrl);
	}

	/**
	 * Persist broker credentials via auth-service and immediately invalidate both cache layers.
	 */
	@Override
	public void saveCredentials(String email, String brokerName, String apiKey, String apiSecret) {
		String normalizedEmail = normalizeEmail(email);
		String normalizedBroker = normalizeBrokerName(brokerName);

		Map<String, String> requestBody = Map.of(
				"apiKey", encryptFieldValue(apiKey),
				"apiSecret", encryptFieldValue(apiSecret),
				"redirectUrl", encryptFieldValue(prepareOuthRedirectUrl(brokerName) != null ? prepareOuthRedirectUrl(brokerName) : "")
				);

		String endpointUrl = authServiceBaseUrl + "/api/internal/broker-credentials/" + normalizedBroker;
		executeAuthServiceRequest(endpointUrl, HttpMethod.POST, requestBody, normalizedEmail, Object.class);

		evictCredentialsCache(normalizedEmail, normalizedBroker);
		log.info("Successfully updated credentials and evicted cache for user: {}, broker: {}", normalizedEmail, normalizedBroker);
	}


	/**
	 * Safely updates the daily access token while preserving existing API Key, Secret, and Redirect URL.
	 */
	@Override
	public void updateAccessToken(String email, String brokerName, BrokerTokenResponse accessToken) {
		String normalizedEmail = normalizeEmail(email);
		String normalizedBroker = normalizeBrokerName(brokerName);

		// 1. Retrieve existing credentials to preserve apiKey and apiSecret
		BrokerCredentials existingCredentials = getCredentials(normalizedEmail, normalizedBroker);
		if (existingCredentials == null) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Cannot update access token: No existing credentials found for " + normalizedBroker);
		}

		// 2. Build encrypted payload including the new accessToken
		Map<String, String> requestBody = Map.of(
				"apiKey", encryptFieldValue(existingCredentials.apiKey()),
				"apiSecret", encryptFieldValue(existingCredentials.apiSecret()),
				"redirectUrl", encryptFieldValue(existingCredentials.redirectUrl() != null ? existingCredentials.redirectUrl() : ""),
				"accessToken", encryptFieldValue(accessToken.accessToken()),
				"accessTokenExpiry", encryptFieldValue(String.valueOf(accessToken.expiryEpochMillis()))
				);

		// 3. Persist updated credentials to Auth-Service
		String endpointUrl = authServiceBaseUrl + "/api/internal/broker-credentials/" + normalizedBroker;
		executeAuthServiceRequest(endpointUrl, HttpMethod.POST, requestBody, normalizedEmail, Object.class);

		// 4. Invalidate both cache tiers so subsequent requests pull the new access token
		evictCredentialsCache(normalizedEmail, normalizedBroker);
		log.info("Successfully updated access token and evicted cache for user: {}, broker: {}", normalizedEmail, normalizedBroker);
	}

	/**
	 * Evict credentials from both local memory and distributed Redis caches.
	 */
	@Override
	public void evictCredentialsCache(String email, String brokerName) {
		String cacheKey = buildCacheKey(email, brokerName);
		inMemoryCache.invalidate(cacheKey);
		try {
			redisCacheService.delete(cacheKey);
		} catch (Exception e) {
			log.warn("Failed to invalidate Remote Redis cache key: {}", cacheKey, e);
		}
	}

	@Override
	public String prepareOuthRedirectUrl(String brokerName) {
		return  redirectBaseUrl + "/" + brokerName.toUpperCase() + "/callback";
	}

	// ---------------------------------------------------------------
	// Private Helpers
	// ---------------------------------------------------------------

	private String buildCacheKey(String email, String brokerName) {
		return CACHE_KEY_PREFIX + normalizeEmail(email) + ":" + normalizeBrokerName(brokerName);
	}

	private String normalizeEmail(String email) {
		if (email == null || email.isBlank()) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "User email cannot be null or empty");
		}
		return email.trim().toLowerCase();
	}

	private String normalizeBrokerName(String brokerName) {
		if (brokerName == null || brokerName.isBlank()) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Broker name cannot be null or empty");
		}
		return brokerName.trim().toUpperCase();
	}

	private BrokerCredentials fetchCredentialsFromAuthService(String UserEmail, String brokerName) {
		String broker = normalizeBrokerName(brokerName);
		String userEmail = normalizeEmail(UserEmail);

		String endpointUrl = authServiceBaseUrl + "/api/internal/broker-credentials/" + broker;

		Map<String, Object> rawResponseBody = executeAuthServiceRequest(endpointUrl, HttpMethod.GET, null, userEmail, Map.class);

		if ((rawResponseBody == null || rawResponseBody.isEmpty()) || (rawResponseBody.get(broker) == null)) {
			return null;
		}

		Map<String, String> encryptedDataMap = objectMapper.convertValue(rawResponseBody.get(broker), new TypeReference<Map<String, String>>() {});

		if (encryptedDataMap == null || !encryptedDataMap.containsKey("apiKey")) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Invalid or missing API key payload from auth-service");
		}

		String accessToken = null;
		if (encryptedDataMap.containsKey("accessToken") && encryptedDataMap.get("accessToken") != null) {
			accessToken = decryptFieldValue(String.valueOf(encryptedDataMap.get("accessToken")));
		}


		Long accessTokenExpiry = null;
		if (encryptedDataMap.containsKey("accessTokenExpiry") && encryptedDataMap.get("accessTokenExpiry") != null) {
			accessTokenExpiry = Long.parseLong(decryptFieldValue(String.valueOf(encryptedDataMap.get("accessTokenExpiry"))));
		}

		return new BrokerCredentials(
				broker, 
				userEmail,
				decryptFieldValue(encryptedDataMap.get("apiKey")),
				decryptFieldValue(encryptedDataMap.get("apiSecret")),
				null,
				decryptFieldValue(encryptedDataMap.getOrDefault("redirectUrl", "")), 
				accessToken, 
				accessTokenExpiry, 
				null);

	}

	private <T> T executeAuthServiceRequest(String endpointUrl, HttpMethod httpMethod, Object requestBody, String userEmail, Class<T> responseType) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(serviceToken);
		headers.set("X-User-Email", userEmail);

		HttpEntity<Object> httpEntity = new HttpEntity<>(requestBody, headers);

		try {
			ResponseEntity<T> response = restTemplate.exchange(endpointUrl, httpMethod, httpEntity, responseType);

			if (response.getStatusCode().value() == 404) {
				log.info("Broker Credentials Not found for {}", endpointUrl);
				return null; 
			}

			return response.getBody();
		} catch (HttpStatusCodeException e) {
			log.error("Auth-service call failed with status: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
			throw new BrokerException(ErrorCode.BROKER_UNAVAILABLE, "Auth-service returned error code: " + e.getStatusCode());
		} catch (RestClientException e) {
			log.error("Network error during auth-service communication at endpoint: {}", endpointUrl, e);
			throw new BrokerException(ErrorCode.BROKER_UNAVAILABLE, "Auth-service is unreachable");
		}
	}

	private String encryptFieldValue(String plaintext) {
		try {
			return AesGcmEncryptionUtil.encrypt(plaintext, encryptionKey);
		} catch (Exception e) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Credential field encryption failed");
		}
	}

	private String decryptFieldValue(String encrypted) {
		if (encrypted == null || encrypted.isBlank()) {
			return "";
		}
		try {
			return AesGcmEncryptionUtil.decrypt(encrypted, encryptionKey);
		} catch (Exception e) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Credential field decryption failed");
		}
	}

	private String serializeAndEncryptCredentials(BrokerCredentials credentials) {
		try {
			String jsonPayload = objectMapper.writeValueAsString(credentials);
			return AesGcmEncryptionUtil.encrypt(jsonPayload, encryptionKey);
		} catch (Exception e) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Serialization for L2 caching failed");
		}
	}

	private BrokerCredentials deserializeAndDecryptCredentials(String encryptedJsonPayload) {
		try {
			String decryptedJsonPayload = AesGcmEncryptionUtil.decrypt(encryptedJsonPayload, encryptionKey);
			return objectMapper.readValue(decryptedJsonPayload, BrokerCredentials.class);
		} catch (Exception e) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Deserialization from L2 cache failed");
		}
	}
}
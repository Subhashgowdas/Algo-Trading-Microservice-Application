package com.algotrade.broker_service.brokerAdapterImpl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
import com.algotrade.broker_service.dto.BrokerTokenResponse;
import com.algotrade.broker_service.exception.BrokerAuthenticationException;
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
import com.algotrade.broker_service.security.util.AesGcmEncryptionUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class ZerodhaAdapter implements BrokerAdapter {

	private static final Logger log = LoggerFactory.getLogger(ZerodhaAdapter.class);

	private final String brokerName;
	private final String baseUrl;
	private final String authUrl;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	// Injecting dynamic properties from application.yml
	public ZerodhaAdapter(
			@Value("${broker.zerodha.name:ZERODHA}") String brokerName,
			@Value("${broker.zerodha.base-url:https://api.kite.trade}") String baseUrl,
			@Value("${broker.zerodha.auth-url:https://kite.zerodha.com}") String authUrl,
			RestClient restClient, 
			ObjectMapper objectMapper) {
		this.brokerName = brokerName;
		this.baseUrl = baseUrl;
		this.restClient = restClient;
		this.objectMapper = objectMapper;
		this.authUrl = authUrl;
	}

	@Override
	public String getBrokerName() {
		return brokerName;
	}

	@Override
	public String getAuthorizationUrl(String apiKey, String redirectUri) {
		return UriComponentsBuilder.fromUriString(authUrl)
				.path("/connect/login")
				.queryParam("v", "3")
				.queryParam("api_key", apiKey)
				.queryParam("redirect_uri", redirectUri)
				.build()
				.toUriString();
	}

	@Override
	public BrokerTokenResponse exchangeRequestToken(String requestToken, String apiKey, String apiSecret, String redirectUri) throws BrokerException {
		// 1. Calculate the SHA-256 checksum using pure Java 21 
		String checksumData = apiKey + requestToken + apiSecret;
		String checksum = AesGcmEncryptionUtil.sha256Hex(checksumData); 

		// 2. Prepare the endpoint and Form-Data payload
		String endpoint = baseUrl + "/session/token";
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("api_key", apiKey);
		body.add("request_token", requestToken);
		body.add("checksum", checksum);

		try {
			// 3. Execute POST request using your existing RestClient configuration
			String responseBody = restClient.post()
					.uri(endpoint)
					.header("X-Kite-Version", "3")
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(body)
					.retrieve()
					.onStatus(status -> status.isError(), (req, res) -> {
						String errorContent = new String(res.getBody().readAllBytes());
						handleError(errorContent, res.getStatusCode().value()); // Reuses your existing error handler
						//						throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Broker API returned HTTP " + res.getStatusCode().value());
					})
					.body(String.class);

			// 4. Null-guarding the response to prevent crashing
			if (responseBody == null || responseBody.isBlank()) {
				throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: Response body is completely empty");
			}

			// 5. Parse JSON and extract the access token
			JsonNode response = objectMapper.readTree(responseBody);
			if (response.has("data") && response.path("data").has("access_token")) {
				String accessToken =  response.path("data").path("access_token").asText();

				return new BrokerTokenResponse(accessToken, getZerodhaExpiry());

			} else {
				throw new BrokerException(ErrorCode.SESSION_EXPIRED, "Invalid token payload returned from Zerodha");
			}

		} catch (Exception e) {
			// Unwrap custom exceptions to prevent masking
			if (e instanceof BrokerException) {
				throw (BrokerException) e;
			}
			if (e.getCause() instanceof BrokerException) {
				throw (BrokerException) e.getCause();
			}
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Token exchange failed: " + e.getMessage());
		}
	}

	private long getZerodhaExpiry() {
		ZoneId istZone = ZoneId.of("Asia/Kolkata");
		ZonedDateTime now = ZonedDateTime.now(istZone);
		
		// Set the time to 6:00 AM today
		ZonedDateTime expiry = now.withHour(6).withMinute(0).withSecond(0).withNano(0);
		
		// If it is already past 6:00 AM, push the expiry to 6:00 AM tomorrow
		if (!now.isBefore(expiry)) {
			expiry = expiry.plusDays(1);
		}
		
		return expiry.toInstant().toEpochMilli();
	}

	private String getAuthHeader(BrokerCredentials credentials) {
		return "token " + credentials.apiKey() + ":" + credentials.accessToken();
	}

	// --- Order Operations ---

	@Override
	public OrderResponse placeOrder(OrderRequest request, BrokerCredentials credentials) {
	    // 1. Resolve variety and endpoint
	    String variety = request.variety() != null && !request.variety().isBlank() 
	                     ? request.variety().toLowerCase() 
	                     : "regular";
	    String endpoint = baseUrl + "/orders/" + variety;

	    MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
	    
	    // 2. Mandatory Fields
	    body.add("exchange", request.exchange());
	    body.add("tradingsymbol", request.symbol()); // Note: API expects 'tradingsymbol'
	    body.add("transaction_type", request.transactionType());
	    body.add("order_type", request.orderType());
	    body.add("quantity", String.valueOf(request.quantity()));
	    body.add("product", request.product());
	    body.add("validity", request.validity());

	    // 3. Price Fields (Only send if greater than 0. Sending 0 for a MARKET order causes errors)
	    if (request.price() > 0) {
	        body.add("price", String.valueOf(request.price()));
	    }
	    if (request.triggerPrice() > 0) {
	        body.add("trigger_price", String.valueOf(request.triggerPrice()));
	    }

	    // 4. Advanced Standard Fields (Only if valid)
	    if (request.discloseQuantity() > 0) {
	        body.add("disclosed_quantity", String.valueOf(request.discloseQuantity()));
	    }
	    
	    if (request.marketProtection() > 0) {
	        body.add("market_protection", String.valueOf(request.marketProtection()));
	    }

	    if ("TTL".equalsIgnoreCase(request.validity()) && request.validityTtl() > 0) {
	        body.add("validity_ttl", String.valueOf(request.validityTtl()));
	    }

	    if (request.tag() != null && !request.tag().isBlank()) {
	        body.add("tag", request.tag());
	    }

	    // 5. Variety-Specific Fields (Iceberg)
	    if ("iceberg".equals(variety)) {
	        if (request.icebergLegs() >= 2) { // Kite requires at least 2 legs for iceberg
	            body.add("iceberg_legs", String.valueOf(request.icebergLegs()));
	        }
	        if (request.icebergQuantity() > 0) {
	            body.add("iceberg_quantity", String.valueOf(request.icebergQuantity()));
	        }
	    }

	    // Note: targetPrice, stopLossPrice, and trailingStoploss are typically handled 
	    // externally by your algo, or via GTTs (Good Till Triggered) in Kite. 
	    // They are not passed in the standard /orders/{variety} POST request.

	    // 6. Execute Request
	    JsonNode response = executePostOrPut(endpoint, body, credentials, true);
	    String orderId = response.path("data").path("order_id").asText();

	    return new OrderResponse(
	        orderId, 
	        null, null, null, 
	        "OPEN", 
	        request.quantity(), 
	        0, 
	        0.0, 
	        null, 
	        "Order Placed", 
	        System.currentTimeMillis(), 
	        0L
	    );
	}

	@Override
	public OrderResponse modifyOrder(String orderId, OrderRequest request, BrokerCredentials credentials) {
		String variety = request.variety()!= null? request.variety() : "regular";
		String endpoint = baseUrl + "/orders/" + variety + "/" + orderId;

		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("order_type", request.orderType());
		body.add("quantity", String.valueOf(request.quantity()));
		if (request.price() > 0) body.add("price", String.valueOf(request.price()));
		if (request.triggerPrice() > 0) body.add("trigger_price", String.valueOf(request.triggerPrice()));

		executePostOrPut(endpoint, body, credentials, false); // PUT request

		return new OrderResponse(orderId, null, null, null, "OPEN", 0, 0, 0.0, null, "Order Modified", System.currentTimeMillis(), 0L);
	}

	@Override
	public OrderResponse cancelOrder(String orderId, BrokerCredentials credentials) {
		String endpoint = baseUrl + "/orders/regular/" + orderId;
		executeDelete(endpoint, credentials);
		return new OrderResponse(orderId, null, null, null, "CANCELLED", 0, 0, 0.0, null, "Order Cancelled", System.currentTimeMillis(), 0L);
	}

	@Override
	public boolean convertPosition(String symbol, String exchange, String transactionType, String oldProduct, String newProduct, int quantity, BrokerCredentials credentials) {
		String endpoint = baseUrl + "/portfolio/positions";
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("tradingsymbol", symbol);
		body.add("exchange", exchange);
		body.add("transaction_type", transactionType);
		body.add("position_type", "day");
		body.add("quantity", String.valueOf(quantity));
		body.add("old_product", oldProduct);
		body.add("new_product", newProduct);

		executePostOrPut(endpoint, body, credentials, false); // false for PUT request
		return true;
	}

	// --- Portfolio & Funds ---

	@Override
	public List<Position> getPositions(BrokerCredentials credentials) {
		String endpoint = baseUrl + "/portfolio/positions";
		JsonNode response = executeGet(endpoint, credentials);
		List<Position> positions = new ArrayList<>();

		// Zerodha separates positions into 'net' and 'day' [cite: 1]
		JsonNode netPositions = response.path("data").path("net");
		for (JsonNode node : netPositions) {
			int netQuantity = node.path("quantity").asInt();
			String positionType = netQuantity > 0 ? "LONG" : (netQuantity < 0 ? "SHORT" : "CLOSED");

			positions.add(new Position(
					node.path("tradingsymbol").asText(),
					node.path("exchange").asText(),
					node.path("instrument_token").asText(),
					node.path("product").asText(),
					positionType,
					netQuantity,
					node.path("buy_quantity").asInt(),
					node.path("sell_quantity").asInt(),
					node.path("overnight_quantity").asInt(),
					node.path("day_buy_quantity").asInt(),
					node.path("day_sell_quantity").asInt(),
					node.path("average_price").asDouble(),
					node.path("buy_price").asDouble(),
					node.path("sell_price").asDouble(),
					node.path("last_price").asDouble(),
					node.path("close_price").asDouble(),
					node.path("pnl").asDouble(),
					node.path("realised").asDouble(),
					node.path("unrealised").asDouble(),
					node.path("multiplier").asInt()
					));
		}
		return positions;
	}

	@Override
	public List<Holding> getHoldings(BrokerCredentials credentials) {
		String endpoint = baseUrl + "/portfolio/holdings";
		JsonNode response = executeGet(endpoint, credentials);
		List<Holding> holdings = new ArrayList<>();

		for (JsonNode node : response.path("data")) {
			int totalQty = node.path("quantity").asInt() + node.path("t1_quantity").asInt();
			holdings.add(new Holding(
					node.path("tradingsymbol").asText(),
					node.path("exchange").asText(),
					node.path("instrument_token").asText(),
					node.path("isin").asText(),
					totalQty,
					node.path("realised_quantity").asInt(),
					node.path("t1_quantity").asInt(),
					node.path("collateral_quantity").asInt(),
					node.path("authorised_quantity").asInt(),
					node.path("average_price").asDouble(),
					node.path("last_price").asDouble(),
					node.path("close_price").asDouble(),
					node.path("pnl").asDouble(),
					node.path("day_change").asDouble(),
					node.path("day_change_percentage").asDouble()
					));
		}
		return holdings;
	}

	@Override
	public Funds getFunds(BrokerCredentials credentials) {
		String endpoint = baseUrl + "/user/margins";
		JsonNode response = executeGet(endpoint, credentials);
		JsonNode equity = response.path("data").path("equity");

		return new Funds(
				equity.path("net").asDouble(),
				equity.path("available").path("cash").asDouble(),
				equity.path("available").path("cash").asDouble(), // Withdrawable approximations
				equity.path("available").path("opening_balance").asDouble(),
				equity.path("available").path("intraday_payin").asDouble(),
				equity.path("available").path("collateral").asDouble(),
				equity.path("available").path("adhoc_margin").asDouble(),
				0.0,
				equity.path("utilised").path("debits").asDouble(),
				equity.path("utilised").path("span").asDouble(),
				equity.path("utilised").path("exposure").asDouble(),
				equity.path("utilised").path("delivery").asDouble(),
				equity.path("utilised").path("option_premium").asDouble(),
				equity.path("utilised").path("m2m_realised").asDouble(),
				equity.path("utilised").path("m2m_unrealised").asDouble()
				);
	}

	@Override
	public List<Trade> getTrades(BrokerCredentials credentials) {
		String endpoint = baseUrl + "/trades";
		JsonNode response = executeGet(endpoint, credentials);
		List<Trade> trades = new ArrayList<>();

		for (JsonNode node : response.path("data")) {
			trades.add(new Trade(
					node.path("trade_id").asText(),
					node.path("order_id").asText(),
					node.path("exchange_order_id").asText(),
					node.path("tradingsymbol").asText(),
					node.path("exchange").asText(),
					node.path("instrument_token").asText(),
					node.path("transaction_type").asText(),
					node.path("product").asText(),
					node.path("quantity").asInt(),
					node.path("average_price").asDouble(),
					0L, 0L, 0L // Timestamps require date parsing in production
					));
		}
		return trades;
	}

	@Override
	public Quote getQuote(String symbol, BrokerCredentials credentials) {
		String endpoint = baseUrl + "/quote";
		String queryParam = "NSE:" + symbol; // Assuming NSE for this example

		String url = UriComponentsBuilder.fromUriString(endpoint)
				.queryParam("i", queryParam)
				.toUriString();

		JsonNode response = executeGet(url, credentials);
		JsonNode data = response.path("data").path(queryParam);

		return new Quote(
				symbol, "NSE",
				data.path("instrument_token").asText(),
				data.path("last_price").asDouble(),
				data.path("last_quantity").asInt(),
				data.path("volume").asLong(),
				data.path("average_price").asDouble(),
				data.path("depth").path("buy").get(0).path("price").asDouble(),
				data.path("depth").path("sell").get(0).path("price").asDouble(),
				data.path("depth").path("buy").get(0).path("quantity").asInt(),
				data.path("depth").path("sell").get(0).path("quantity").asInt(),
				data.path("buy_quantity").asInt(),
				data.path("sell_quantity").asInt(),
				data.path("ohlc").path("open").asDouble(),
				data.path("ohlc").path("high").asDouble(),
				data.path("ohlc").path("low").asDouble(),
				data.path("ohlc").path("close").asDouble(),
				data.path("net_change").asDouble(),
				0.0,
				data.path("lower_circuit_limit").asDouble(),
				data.path("upper_circuit_limit").asDouble(),
				data.path("open_interest").asDouble(),
				0L, 0L
				);
	}

	@Override
	public BrokerProfile getProfile(BrokerCredentials credentials) {
		String endpoint = baseUrl + "/user/profile";
		JsonNode response = executeGet(endpoint, credentials);
		JsonNode data = response.path("data");

		List<String> exchanges = new ArrayList<>();
		data.path("exchanges").forEach(e -> exchanges.add(e.asText()));

		List<String> products = new ArrayList<>();
		data.path("products").forEach(p -> products.add(p.asText()));

		List<String> orderTypes = new ArrayList<>();
		data.path("order_types").forEach(o -> orderTypes.add(o.asText()));

		return new BrokerProfile(
				data.path("user_id").asText(),
				brokerName,
				data.path("user_name").asText(),
				data.path("email").asText(),
				null, // phone not provided in Kite profile
				data.path("user_type").asText(),
				exchanges,
				products,
				orderTypes,
				!data.path("meta").path("demat_consent").asText().equals("empty"), // Assuming empty means no consent
				false,
				null, null, true
				);
	}

	@Override
	public List<Instrument> getInstruments(BrokerCredentials credentials) {
		// Zerodha returns a massive CSV file for instruments, not JSON [cite: 3]. 
		// In a production system, this should be downloaded once a day at 8:00 AM via a scheduled job.
		// Doing it per-request will block threads and consume massive memory.
		throw new UnsupportedOperationException("Instrument parsing must be done asynchronously via daily CSV download.");
	}

	// --- HTTP Execution Wrappers ---

	private JsonNode executePostOrPut(String url, MultiValueMap<String, String> body, BrokerCredentials credentials, boolean isPost) {
		try {
			String responseBody = (isPost ? restClient.post() : restClient.put())
					.uri(url)
					.header("X-Kite-Version", "3")
					.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.body(body)
					.retrieve()
					.onStatus(status -> status.isError(), (req, res) -> handleError(new String(res.getBody().readAllBytes()), res.getStatusCode().value()))
					.body(String.class);
			return objectMapper.readTree(responseBody);
		} catch (Exception e) {
			if (e instanceof BrokerException) throw (BrokerException) e;
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: " + e.getMessage());
		}
	}

	private JsonNode executeGet(String url, BrokerCredentials credentials) {
		try {
			String responseBody = restClient.get()
					.uri(url)
					.header("X-Kite-Version", "3")
					.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
					.retrieve()
					.onStatus(status -> status.isError(), (req, res) -> {
						String errorContent = new String(res.getBody().readAllBytes());

						// Call your custom error handler
						handleError(errorContent, res.getStatusCode().value());

						// Failsafe: If handleError() merely logs the error and does NOT throw,
						// we MUST throw an exception here to stop execution and prevent the null crash.
						throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Broker API returned HTTP " + res.getStatusCode().value());
					})
					.body(String.class);

			// Prevent Jackson from crashing with 'argument "content" is null'
			if (responseBody == null || responseBody.isBlank()) {
				throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: Response body is completely empty");
			}

			return objectMapper.readTree(responseBody);

		} catch (Exception e) {
			// 1. Check if it's already our custom exception
			if (e instanceof BrokerException) {
				throw (BrokerException) e;
			}

			// 2. Spring RestClient might wrap the exception thrown in onStatus(). Unwrap it.
			if (e.getCause() instanceof BrokerException) {
				throw (BrokerException) e.getCause();
			}

			// 3. Generic fallback for networking issues, etc.
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: " + e.getMessage());
		}
	}
	private void executeDelete(String url, BrokerCredentials credentials) {
		try {
			restClient.delete()
			.uri(url)
			.header("X-Kite-Version", "3")
			.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
			.retrieve()
			.onStatus(status -> status.isError(), (req, res) -> handleError(new String(res.getBody().readAllBytes()), res.getStatusCode().value()))
			.toBodilessEntity();
		} catch (Exception e) {
			if (e instanceof BrokerException) throw (BrokerException) e;
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: " + e.getMessage());
		}
	}

	private void handleError(String errorJson, int httpStatusCode) {
		try {
			JsonNode root = objectMapper.readTree(errorJson);
			String errorType = root.path("error_type").asText();
			String message = root.path("message").asText();

			ErrorCode canonicalCode = switch (errorType) {
			case "OrderException" -> ErrorCode.ORDER_REJECTED;
			case "InputException" -> ErrorCode.INVALID_QUANTITY;
			case "MarginException" -> ErrorCode.INSUFFICIENT_MARGIN;
			case "TokenException" -> ErrorCode.SESSION_EXPIRED;
			case "DataException" -> ErrorCode.QUOTE_NOT_AVAILABLE;
			default -> ErrorCode.INTERNAL_ERROR;
			};

			if (httpStatusCode == 429) canonicalCode = ErrorCode.RATE_LIMIT_EXCEEDED;

			if ("TokenException".equals(errorType)) {
				throw new BrokerAuthenticationException(canonicalCode, message, errorType, errorJson);
			}

			throw new BrokerException(canonicalCode, message, errorType, errorJson, null, httpStatusCode);
		} catch (BrokerException e) {
			throw e;
		} catch (Exception e) {
			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Failed to parse broker error response: " + errorJson, "ParseError", errorJson, null, httpStatusCode);
		}
	}
}
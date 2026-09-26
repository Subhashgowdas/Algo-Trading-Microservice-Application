//package com.algotrade.broker_service.brokerAdapterImpl;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Component;
//import org.springframework.util.LinkedMultiValueMap;
//import org.springframework.util.MultiValueMap;
//import org.springframework.web.client.RestClient;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import com.algotrade.broker_service.brokerAdapter.BrokerAdapter;
//import com.algotrade.broker_service.exception.BrokerException;
//import com.algotrade.broker_service.exception.ErrorCode;
//import com.algotrade.broker_service.model.BrokerCredentials;
//import com.algotrade.broker_service.model.BrokerProfile;
//import com.algotrade.broker_service.model.Funds;
//import com.algotrade.broker_service.model.Holding;
//import com.algotrade.broker_service.model.Instrument;
//import com.algotrade.broker_service.model.OrderRequest;
//import com.algotrade.broker_service.model.OrderResponse;
//import com.algotrade.broker_service.model.Position;
//import com.algotrade.broker_service.model.Quote;
//import com.algotrade.broker_service.model.Trade;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//@Component
//public class ZerodhaAdapter1 implements BrokerAdapter {
//
//	private static final Logger log = LoggerFactory.getLogger(ZerodhaAdapter1.class);
//
//	private final String brokerName;
//	private final String baseUrl;
//	private final RestClient restClient;
//	private final ObjectMapper objectMapper;
//
//	// Dynamic properties injected from application.yml
//	public ZerodhaAdapter1(
//			@Value("${broker.zerodha.name:ZERODHA}") String brokerName,
//			@Value("${broker.zerodha.base-url:https://api.kite.trade}") String baseUrl,
//			RestClient restClient, 
//			ObjectMapper objectMapper) {
//		this.brokerName = brokerName;
//		this.baseUrl = baseUrl;
//		this.restClient = restClient;
//		this.objectMapper = objectMapper;
//	}
//
//	@Override
//	public String getBrokerName() {
//		return brokerName;
//	}
//
//	private String getAuthHeader(BrokerCredentials credentials) {
//		return "token " + credentials.apiKey() + ":" + credentials.accessToken();
//	}
//
//	@Override
//	public OrderResponse placeOrder(OrderRequest request, BrokerCredentials credentials) {
//		String variety = request.variety() != null ? request.variety() : "regular";
//		String endpoint = baseUrl + "/orders/" + variety;
//
//		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
//		body.add("exchange", request.exchange());
//		body.add("tradingsymbol", request.symbol());
//		body.add("transaction_type", request.transactionType());
//		body.add("order_type", request.orderType());
//		body.add("quantity", String.valueOf(request.quantity()));
//		body.add("product", request.product());
//		body.add("validity", request.validity());
//
//		if (request.price() > 0) body.add("price", String.valueOf(request.price()));
//		if (request.triggerPrice() > 0) body.add("trigger_price", String.valueOf(request.triggerPrice()));
//		if (request.tag() != null) body.add("tag", request.tag());
//		body.add("autoslice", "true");
//
//		JsonNode response = executePostOrPut(endpoint, body, credentials, true);
//		String orderId = response.path("data").path("order_id").asText();
//
//		return new OrderResponse(orderId, null, null, null, "OPEN", 0, 0, 0.0, null, "Order Placed", System.currentTimeMillis(), 0L);
//	}
//
//	@Override
//	public OrderResponse cancelOrder(String orderId, BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/orders/regular/" + orderId;
//		executeDelete(endpoint, credentials);
//		return new OrderResponse(orderId, null, null, null, "CANCELLED", 0, 0, 0.0, null, "Order Cancelled", System.currentTimeMillis(), 0L);
//	}
//
//	@Override
//	public List<Position> getPositions(BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/portfolio/positions";
//		JsonNode response = executeGet(endpoint, credentials);
//		List<Position> positions = new ArrayList<>();
//
//		JsonNode netPositions = response.path("data").path("net");
//		for (JsonNode node : netPositions) {
//			int netQuantity = node.path("quantity").asInt();
//			String positionType = netQuantity > 0 ? "LONG" : (netQuantity < 0 ? "SHORT" : "CLOSED");
//
//			positions.add(new Position(
//					node.path("tradingsymbol").asText(), node.path("exchange").asText(), node.path("instrument_token").asText(),
//					node.path("product").asText(), positionType, netQuantity, node.path("buy_quantity").asInt(),
//					node.path("sell_quantity").asInt(), node.path("overnight_quantity").asInt(), node.path("day_buy_quantity").asInt(),
//					node.path("day_sell_quantity").asInt(), node.path("average_price").asDouble(), node.path("buy_price").asDouble(),
//					node.path("sell_price").asDouble(), node.path("last_price").asDouble(), node.path("close_price").asDouble(),
//					node.path("pnl").asDouble(), node.path("realised").asDouble(), node.path("unrealised").asDouble(), node.path("multiplier").asInt()
//					));
//		}
//		return positions;
//	}
//
//	@Override
//	public List<Holding> getHoldings(BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/portfolio/holdings";
//		JsonNode response = executeGet(endpoint, credentials);
//		List<Holding> holdings = new ArrayList<>();
//
//		for (JsonNode node : response.path("data")) {
//			int totalQty = node.path("quantity").asInt() + node.path("t1_quantity").asInt();
//			holdings.add(new Holding(
//					node.path("tradingsymbol").asText(), node.path("exchange").asText(), node.path("instrument_token").asText(),
//					node.path("isin").asText(), totalQty, node.path("realised_quantity").asInt(), node.path("t1_quantity").asInt(),
//					node.path("collateral_quantity").asInt(), node.path("authorised_quantity").asInt(), node.path("average_price").asDouble(),
//					node.path("last_price").asDouble(), node.path("close_price").asDouble(), node.path("pnl").asDouble(),
//					node.path("day_change").asDouble(), node.path("day_change_percentage").asDouble()
//					));
//		}
//		return holdings;
//	}
//
//	@Override
//	public Funds getFunds(BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/user/margins";
//		JsonNode response = executeGet(endpoint, credentials);
//		JsonNode equity = response.path("data").path("equity");
//
//		return new Funds(
//				equity.path("net").asDouble(), equity.path("available").path("cash").asDouble(), equity.path("available").path("cash").asDouble(),
//				equity.path("available").path("opening_balance").asDouble(), equity.path("available").path("intraday_payin").asDouble(),
//				equity.path("available").path("collateral").asDouble(), equity.path("available").path("adhoc_margin").asDouble(), 0.0,
//				equity.path("utilised").path("debits").asDouble(), equity.path("utilised").path("span").asDouble(),
//				equity.path("utilised").path("exposure").asDouble(), equity.path("utilised").path("delivery").asDouble(),
//				equity.path("utilised").path("option_premium").asDouble(), equity.path("utilised").path("m2m_realised").asDouble(),
//				equity.path("utilised").path("m2m_unrealised").asDouble()
//				);
//	}
//
//	@Override
//	public List<Trade> getTrades(BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/trades";
//		JsonNode response = executeGet(endpoint, credentials);
//		List<Trade> trades = new ArrayList<>();
//
//		for (JsonNode node : response.path("data")) {
//			trades.add(new Trade(
//					node.path("trade_id").asText(), node.path("order_id").asText(), node.path("exchange_order_id").asText(),
//					node.path("tradingsymbol").asText(), node.path("exchange").asText(), node.path("instrument_token").asText(),
//					node.path("transaction_type").asText(), node.path("product").asText(), node.path("quantity").asInt(),
//					node.path("average_price").asDouble(), 0L, 0L, 0L
//					));
//		}
//		return trades;
//	}
//
//	@Override
//	public Quote getQuote(String symbol, BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/quote";
//		String queryParam = "NSE:" + symbol; 
//
//		String url = UriComponentsBuilder.fromHttpUrl(endpoint).queryParam("i", queryParam).toUriString();
//		JsonNode response = executeGet(url, credentials);
//		JsonNode data = response.path("data").path(queryParam);
//
//		return new Quote(
//				symbol, "NSE", data.path("instrument_token").asText(), data.path("last_price").asDouble(),
//				data.path("last_quantity").asInt(), data.path("volume").asLong(), data.path("average_price").asDouble(),
//				data.path("depth").path("buy").get(0).path("price").asDouble(), data.path("depth").path("sell").get(0).path("price").asDouble(),
//				data.path("depth").path("buy").get(0).path("quantity").asInt(), data.path("depth").path("sell").get(0).path("quantity").asInt(),
//				data.path("buy_quantity").asInt(), data.path("sell_quantity").asInt(), data.path("ohlc").path("open").asDouble(),
//				data.path("ohlc").path("high").asDouble(), data.path("ohlc").path("low").asDouble(), data.path("ohlc").path("close").asDouble(),
//				data.path("net_change").asDouble(), 0.0, data.path("lower_circuit_limit").asDouble(), data.path("upper_circuit_limit").asDouble(),
//				data.path("open_interest").asDouble(), 0L, 0L
//				);
//	}
//
//	@Override
//	public BrokerProfile getProfile(BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/user/profile";
//		JsonNode response = executeGet(endpoint, credentials);
//		JsonNode data = response.path("data");
//
//		List<String> exchanges = new ArrayList<>();
//		data.path("exchanges").forEach(e -> exchanges.add(e.asText()));
//
//		List<String> products = new ArrayList<>();
//		data.path("products").forEach(p -> products.add(p.asText()));
//
//		List<String> orderTypes = new ArrayList<>();
//		data.path("order_types").forEach(o -> orderTypes.add(o.asText()));
//
//		return new BrokerProfile(
//				data.path("user_id").asText(), brokerName, data.path("user_name").asText(), data.path("email").asText(),
//				null, data.path("user_type").asText(), exchanges, products, orderTypes,
//				!data.path("meta").path("demat_consent").asText().equals("empty"), false, null, null, true
//				);
//	}
//
//	@Override
//	public List<Instrument> getInstruments(BrokerCredentials credentials) {
//		String endpoint = baseUrl + "/instruments";
//		List<Instrument> instruments = new ArrayList<>();
//
//		try {
//			// Zerodha returns CSV data for this endpoint, not JSON
//			String csvData = restClient.get()
//					.uri(endpoint)
//					.header("X-Kite-Version", "3")
//					.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
//					.retrieve()
//					.body(String.class);
//
//			if (csvData != null) {
//				String[] lines = csvData.split("\n");
//				// Skip header line and parse
//				for (int i = 1; i < lines.length; i++) {
//					String[] columns = lines[i].split(",");
//					if (columns.length > 11) {
//						instruments.add(new Instrument(
//								columns[2], columns[11], columns[3], columns[0], columns[1], null,
//								columns[10], columns[9], (int) Double.parseDouble(columns[8]), Double.parseDouble(columns[7]),
//								columns[5], columns.length > 6 && !columns[6].isEmpty() ? Double.parseDouble(columns[6]) : 0.0
//								));
//					}
//				}
//			}
//			return instruments;
//		} catch (Exception e) {
//			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "Failed to download/parse instrument list: " + e.getMessage());
//		}
//	}
//
//	// --- HTTP Execution Wrappers ---
//
//	private JsonNode executePostOrPut(String url, MultiValueMap<String, String> body, BrokerCredentials credentials, boolean isPost) {
//		try {
//			String responseBody = (isPost ? restClient.post() : restClient.put())
//					.uri(url)
//					.header("X-Kite-Version", "3")
//					.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
//					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
//					.body(body)
//					.retrieve()
//					.onStatus(status -> status.isError(), (req, res) -> handleError(new String(res.getBody().readAllBytes()), res.getStatusCode().value()))
//					.body(String.class);
//			return objectMapper.readTree(responseBody);
//		} catch (Exception e) {
//			if (e instanceof BrokerException) throw (BrokerException) e;
//			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: " + e.getMessage());
//		}
//	}
//
//	private JsonNode executeGet(String url, BrokerCredentials credentials) {
//		try {
//			String responseBody = restClient.get()
//					.uri(url)
//					.header("X-Kite-Version", "3")
//					.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
//					.retrieve()
//					.onStatus(status -> status.isError(), (req, res) -> handleError(new String(res.getBody().readAllBytes()), res.getStatusCode().value()))
//					.body(String.class);
//			return objectMapper.readTree(responseBody);
//		} catch (Exception e) {
//			if (e instanceof BrokerException) throw (BrokerException) e;
//			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: " + e.getMessage());
//		}
//	}
//
//	private void executeDelete(String url, BrokerCredentials credentials) {
//		try {
//			restClient.delete()
//			.uri(url)
//			.header("X-Kite-Version", "3")
//			.header(HttpHeaders.AUTHORIZATION, getAuthHeader(credentials))
//			.retrieve()
//			.onStatus(status -> status.isError(), (req, res) -> handleError(new String(res.getBody().readAllBytes()), res.getStatusCode().value()))
//			.toBodilessEntity();
//		} catch (Exception e) {
//			if (e instanceof BrokerException) throw (BrokerException) e;
//			throw new BrokerException(ErrorCode.INTERNAL_ERROR, "API Call Failed: " + e.getMessage());
//		}
//	}
//
//	private void handleError(String errorJson, int httpStatusCode){
//		try {
//			JsonNode root = objectMapper.readTree(errorJson);
//			String errorType = root.path("error_type").asText();
//			String message = root.path("message").asText();
//
//			ErrorCode canonicalCode = switch (errorType) {
//			case "OrderException" -> ErrorCode.ORDER_REJECTED;
//			case "InputException" -> ErrorCode.INVALID_QUANTITY;
//			case "MarginException" -> ErrorCode.INSUFFICIENT_MARGIN;
//			case "TokenException" -> ErrorCode.SESSION_EXPIRED;
//			case "DataException" -> ErrorCode.QUOTE_NOT_AVAILABLE;
//			default -> ErrorCode.INTERNAL_ERROR;
//			};
//
//			if (httpStatusCode == 429) canonicalCode = ErrorCode.RATE_LIMIT_EXCEEDED;
//
//			throw new BrokerException(canonicalCode, message, errorType, errorJson, null, httpStatusCode);
//		}catch(Exception ex){
//			ex.printStackTrace();
//		}
//	}
//}
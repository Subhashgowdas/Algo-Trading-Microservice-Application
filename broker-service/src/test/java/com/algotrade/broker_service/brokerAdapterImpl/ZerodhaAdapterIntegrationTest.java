//package com.algotrade.broker_service.brokerAdapterImpl;
//
//import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
//import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
//import static com.github.tomakehurst.wiremock.client.WireMock.get;
//import static com.github.tomakehurst.wiremock.client.WireMock.matching;
//import static com.github.tomakehurst.wiremock.client.WireMock.post;
//import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
//import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertNotNull;
//import static org.junit.jupiter.api.Assertions.assertThrows;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.web.client.RestClient;
//
//import com.algotrade.broker_service.exception.BrokerException;
//import com.algotrade.broker_service.exception.ErrorCode;
//import com.algotrade.broker_service.model.BrokerCredentials;
//import com.algotrade.broker_service.model.Funds;
//import com.algotrade.broker_service.model.OrderRequest;
//import com.algotrade.broker_service.model.OrderResponse;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.github.tomakehurst.wiremock.junit5.WireMockTest;
//
//@WireMockTest(httpPort = 8082)
//class ZerodhaAdapterIntegrationTest {
//
//	private ZerodhaAdapter zerodhaAdapter;
//	private BrokerCredentials credentials;
//
//	@BeforeEach
//	void setUp() {
//		// Initialize the RestClient (Normally injected by Spring)
//		RestClient restClient = RestClient.builder().build();
//
//		// Pass the dynamic variables (Mock localhost URL)
//		zerodhaAdapter = new ZerodhaAdapter("ZERODHA", "http://localhost:8082", restClient, new ObjectMapper());
//
//		// Setup mock credentials
//		credentials = BrokerCredentials.of("ZERODHA", "VAH532", "mal5n4i5bx6pe3m1", "g0o7hricwubsvnmjdde0g5alyti03ndz");
//	}
//
//	@Test
//	void testGetFunds_Success() {
//		// 1. Arrange: Stub the WireMock server for a GET /user/margins request
//		String mockFundsResponse = """
//				{
//				    "status": "success",
//				    "data": {
//				        "equity": {
//				            "net": 150000.50,
//				            "available": {
//				                "cash": 100000.00,
//				                "opening_balance": 100000.00,
//				                "intraday_payin": 0.0,
//				                "collateral": 50000.50,
//				                "adhoc_margin": 0.0
//				            },
//				            "utilised": {
//				                "debits": 25000.00,
//				                "span": 15000.00,
//				                "exposure": 10000.00,
//				                "delivery": 0.0,
//				                "option_premium": 0.0,
//				                "m2m_realised": 500.00,
//				                "m2m_unrealised": -200.00
//				            }
//				        }
//				    }
//				}
//				""";
//
//		stubFor(get(urlEqualTo("/user/margins"))
//				.withHeader("Authorization", equalTo("token test_api_key:test_access_token"))
//				.willReturn(aResponse()
//						.withStatus(200)
//						.withHeader("Content-Type", "application/json")
//						.withBody(mockFundsResponse)));
//
//		// 2. Act: Call the method
//		Funds funds = zerodhaAdapter.getFunds(credentials);
//
//		// 3. Assert: Validate that parsing was successful
//		assertNotNull(funds);
//		assertEquals(150000.50, funds.availableMargin());
//		assertEquals(100000.00, funds.availableCash());
//		assertEquals(25000.00, funds.totalUtilisedMargin());
//		assertEquals(15000.00, funds.spanMargin());
//	}
//
//	@Test
//	void testPlaceOrder_Success() {
//		String mockZerodhaResponse = """
//				{
//				    "status": "success",
//				    "data": {
//				        "order_id": "240108010918222"
//				    }
//				}
//				""";
//
//		// Validate that it strictly enforces Form-UrlEncoded format (not JSON)
//		stubFor(post(urlEqualTo("/orders/regular"))
//				.withHeader("Authorization", matching("^token .*:.*$"))
//
//				// 'containing' is already flexible, but you can also use 'matching' for regex validation
//				.withHeader("Content-Type", matching(".*application/x-www-form-urlencoded.*"))
//				.willReturn(aResponse()
//						.withStatus(200)
//						.withHeader("Content-Type", "application/json")
//						.withBody(mockZerodhaResponse)));
//
//		OrderRequest request = new OrderRequest(
//				"RELIANCE", "NSE", "LIMIT", "BUY", "regular", 
//				10, 2500.50, 0, "CNC", "DAY", 0, 0, 0, "testAlgo", 0, 0, 0, 0, 0
//				);
//
//		OrderResponse response = zerodhaAdapter.placeOrder(request, credentials);
//
//		assertNotNull(response);
//		assertEquals("240108010918222", response.orderId());
//		assertEquals("OPEN", response.status());
//	}
//
//	@Test
//	void testExceptionMapping_TokenExpired() {
//		String mockErrorResponse = """
//				{
//				    "status": "error",
//				    "message": "Token is invalid or has expired.",
//				    "error_type": "TokenException"
//				}
//				""";
//
//		stubFor(get(urlEqualTo("/portfolio/positions"))
//				.willReturn(aResponse()
//						.withStatus(403)
//						.withHeader("Content-Type", "application/json")
//						.withBody(mockErrorResponse)));
//
//		BrokerException exception = assertThrows(BrokerException.class, () -> {
//			zerodhaAdapter.getPositions(credentials);
//		});
//
//		// Verifies the HTTP 403 / TokenException is translated into our canonical ErrorCode
//		assertEquals(ErrorCode.SESSION_EXPIRED, exception.getErrorCode());
//		assertEquals("TokenException", exception.getBrokerErrorCode());
//	}
//}
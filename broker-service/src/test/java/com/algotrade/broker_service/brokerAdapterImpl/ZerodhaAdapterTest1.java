/*
 * package com.algotrade.broker_service.brokerAdapterImpl;
 * 
 * import static org.junit.jupiter.api.Assertions.assertEquals; import static
 * org.junit.jupiter.api.Assertions.assertNotNull; import static
 * org.junit.jupiter.api.Assertions.assertThrows; import static
 * org.springframework.test.web.client.match.MockRestRequestMatchers.header;
 * import static
 * org.springframework.test.web.client.match.MockRestRequestMatchers.method;
 * import static
 * org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
 * import static
 * org.springframework.test.web.client.response.MockRestResponseCreators.
 * withStatus; import static
 * org.springframework.test.web.client.response.MockRestResponseCreators.
 * withSuccess;
 * 
 * import org.junit.jupiter.api.BeforeEach; import org.junit.jupiter.api.Test;
 * import org.springframework.http.HttpMethod; import
 * org.springframework.http.MediaType; import
 * org.springframework.test.web.client.MockRestServiceServer; import
 * org.springframework.web.client.RestClient;
 * 
 * import com.algotrade.broker_service.exception.BrokerException; import
 * com.algotrade.broker_service.exception.ErrorCode; import
 * com.algotrade.broker_service.model.BrokerCredentials; import
 * com.algotrade.broker_service.model.Funds; import
 * com.algotrade.broker_service.model.OrderRequest; import
 * com.algotrade.broker_service.model.OrderResponse; import
 * com.fasterxml.jackson.databind.ObjectMapper;
 * 
 * class ZerodhaAdapterTest1 {
 * 
 * private ZerodhaAdapter1 ZerodhaAdapter1; private MockRestServiceServer
 * mockServer; private BrokerCredentials credentials;
 * 
 * @BeforeEach void setUp() { // 1. Create a RestClient Builder and bind the
 * Spring Mock Server to it RestClient.Builder builder = RestClient.builder();
 * mockServer = MockRestServiceServer.bindTo(builder).build();
 * 
 * // 2. Pass the dynamic base URL and the mocked RestClient into the Adapter
 * ZerodhaAdapter1 = new ZerodhaAdapter1("ZERODHA", "https://api.kite.trade",
 * builder.build(), new ObjectMapper());
 * 
 * credentials = BrokerCredentials.of("ZERODHA", "VAH532", "mal5n4i5bx6pe3m1",
 * "g0o7hricwubsvnmjdde0g5alyti03ndz"); }
 * 
 * @Test void testGetFunds_Success() { // 1. Arrange: Prepare mock JSON and
 * expect a GET request with exact headers String mockFundsResponse = """ {
 * "status": "success", "data": { "equity": { "net": 150000.50, "available": {
 * "cash": 100000.00, "opening_balance": 100000.00, "intraday_payin": 0.0,
 * "collateral": 50000.50, "adhoc_margin": 0.0 }, "utilised": { "debits":
 * 25000.00, "span": 15000.00, "exposure": 10000.00, "delivery": 0.0,
 * "option_premium": 0.0, "m2m_realised": 500.00, "m2m_unrealised": -200.00 } }
 * } } """;
 * 
 * // Assuming 'credentials' is setup in your @BeforeEach method String
 * expectedAuthHeader = "token " + credentials.apiKey() + ":" +
 * credentials.accessToken();
 * 
 * mockServer.expect(requestTo("https://api.kite.trade/user/margins"))
 * .andExpect(method(HttpMethod.GET)) // Passes the dynamically built string
 * .andExpect(header("Authorization", expectedAuthHeader))
 * .andRespond(withSuccess(mockFundsResponse, MediaType.APPLICATION_JSON)); //
 * 2. Act Funds funds = ZerodhaAdapter1.getFunds(credentials);
 * 
 * // 3. Assert: Validate that our parsing engine worked correctly
 * mockServer.verify(); assertNotNull(funds); assertEquals(150000.50,
 * funds.availableMargin()); assertEquals(100000.00, funds.availableCash());
 * assertEquals(25000.00, funds.totalUtilisedMargin()); }
 * 
 * @Test void testPlaceOrder_Success() { String mockZerodhaResponse = """ {
 * "status": "success", "data": { "order_id": "240108010918222" } } """;
 * 
 * mockServer.expect(requestTo("https://api.kite.trade/orders/regular"))
 * .andExpect(method(HttpMethod.POST)) .andExpect(header("Content-Type",
 * MediaType.APPLICATION_FORM_URLENCODED_VALUE))
 * .andRespond(withSuccess(mockZerodhaResponse, MediaType.APPLICATION_JSON));
 * 
 * OrderRequest request = new OrderRequest( "RELIANCE", "NSE", "LIMIT", "BUY",
 * "regular", 10, 2500.50, 0, "CNC", "DAY", 0, 0, 0, "testAlgo", 0, 0, 0, 0, 0
 * );
 * 
 * OrderResponse response = ZerodhaAdapter1.placeOrder(request, credentials);
 * 
 * mockServer.verify(); assertNotNull(response); assertEquals("240108010918222",
 * response.orderId()); assertEquals("OPEN", response.status()); }
 * 
 * @Test void testExceptionMapping_TokenExpired() { String mockErrorResponse =
 * """ { "status": "error", "message": "Token is invalid or has expired.",
 * "error_type": "TokenException" } """;
 * 
 * mockServer.expect(requestTo("https://api.kite.trade/portfolio/positions"))
 * .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN)
 * .body(mockErrorResponse) .contentType(MediaType.APPLICATION_JSON));
 * 
 * BrokerException exception = assertThrows(BrokerException.class, () -> {
 * ZerodhaAdapter1.getPositions(credentials); });
 * 
 * mockServer.verify(); assertEquals(ErrorCode.SESSION_EXPIRED,
 * exception.getErrorCode()); assertEquals("TokenException",
 * exception.getBrokerErrorCode()); } }
 */
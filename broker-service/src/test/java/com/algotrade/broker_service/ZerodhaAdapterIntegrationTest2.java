package com.algotrade.broker_service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.algotrade.broker_service.brokerAdapterImpl.ZerodhaAdapter;
import com.algotrade.broker_service.exception.BrokerException;
import com.algotrade.broker_service.exception.ErrorCode;
import com.algotrade.broker_service.model.BrokerCredentials;
import com.algotrade.broker_service.model.Funds;
import com.algotrade.broker_service.model.OrderRequest;
import com.algotrade.broker_service.model.OrderResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class ZerodhaAdapterUnitTest2 {

    private ZerodhaAdapter zerodhaAdapter;
    private BrokerCredentials credentials;
    private MockRestServiceServer mockServer;
    private ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        // Build a RestClient that we can control
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();

        // Create the adapter with the mock‑ready RestClient
        RestClient restClient = restClientBuilder.build();
        zerodhaAdapter = new ZerodhaAdapter("ZERODHA", "http://localhost:8080",null, restClient, objectMapper);

        credentials = BrokerCredentials.of("ZERODHA", "VAH532", "mal5n4i5bx6pe3m1", "g0o7hricwubsvnmjdde0g5alyti03ndz");
    }

    @Test
    void testGetFunds_Success() {
        String mockResponse = """
            {
                "status": "success",
                "data": {
                    "equity": {
                        "net": 150000.50,
                        "available": {
                            "cash": 100000.00,
                            "opening_balance": 100000.00,
                            "intraday_payin": 0.0,
                            "collateral": 50000.50,
                            "adhoc_margin": 0.0
                        },
                        "utilised": {
                            "debits": 25000.00,
                            "span": 15000.00,
                            "exposure": 10000.00,
                            "delivery": 0.0,
                            "option_premium": 0.0,
                            "m2m_realised": 500.00,
                            "m2m_unrealised": -200.00
                        }
                    }
                }
            }""";

        // Tell the mock server what request to expect and how to respond
        mockServer.expect(requestTo("/user/margins"))
                  .andExpect(method(HttpMethod.GET))
                  .andExpect(header("Authorization", "token test_api_key:test_access_token"))
                  .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        Funds funds = zerodhaAdapter.getFunds(credentials);

        assertNotNull(funds);
        assertEquals(150000.50, funds.availableMargin());
        assertEquals(100000.00, funds.availableCash());
        assertEquals(25000.00, funds.totalUtilisedMargin());
        assertEquals(15000.00, funds.spanMargin());

        // Verify that exactly the expected request was made
        mockServer.verify();
    }

    @Test
    void testPlaceOrder_Success() {
        String mockResponse = """
            {
                "status": "success",
                "data": {
                    "order_id": "240108010918222"
                }
            }""";

        mockServer.expect(requestTo("/orders/regular"))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(header("Content-Type", "application/x-www-form-urlencoded"))
                  .andRespond(withSuccess(mockResponse, MediaType.APPLICATION_JSON));

        OrderRequest request = new OrderRequest(
            "RELIANCE", "NSE", "LIMIT", "BUY", "regular",
            10, 2500.50, 0, "CNC", "DAY", 0, 0, 0, "testAlgo", 0, 0, 0, 0, 0
        );

        OrderResponse response = zerodhaAdapter.placeOrder(request, credentials);

        assertNotNull(response);
        assertEquals("240108010918222", response.orderId());
        assertEquals("OPEN", response.status());
        mockServer.verify();
    }

    @Test
    void testExceptionMapping_TokenExpired() {
        String mockErrorResponse = """
            {
                "status": "error",
                "message": "Token is invalid or has expired.",
                "error_type": "TokenException"
            }""";

        mockServer.expect(requestTo("/portfolio/positions"))
                  .andExpect(method(HttpMethod.GET))
                  .andRespond(withStatus(org.springframework.http.HttpStatus.FORBIDDEN)
                      .body(mockErrorResponse)
                      .contentType(MediaType.APPLICATION_JSON));

        BrokerException exception = assertThrows(BrokerException.class, () -> {
            zerodhaAdapter.getPositions(credentials);
        });

        assertEquals(ErrorCode.SESSION_EXPIRED, exception.getErrorCode());
        assertEquals("TokenException", exception.getBrokerErrorCode());
        mockServer.verify();
    }
}
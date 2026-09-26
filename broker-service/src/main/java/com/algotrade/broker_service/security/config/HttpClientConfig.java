package com.algotrade.broker_service.security.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {
	
	@Bean
    public RestClient restClient(RestClient.Builder builder) {
        // You can add your ultra-low latency Apache HttpClient customizer here
        return builder.build();
    }

	public RestClient optimizedRestClient() {
		SocketConfig socketConfig = SocketConfig.custom()
				.setTcpNoDelay(true)
				.setSoTimeout(Timeout.ofSeconds(3))
				.build();

		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setDefaultSocketConfig(socketConfig);
		connectionManager.setMaxTotal(300);
		connectionManager.setDefaultMaxPerRoute(150);

		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout(Timeout.ofMilliseconds(500))
				.setResponseTimeout(Timeout.ofSeconds(5))
				.build();

		CloseableHttpClient httpClient = HttpClients.custom()
				.setConnectionManager(connectionManager)
				.setDefaultRequestConfig(requestConfig)
				.setKeepAliveStrategy((response, context) -> TimeValue.ofSeconds(30))
				.build();

		return RestClient.builder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory(httpClient))
				.build();
	}

}

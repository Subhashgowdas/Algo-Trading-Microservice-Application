package com.algotrade.authservice.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Otp otp, RateLimit rateLimit) {
	public record Otp(int ttl, int length) {}
	public record RateLimit(int registerInitiate) {}
}

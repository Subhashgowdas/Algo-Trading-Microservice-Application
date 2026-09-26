package com.algotrade.authservice.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.otp")
public class OtpProperties {

	// Registration / login OTP
	private int length = 6;
	private long ttlSeconds = 300;
	private int maxAttempts = 3;
	private long resendDelaySeconds = 60;

	// Password reset token (can be same length but different TTL/rate)
	private long resetTokenTtlSeconds = 900;        // 15 minutes
	private long resetTokenResendDelaySeconds = 120; // 2 minutes
	private int resetTokenMaxAttempts = 3;
}

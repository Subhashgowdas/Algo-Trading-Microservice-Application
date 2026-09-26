package com.algotrade.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {
    private long defaultTtlSeconds = 300;
    private int otpTtlSeconds = 300;
    private int otpRateLimitMaxAttempts = 5;
    private int otpRateLimitWindowSeconds = 600;
}
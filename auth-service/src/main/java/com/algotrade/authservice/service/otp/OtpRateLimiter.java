package com.algotrade.authservice.service.otp;

import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Component;

import com.algotrade.common.cache.CacheKeyPrefix;
import com.algotrade.common.cache.CacheService;

@Component
public class OtpRateLimiter {
    private final CacheService cacheService;

    public OtpRateLimiter(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /** OTP rate limiter (registration/login) */
    public boolean isAllowed(String email, long windowSeconds, int maxAttempts) {
        return isAllowed(email, windowSeconds, maxAttempts, CacheKeyPrefix.AUTH_RATE_LIMIT_OTP);
    }

    /** Generic rate limiter with custom key prefix */
    public boolean isAllowed(String email, long windowSeconds, int maxAttempts, String rateLimitKeyPrefix) {
        String key = rateLimitKeyPrefix + email;
        Long count = cacheService.increment(key);
        if (count == 1) {
            cacheService.expire(key, windowSeconds, TimeUnit.SECONDS);
        }
        return count <= maxAttempts;
    }

    public long getRemainingAttempts(String email) {
        String key = CacheKeyPrefix.AUTH_RATE_LIMIT_OTP + email;
        return cacheService.exists(key) ? Long.parseLong(cacheService.get(key).orElse(0L).toString()) : 0;
    }
}
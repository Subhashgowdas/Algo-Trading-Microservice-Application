package com.algotrade.common.cache;

public final class CacheKeyPrefix {
	private CacheKeyPrefix() {}
	
	public static final String AUTH_OTP = "auth:otp:";
	public static final String AUTH_RESET_OTP = "auth:reset:otp:";
	public static final String AUTH_RATE_LIMIT_OTP = "auth:rate:otp:";
	public static final String AUTH_RESET_RATE_LIMIT_OTP = "auth:reset:rate:otp:";
}

package com.algotrade.authservice.service.otp;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.algotrade.authservice.auth.config.OtpProperties;
import com.algotrade.authservice.service.email.EmailTemplateService;
import com.algotrade.common.cache.CacheKeyPrefix;
import com.algotrade.common.cache.CacheService;
import com.algotrade.common.config.CacheProperties;
import com.algotrade.common.email.EmailService;
import com.algotrade.common.exception.RateLimitExceededException;

@Service
public class OtpServiceImpl implements OtpService {

	private final CacheService cacheService;
	private final OtpGenerator otpGenerator;
	private final EmailService emailService;
	private final OtpRateLimiter rateLimiter;
	private final OtpProperties otpProperties;
	private final CacheProperties cacheProperties;
	private final EmailTemplateService emailTemplateService;


	public OtpServiceImpl(CacheService cacheService,
			OtpGenerator otpGenerator,
			EmailService emailService,
			OtpRateLimiter rateLimiter,
			OtpProperties otpProperties,
			CacheProperties cacheProperties,
			EmailTemplateService emailTemplateService) {
		this.cacheService = cacheService;
		this.otpGenerator = otpGenerator;
		this.emailService = emailService;
		this.rateLimiter = rateLimiter;
		this.otpProperties = otpProperties;
		this.cacheProperties = cacheProperties;
		this.emailTemplateService = emailTemplateService;
	}

	@Override
	public void sendOtp(String email) {
	    if (!rateLimiter.isAllowed(email, otpProperties.getResendDelaySeconds(), otpProperties.getMaxAttempts())) {
	        throw new RateLimitExceededException("Too many OTP requests. Please wait.");
	    }
	    String otp = otpGenerator.generate(otpProperties.getLength());
	    cacheService.set(CacheKeyPrefix.AUTH_OTP + email, otp, otpProperties.getTtlSeconds(), TimeUnit.SECONDS);

	    Map<String, Object> model = Map.of(
	        "otp", otp,
	        "expiryMinutes", otpProperties.getTtlSeconds() / 60
	    );
	    EmailTemplateService.RenderedEmail rendered = emailTemplateService.render("REG_OTP", model);
	    emailService.sendEmail(email, rendered.subject(), rendered.body());
	}

	@Override
	public boolean verifyOtp(String email, String otp) {
		String key = CacheKeyPrefix.AUTH_OTP + email;
		var stored = cacheService.get(key);
		if (stored.isPresent() && stored.get().equals(otp)) {
			cacheService.delete(key);
			return true;
		}
		return false;
	}

	@Override
	public void sendResetToken(String email) {
	    if (!rateLimiter.isAllowed(email, otpProperties.getResetTokenResendDelaySeconds(),
	            otpProperties.getResetTokenMaxAttempts(), CacheKeyPrefix.AUTH_RESET_RATE_LIMIT_OTP)) {
	        throw new RateLimitExceededException("Too many reset attempts. Please wait.");
	    }
	    String token = otpGenerator.generate(otpProperties.getLength());
	    cacheService.set(CacheKeyPrefix.AUTH_RESET_OTP + email, token, otpProperties.getResetTokenTtlSeconds(), TimeUnit.SECONDS);

	    Map<String, Object> model = Map.of(
	        "token", token,
	        "expiryMinutes", otpProperties.getResetTokenTtlSeconds() / 60
	    );
	    EmailTemplateService.RenderedEmail rendered = emailTemplateService.render("RESET_PWD", model);
	    emailService.sendEmail(email, rendered.subject(), rendered.body());
	}

	@Override
	public boolean verifyResetToken(String email, String token) {
		String key = CacheKeyPrefix.AUTH_RESET_OTP + email;
		var stored = cacheService.get(key);
		if (stored.isPresent() && stored.get().equals(token)) {
			cacheService.delete(key); // one-time use
			return true;
		}
		return false;
	}
}
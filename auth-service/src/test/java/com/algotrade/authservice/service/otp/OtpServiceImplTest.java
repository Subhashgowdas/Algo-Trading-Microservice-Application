package com.algotrade.authservice.service.otp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.algotrade.authservice.auth.config.OtpProperties;
import com.algotrade.common.cache.CacheKeyPrefix;
import com.algotrade.common.cache.CacheService;
import com.algotrade.common.config.CacheProperties;
import com.algotrade.common.email.EmailService;
import com.algotrade.common.exception.RateLimitExceededException;

@ExtendWith(MockitoExtension.class)
class OtpServiceImplTest {

    @Mock
    private CacheService cacheService;
    @Mock
    private OtpGenerator otpGenerator;
    @Mock
    private EmailService emailService;
    @Mock
    private OtpRateLimiter rateLimiter;
    @Mock
    private OtpProperties otpProperties;
    @Mock
    private CacheProperties cacheProperties;

    @InjectMocks
    private OtpServiceImpl otpService;

    private final String email = "user@example.com";
    private final String otp = "123456";

    @BeforeEach
    void setUp() {
        // Default property stubs
        when(otpProperties.getLength()).thenReturn(6);
        when(otpProperties.getTtlSeconds()).thenReturn(300L);
        when(otpProperties.getMaxAttempts()).thenReturn(3);
        when(otpProperties.getResendDelaySeconds()).thenReturn(60L);
    }

    @Test
    @DisplayName("sendOtp should generate OTP, store in cache, and send email")
    void shouldSendOtpSuccessfully() {
        when(rateLimiter.isAllowed(email, 60L, 3)).thenReturn(true);
        when(otpGenerator.generate(6)).thenReturn(otp);

        otpService.sendOtp(email);

        // Verify OTP stored in Redis with correct key and TTL
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);
        verify(cacheService).set(keyCaptor.capture(), valueCaptor.capture(), eq(300L), eq(TimeUnit.SECONDS));
        assertThat(keyCaptor.getValue()).isEqualTo(CacheKeyPrefix.AUTH_OTP + email);
        assertThat(valueCaptor.getValue()).isEqualTo(otp);

        // Verify email sent
        verify(emailService).sendEmail(email, "Your Login OTP", "OTP is: " + otp);
    }

    @Test
    @DisplayName("sendOtp should throw RateLimitExceededException when rate limit hit")
    void shouldThrowWhenRateLimited() {
        when(rateLimiter.isAllowed(email, 60L, 3)).thenReturn(false);

        assertThrows(RateLimitExceededException.class, () -> otpService.sendOtp(email));
        verify(cacheService, never()).set(anyString(), any(), anyLong(), any());
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("verifyOtp should return true for correct OTP and delete it")
    void shouldVerifyCorrectOtp() {
        when(cacheService.get(CacheKeyPrefix.AUTH_OTP + email)).thenReturn(Optional.of(otp));

        boolean result = otpService.verifyOtp(email, otp);
        assertTrue(result);
        verify(cacheService).delete(CacheKeyPrefix.AUTH_OTP + email);
    }

    @Test
    @DisplayName("verifyOtp should return false for incorrect OTP and not delete")
    void shouldRejectIncorrectOtp() {
        when(cacheService.get(CacheKeyPrefix.AUTH_OTP + email)).thenReturn(Optional.of(otp));

        boolean result = otpService.verifyOtp(email, "000000");
        assertFalse(result);
        verify(cacheService, never()).delete(anyString());
    }

    @Test
    @DisplayName("verifyOtp should return false when no OTP stored")
    void shouldReturnFalseWhenNoOtp() {
        when(cacheService.get(CacheKeyPrefix.AUTH_OTP + email)).thenReturn(Optional.empty());

        boolean result = otpService.verifyOtp(email, otp);
        assertFalse(result);
        verify(cacheService, never()).delete(anyString());
    }
}
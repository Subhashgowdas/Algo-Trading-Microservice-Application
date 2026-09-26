package com.algotrade.authservice.service.otp;

public interface OtpService {
    void sendOtp(String email);
    boolean verifyOtp(String email, String otp);    
    void sendResetToken(String email);
    boolean verifyResetToken(String email, String token);
}
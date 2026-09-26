package com.algotrade.authservice.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.algotrade.authservice.service.otp.OtpService;

@RestController
@RequestMapping("/test")
public class TestController {

    private final OtpService otpService;

    public TestController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/otp")
    public String sendTestOtp(@RequestParam String email) {
        otpService.sendOtp(email);
        return "OTP sent. Check application logs for the mock email.";
    }
}

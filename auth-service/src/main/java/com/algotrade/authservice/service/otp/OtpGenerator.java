package com.algotrade.authservice.service.otp;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class OtpGenerator {
	
	private final SecureRandom random = new SecureRandom();
	
	public String generate(int length) {
		if(length <= 0) {
			throw new IllegalArgumentException("OTP length must be positive");
		}
		int bound = (int)Math.pow(10, length);
		int number = random.nextInt(bound);
		return String.format("%0"+length+"d", number);
	}

}

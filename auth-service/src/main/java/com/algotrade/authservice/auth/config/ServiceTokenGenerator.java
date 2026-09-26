package com.algotrade.authservice.auth.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.algotrade.authservice.service.jwt.JwtService;
import com.algotrade.authservice.service.otp.OtpGenerator;

@Component
@Profile("generate-service-token")
public class ServiceTokenGenerator implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ServiceTokenGenerator.class);

    private final JwtService jwtService;
    private final OtpGenerator otpGenerator;

    public ServiceTokenGenerator(JwtService jwtService,
                                 OtpGenerator otpGenerator) {
        this.jwtService = jwtService;
        this.otpGenerator = otpGenerator;
    }
    
	/*
	 * To run this calss do the setting in Run Argument > Arguments > Program Argumnets tab >
	 * --spring.profiles.active=generate-service-token
	 */
    
    @Override
    public void run(String... args) {

        String token = jwtService.generateServiceToken("broker-service");

        System.out.println("=================================");
        System.out.println(token);
        System.out.println("=================================");

        log.info("Service token generated");

        System.exit(0);
    }
}
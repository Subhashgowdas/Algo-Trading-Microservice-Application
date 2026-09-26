package com.algotrade.broker.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class StartupCheck implements CommandLineRunner {

    @Value("${app.internal.service-token:NOT_FOUND}")
    private String token;

    @Override
    public void run(String... args) {
        log.info("Token found? {}", !"NOT_FOUND".equals(token));
    }
}
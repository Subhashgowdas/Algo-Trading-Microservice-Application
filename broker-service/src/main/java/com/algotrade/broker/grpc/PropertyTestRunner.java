package com.algotrade.broker.grpc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Component
public class PropertyTestRunner implements CommandLineRunner {

    @Value("${app.internal.service-token}")
    private String token;

    @Override
    public void run(String... args) {
        System.out.println("Property token = " + token);
    }
}
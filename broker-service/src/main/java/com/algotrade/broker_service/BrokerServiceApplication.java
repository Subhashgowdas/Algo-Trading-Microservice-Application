package com.algotrade.broker_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@ComponentScan(basePackages = {
		"com.algotrade.broker_service",
		"com.algotrade.common"
})
public class BrokerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BrokerServiceApplication.class, args);
	}

}

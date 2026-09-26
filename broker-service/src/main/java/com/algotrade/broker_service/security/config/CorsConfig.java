package com.algotrade.broker_service.security.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Production ready CORS configuration.
 *
 * Allows the React frontend (localhost:3000) to call the broker service.
 * In production, update allowedOrigins with your real domain.
 */
@Configuration
public class CorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();

		// Domains allowed to access this API
		config.setAllowedOrigins(List.of("http://localhost:3000"));

		// HTTP methods allowed in cross‑origin requests
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

		// Headers the browser may send
		config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

		// Allow credentials (cookies, Authorization headers)
		config.setAllowCredentials(true);

		// Apply this configuration to all paths
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}
}
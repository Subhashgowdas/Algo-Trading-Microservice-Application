package com.algotrade.broker_service.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import com.algotrade.broker_service.security.JwtAuthenticationFilter;

/**
 *
 * Design choices:
 * - CSRF is disabled because we use stateless JWT tokens (no cookies).
 * - Sessions are stateless – no HttpSession is created.
 * - All endpoints require authentication by default.
 *   We will add specific public endpoints later if needed.
 * - The JwtAuthenticationFilter is executed before Spring’s built‑in
 *   UsernamePasswordAuthenticationFilter.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final CorsConfigurationSource corsConfigurationSource;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
			CorsConfigurationSource corsConfigurationSource) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.corsConfigurationSource = corsConfigurationSource;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		// 1. Enable CORS with our custom configuration
		.cors(cors -> cors.configurationSource(corsConfigurationSource))

		// 2. Disable CSRF – APIs are stateless, no browser forms
		.csrf(AbstractHttpConfigurer::disable)
		
//		.formLogin(AbstractHttpConfigurer::disable)
//		.httpBasic(AbstractHttpConfigurer::disable)
		
		// 3. Never create an HTTP session
		.sessionManagement(session ->
		session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

		// 4. Authorization rules – every request must be authenticated
		.authorizeHttpRequests(auth -> auth
				.anyRequest().authenticated()
				)

		// 5. Insert our JWT filter before Spring’s default filter
		.addFilterBefore(jwtAuthenticationFilter,
				UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
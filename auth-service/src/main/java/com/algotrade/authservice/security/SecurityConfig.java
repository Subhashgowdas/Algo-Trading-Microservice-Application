package com.algotrade.authservice.security;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	
	private final InternalServiceTokenFilter internalServiceTokenFilter;

	// Update constructor to include this filter

	// Read allowed origin from properties (fallback to localhost:3000)
	@Value("${app.cors.allowed-origins:http://localhost:3000}")
	private String allowedOrigins;

	public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, InternalServiceTokenFilter internalServiceTokenFilter) {
		this.jwtAuthenticationFilter = jwtAuthenticationFilter;
		this.internalServiceTokenFilter = internalServiceTokenFilter;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
		.cors(cors -> cors.configurationSource(corsConfigurationSource()))
		.csrf(AbstractHttpConfigurer::disable)
		.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
		.authorizeHttpRequests(auth -> auth
				.requestMatchers("/api/auth/login", "/api/auth/register/**", "/api/auth/refresh",
						"/api/auth/forgot-password", "/api/auth/reset-password","/api/internal/**").permitAll()
				.requestMatchers("/api/auth/logout", "/api/auth/logout-all").authenticated()
				.requestMatchers("/api/broker/credentials/**").authenticated()
//				.requestMatchers("/api/internal/**").hasRole("INTERNAL") 
				.anyRequest().authenticated()
				)
		.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
		.addFilterBefore(internalServiceTokenFilter, JwtAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		// Support comma-separated list in properties
		configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
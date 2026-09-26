package com.algotrade.authservice.security;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.algotrade.authservice.service.jwt.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {
		// 1. Extract the Authorization header
		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (authHeader == null || !authHeader.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authHeader.substring(7);
		try {
			// 2. Validate token and extract claims
			Jws<Claims> claimsJws = jwtService.validateToken(token);
			String email = claimsJws.getPayload().getSubject();

			List<?> rawRoles = claimsJws.getPayload().get("roles", List.class);

			// 3. Build Spring Security authentication object
			List<SimpleGrantedAuthority> authorities = Optional.ofNullable(rawRoles)
					.orElse(Collections.emptyList())
					.stream()
					.filter(String.class::isInstance)
					.map(role -> new SimpleGrantedAuthority((String) role))
					.toList();

			UsernamePasswordAuthenticationToken authentication =
					new UsernamePasswordAuthenticationToken(email, null, authorities);
			authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

			// 4. Set the security context
			SecurityContextHolder.getContext().setAuthentication(authentication);

		} catch (JwtException ex) {
			log.warn("Invalid JWT token: {}", ex.getMessage());
			// Simply don't set authentication – the request will be treated as unauthenticated
		}

		filterChain.doFilter(request, response);
	}
}
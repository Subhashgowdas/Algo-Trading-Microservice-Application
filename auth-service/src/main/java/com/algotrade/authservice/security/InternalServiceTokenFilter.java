package com.algotrade.authservice.security;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.algotrade.authservice.service.jwt.JwtService;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class InternalServiceTokenFilter extends OncePerRequestFilter {

	private static final Logger logger = LoggerFactory.getLogger(InternalServiceTokenFilter.class);
	private final JwtService jwtService;

	public InternalServiceTokenFilter(JwtService jwtService) {
		super();
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {


		if(!request.getRequestURI().startsWith("/api/internals")) {
			filterChain.doFilter(request, response); return ; }

		String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
		if(authHeader == null || !authHeader.startsWith("Bearer ")) {
			response.sendError(HttpServletResponse.
					SC_UNAUTHORIZED,"Missing Service Token"); return ; }

		String Token = authHeader.substring(7); try { Jws<Claims> claimsJws =
				jwtService.validateToken(Token); Claims claims = claimsJws.getPayload();


				if(!"broker-service".equals(claims.getSubject()) ||
						!Boolean.TRUE.equals(claims.get("service",Boolean.class))) {
					response.sendError(HttpServletResponse.
							SC_FORBIDDEN,"Invalid Service token claims"); return; }

				UsernamePasswordAuthenticationToken auth = new
						UsernamePasswordAuthenticationToken( claims.getSubject(),null, List.of(new
								SimpleGrantedAuthority("ROLE_INTERNAL")));

				SecurityContextHolder.getContext().setAuthentication(auth); }catch (Exception
						e) { logger.warn("Invalid Service Token: {}",e.getMessage());
						response.sendError(HttpServletResponse.
								SC_UNAUTHORIZED,"Invalid Service Token"); }

		filterChain.doFilter(request, response);

	}
}

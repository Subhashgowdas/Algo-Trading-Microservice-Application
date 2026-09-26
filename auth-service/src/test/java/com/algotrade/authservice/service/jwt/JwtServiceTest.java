package com.algotrade.authservice.service.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.algotrade.authservice.auth.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;

public class JwtServiceTest {
	
	private JwtService jwtService;
	
	@BeforeEach
	void setUp() {
		JwtProperties props =  new JwtProperties();
		props.setAccessTokenExpirationSeconds(10);
		props.setRefreshTokenByteLength(16);
		props.setRefreshTokenLifetimeSeconds(60);
		props.setIssuer("test-issuer");
	}
	
	@Test
	@DisplayName("Generate access token and extract claims")
	void shouldGeneratedAndValidateAccessToken() {
		
		String token = jwtService.generateAccessToken("test@example.com", List.of("ROLE_USER"));
		
		assertNotNull(token);
		Jws<Claims> claimsJws = jwtService.validateToken(token);
		
		assertThat(claimsJws.getPayload().getSubject()).isEqualTo("test@example.com");
		assertThat(claimsJws.getPayload().get("roles",List.class)).containsExactly("ROLE_USER");
		assertThat(claimsJws.getPayload().getIssuer()).isEqualTo("test-issuer");
	}
	
	@Test
	@DisplayName("Access token with invalid signature should throw exception")
	void shouldRejectTokenWithWrongSignature() {
		
		JwtService otherService = null;
		String token = otherService.generateAccessToken("hacker@example.com", List.of("ROLE_ADMIN"));
		assertThrows(JwtException.class, () -> jwtService.validateToken(token));
	}
	
	@Test
	@DisplayName("Expired class token should throw exception")
	void shouldRejectExpiredToken() throws InterruptedException{
		String Token = jwtService.generateAccessToken("user@example.com", List.of("ROLE_USER"));
		Thread.sleep(11_000);
		assertThrows(JwtException.class, () -> jwtService.validateToken(Token));
	}
	
	@Test
	@DisplayName("Refersh token should be random Base64URL String")
	void shouldGenerateRandomRefreshToken() {
		String token1 = jwtService.generateRefreshToken();
		String token2 = jwtService.generateRefreshToken();
		
		assertNotNull(token1);
		assertNotNull(token2);
		assertNotEquals(token1, token2);
		
		assertFalse(token1.contains("="));
		
	}
	
	@Test
	@DisplayName("Extract email and roles from valid token")
	void shouldExctractClaims() {
		String token = jwtService.generateAccessToken("my@email.com", List.of("ROLE_ADMIN","ROLE_TRADER"));
		assertEquals("my@email.com", jwtService.getEmailFromToken(token));
		List<String> roles = jwtService.getRolesFromToken(token);
		assertThat(roles).containsExactlyInAnyOrder("ROLE_ADMIN","ROLE_TRADER");
	}	
}

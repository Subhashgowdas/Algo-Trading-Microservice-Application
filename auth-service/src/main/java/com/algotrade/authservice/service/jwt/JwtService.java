package com.algotrade.authservice.service.jwt;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import com.algotrade.authservice.auth.config.JwtProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

@Service
public class JwtService {

	private static final Logger log = LoggerFactory.getLogger(JwtService.class);
	private static final String RSA_ALGORITHM = "RSA";

	private final PrivateKey privateKey;
	private final PublicKey publicKey;
	private final JwtProperties jwtProperties;
	private final SecureRandom secureRandom;

	public JwtService(
			JwtProperties jwtProperties,
			@Value("${jwt.private-key-path}") Resource privateKeyResource,
			@Value("${jwt.public-key-path}") Resource publicKeyResource) {

		this.jwtProperties = jwtProperties;

		// 1. Initialize SecureRandom once (computationally expensive to do per request)
		this.secureRandom = new SecureRandom();

		// 2. Fail-fast initialization
		try {
			this.privateKey = loadPrivateKey(privateKeyResource);
			this.publicKey = loadPublicKey(publicKeyResource);
			log.info("JWT RSA keys successfully loaded into memory.");
		} catch (Exception ex) {
			log.error("CRITICAL: Failed to load RSA keys for JWT signing/verification. Check file paths and formats.", ex);
			// Throwing BeanInitializationException prevents the app from starting in a broken state
			throw new BeanInitializationException("Failed to initialize JwtService RSA keys", ex);
		}
	}

	private PrivateKey loadPrivateKey(Resource resource) throws Exception {
		String key = readResourceAndStripPemHeaders(resource, "PRIVATE KEY");
		byte[] keyBytes = Base64.getDecoder().decode(key);
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		return KeyFactory.getInstance(RSA_ALGORITHM).generatePrivate(spec);
	}

	private PublicKey loadPublicKey(Resource resource) throws Exception {
		String key = readResourceAndStripPemHeaders(resource, "PUBLIC KEY");
		byte[] keyBytes = Base64.getDecoder().decode(key);
		X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
		return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(spec);
	}

	private String readResourceAndStripPemHeaders(Resource resource, String keyType) throws Exception {
		// Try-with-resources prevents memory/file-handle leaks
		try (InputStream is = resource.getInputStream()) {
			String pem = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			return pem
					.replaceAll("-----BEGIN " + keyType + "-----", "")
					.replaceAll("-----END " + keyType + "-----", "")
					.replaceAll("\\s", ""); // Strips all newlines and whitespaces safely
		}
	}

	/**
	 * Generate a signed JWT access token for the given user details.
	 */

	public String generateAccessToken(String email, Collection<String> roles) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + (jwtProperties.getAccessTokenExpirationSeconds() * 1000L));

		return Jwts.builder()
				.issuer(jwtProperties.getIssuer())
				.subject(email)
				.claim("roles", roles)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(privateKey) // JJWT automatically applies RS256 based on the PrivateKey type
				.compact();
	}

	/**
	 * Generate a cryptographically strong random refresh token (opaque string).
	 * This is NOT a JWT; it's a random token stored in the database.
	 */

	public String generateRefreshToken() {
		byte[] randomBytes = new byte[jwtProperties.getRefreshTokenByteLength()];
		secureRandom.nextBytes(randomBytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
	}

	/**
	 * Validate an access token and return its claims if valid.
	 * Throws JwtException on any failure (expired, malformed, invalid signature).
	 */

	public Jws<Claims> validateToken(String token) throws JwtException {
		return Jwts.parser()
				.verifyWith(publicKey)
				.build()
				.parseSignedClaims(token);
	}

	/**
	 * Extract the email from a valid access token.
	 */

	public String getEmailFromToken(String token) {
		return validateToken(token).getPayload().getSubject();
	}

	/**
	 * Extract roles from a valid access token.
	 */

	@SuppressWarnings("unchecked")
	public List<String> getRolesFromToken(String token) {
		Claims claims = validateToken(token).getPayload();
		return claims.get("roles", List.class);
	}

	public String generateServiceToken(String serviceName) {
		Date now = new Date();
		// Extract magic numbers into readable variables. 
		// L ensures long multiplication to prevent integer overflow.
		long tenYearsInMillis = 3650L * 24 * 60 * 60 * 1000; 
		Date expiry = new Date(now.getTime() + tenYearsInMillis);

		return Jwts.builder()
				.issuer(jwtProperties.getIssuer())
				.subject(serviceName)
				.claim("service", true)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(privateKey)
				.compact();
	}
}














package com.algotrade.authservice.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
	/** Path to the RSA private key (PEM format) for signing tokens */
	private String privateKeyPath = "classpath:keys/private.pem";

	/** Path to the RSA public key (PEM format) for verifying tokens */
	private String publicKeyPath = "classpath:keys/public.pem";

	/** Token issuer */
	private String issuer = "algotrade-auth-service";

	/** Access token expiration in seconds (default 15 minutes) */
	private long accessTokenExpirationSeconds = 900;

	/** Refresh token length in bytes (raw random bytes, hex-encoded) */
	private int refreshTokenByteLength = 32;

	/** Refresh token absolute lifetime in seconds (default 7 days) */
	private long refreshTokenLifetimeSeconds = 604800;
}
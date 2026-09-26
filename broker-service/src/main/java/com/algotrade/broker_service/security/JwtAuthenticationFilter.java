package com.algotrade.broker_service.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Production-grade stateless JWT authentication filter.
 * Optimized for memory efficiency, thread-safety, and fail-fast startup behavior.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    // Extracted constants to prevent repeated memory allocation
    private static final String RSA_ALGORITHM = "RSA";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;

    private final PublicKey publicKey;
    private final String issuer;

    public JwtAuthenticationFilter(
            @Value("${app.jwt.public-key-path}") Resource publicKeyResource,
            @Value("${app.jwt.issuer}") String issuer) {

        this.issuer = issuer;

        // Fail-fast initialization prevents the application from booting up in a broken state
        try {
            this.publicKey = loadPublicKey(publicKeyResource);
            log.info("JWT Public Key successfully loaded into memory.");
        } catch (Exception ex) {
            log.error("CRITICAL: Failed to load RSA Public Key for JWT verification. Verify file path and format.", ex);
            throw new BeanInitializationException("Failed to initialize JwtAuthenticationFilter public key", ex);
        }
    }

    /**
     * Safely loads the public key using try-with-resources to prevent memory leaks.
     */
    private PublicKey loadPublicKey(Resource resource) throws Exception {
        try (InputStream is = resource.getInputStream()) {
            String pemContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            String cleanedPem = pemContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", ""); // Safe removal of all spaces and newlines

            byte[] keyBytes = Base64.getDecoder().decode(cleanedPem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance(RSA_ALGORITHM).generatePublic(spec);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. Extract the Authorization header safely
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract the raw token string safely
        String token = authHeader.substring(BEARER_PREFIX_LENGTH).trim();

        try {
            // 3. Cryptographically validate the token (signature, issuer, expiration)
            Jws<Claims> claimsJws = Jwts.parser()
                    .verifyWith(publicKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = claimsJws.getPayload();

            // 4. Extract the user's identity
            String email = claims.getSubject();
            
            // 5. DEFENSIVE EXTRACTION: This directly fixes the NullPointerException
            List<?> rawRoles = claims.get("roles", List.class);
            
            List<SimpleGrantedAuthority> authorities = Optional.ofNullable(rawRoles)
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(String.class::isInstance) // Prevents ClassCastExceptions from malicious payloads
                    .map(role -> new SimpleGrantedAuthority((String) role))
                    .toList();

            // 6. Build the authentication context
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(email, null, authorities);
            
            // Attach web details (IP, session context) to the authentication object (Best Practice)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // 7. Set the authentication in the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

        } catch (JwtException ex) {
            // Token validation failed. Log warning and keep security context empty.
            log.warn("Invalid JWT token presented: {}", ex.getMessage());
        }

        // 8. Proceed down the filter chain
        filterChain.doFilter(request, response);
    }
}
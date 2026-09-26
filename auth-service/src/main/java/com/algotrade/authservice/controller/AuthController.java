package com.algotrade.authservice.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.algotrade.authservice.auth.config.JwtProperties;
import com.algotrade.authservice.auth.config.OtpProperties;
import com.algotrade.authservice.dto.AuthResponse;
import com.algotrade.authservice.dto.ForgotPasswordRequest;
import com.algotrade.authservice.dto.LoginRequest;
import com.algotrade.authservice.dto.RegisterCompleteRequest;
import com.algotrade.authservice.dto.RegisterInitiateRequest;
import com.algotrade.authservice.dto.ResetPasswordRequest;
import com.algotrade.authservice.exception.EmailAlreadyExistsException;
import com.algotrade.authservice.exception.InvalidOtpException;
import com.algotrade.authservice.exception.InvalidRefreshTokenException;
import com.algotrade.authservice.model.EntityStatus;
import com.algotrade.authservice.model.RefreshToken;
import com.algotrade.authservice.model.User;
import com.algotrade.authservice.repository.UserRepository;
import com.algotrade.authservice.service.jwt.JwtService;
import com.algotrade.authservice.service.otp.OtpService;
import com.algotrade.authservice.service.password.PasswordStrengthService;
import com.algotrade.authservice.service.token.RefreshTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final OtpService otpService;
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final PasswordStrengthService passwordStrengthService;
	private final JwtService jwtService;
	private final RefreshTokenService refreshTokenService;
	private final JwtProperties jwtProperties;
	private final OtpProperties otpProperties;

	public AuthController(OtpService otpService,
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			PasswordStrengthService passwordStrengthService,
			JwtService jwtService,
			RefreshTokenService refreshTokenService,
			JwtProperties jwtProperties,
			OtpProperties otpProperties) {
		this.otpService = otpService;
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.passwordStrengthService = passwordStrengthService;
		this.jwtService = jwtService;
		this.refreshTokenService = refreshTokenService;
		this.jwtProperties = jwtProperties;
		this.otpProperties = otpProperties;
	}

	// --- Registration endpoints (unchanged) ---
	@PostMapping("/register/initiate")
	public ResponseEntity<Map<String, Object>> initiateRegistration(@Valid @RequestBody RegisterInitiateRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailAlreadyExistsException("Email already registered");
		}
		otpService.sendOtp(request.email());
		long expiryMillis = System.currentTimeMillis() + otpProperties.getTtlSeconds() * 1000L;
		return ResponseEntity.ok(Map.of(
				"message", "OTP sent to email",
				"otpExpiryTimestamp", expiryMillis,
				"resendCooldownSeconds", otpProperties.getResendDelaySeconds()
				));
	}

	@PostMapping("/register/complete")
	public ResponseEntity<Map<String, String>> completeRegistration(@Valid @RequestBody RegisterCompleteRequest request) {

		if (!passwordStrengthService.isStrong(request.password())) {
			throw new IllegalArgumentException("Password is too weak");
		}

		if (!otpService.verifyOtp(request.email(), request.otp())) {
			throw new InvalidOtpException("Invalid or expired OTP");
		}

		User user = User.builder()
				.email(request.email())
				.password(passwordEncoder.encode(request.password()))
				.role("ROLE_USER")
				.build();
		userRepository.save(user);

		return ResponseEntity.ok(Map.of("message", "Registration successful"));
	}


	@PostMapping("/login")
	public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
			HttpServletResponse response) {
		// 1. Find user by email
		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

		// 2. Verify password
		if (!passwordEncoder.matches(request.password(), user.getPassword())) {
			throw new BadCredentialsException("Invalid email or password");
		}

		// 3. Generate tokens
		String accessToken = jwtService.generateAccessToken(user.getEmail(), List.of(user.getRole()));
		String refreshToken = refreshTokenService.createRefreshToken(user);

		// 4. Set refresh token in HttpOnly cookie
		ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
				.httpOnly(true)
				.secure(false)                // set true in production (HTTPS)
				.sameSite("Strict")
				.path("/api/auth")            // cookie sent only to auth endpoints
				.maxAge(jwtProperties.getRefreshTokenLifetimeSeconds())
				.build();
		response.addHeader("Set-Cookie", refreshCookie.toString());

		// 5. Return access token in JSON body
		return ResponseEntity.ok(new AuthResponse(accessToken, null, "Bearer"));
	}

	@PostMapping("/forgot-password")
	public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
		boolean exists = userRepository.existsByEmailAndStatus(request.email(), EntityStatus.ACTV);

		if (!exists) {
			return ResponseEntity.badRequest()
					.body(Map.of("message", "Email not registered"));
		}

		otpService.sendResetToken(request.email());
		return ResponseEntity.ok(Map.of("message", "Reset OTP has been sent to your email"));
	}

	@PostMapping("/reset-password")
	public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {

		// Check password strength
		if (!passwordStrengthService.isStrong(request.newPassword())) {
			throw new IllegalArgumentException("Password is too weak");
		}

		// Verify reset token
		if (!otpService.verifyResetToken(request.email(), request.token())) {
			throw new InvalidOtpException("Invalid or expired reset OTP");
		}

		User user = userRepository.findByEmail(request.email())
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// Update password
		user.setPassword(passwordEncoder.encode(request.newPassword()));
		userRepository.save(user);

		// Revoke all existing refresh tokens for this user (force logout)
		refreshTokenService.revokeAllUserTokens(user);

		return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
	}


	@PostMapping("/logout")
	public ResponseEntity<Map<String, String>> logout(HttpServletRequest request,
			HttpServletResponse response) {
		String refreshTokenValue = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refresh_token".equals(cookie.getName())) {
					refreshTokenValue = cookie.getValue();
					break;
				}
			}
		}

		if (refreshTokenValue != null) {
			RefreshToken token = refreshTokenService.verifyRefreshToken(refreshTokenValue)
					.orElseThrow(() -> new InvalidRefreshTokenException("Invalid refresh token"));
			refreshTokenService.revokeRefreshToken(token);
		}

		// Clear the cookie
		ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
				.httpOnly(true)
				.secure(false)
				.sameSite("Strict")
				.path("/api/auth")
				.maxAge(0)
				.build();
		response.addHeader("Set-Cookie", clearCookie.toString());

		return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
	}

	@PostMapping("/logout-all")
	public ResponseEntity<Map<String, String>> logoutAll(HttpServletRequest request,
			HttpServletResponse response) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String email = (String) auth.getPrincipal();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new RuntimeException("User not found"));
		refreshTokenService.revokeAllUserTokens(user);

		// Clear the cookie
		ResponseCookie clearCookie = ResponseCookie.from("refresh_token", "")
				.httpOnly(true)
				.secure(false)
				.sameSite("Strict")
				.path("/api/auth")
				.maxAge(0)
				.build();
		response.addHeader("Set-Cookie", clearCookie.toString());

		return ResponseEntity.ok(Map.of("message", "All sessions have been revoked"));
	}

	@PostMapping("/refresh")
	public ResponseEntity<AuthResponse> refreshToken(HttpServletRequest request,
			HttpServletResponse response) {
		// 1. Extract refresh token from cookie
		String refreshTokenValue = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if ("refresh_token".equals(cookie.getName())) {
					refreshTokenValue = cookie.getValue();
					break;
				}
			}
		}
		if (refreshTokenValue == null) {
			throw new InvalidRefreshTokenException("Refresh OTP not provided");
		}

		// 2. Verify the raw refresh token
		RefreshToken token = refreshTokenService.verifyRefreshToken(refreshTokenValue)
				.orElseThrow(() -> new InvalidRefreshTokenException("Invalid or expired refresh OTP"));

		// 3. Get the associated user
		User user = token.getUser();

		// 4. Revoke the old refresh token (rotation)
		refreshTokenService.revokeRefreshToken(token);

		// 5. Issue new tokens
		String accessToken = jwtService.generateAccessToken(user.getEmail(), List.of(user.getRole()));
		String newRefreshToken = refreshTokenService.createRefreshToken(user);

		// 6. Set new refresh token in cookie
		ResponseCookie newRefreshCookie = ResponseCookie.from("refresh_token", newRefreshToken)
				.httpOnly(true)
				.secure(false)
				.sameSite("Strict")
				.path("/api/auth")
				.maxAge(jwtProperties.getRefreshTokenLifetimeSeconds())
				.build();
		response.addHeader("Set-Cookie", newRefreshCookie.toString());

		return ResponseEntity.ok(new AuthResponse(accessToken, null, "Bearer"));
	}

}
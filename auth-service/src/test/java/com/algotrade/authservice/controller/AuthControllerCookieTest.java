package com.algotrade.authservice.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import com.algotrade.authservice.auth.config.JwtProperties;
import com.algotrade.authservice.dto.LoginRequest;
import com.algotrade.authservice.model.RefreshToken;
import com.algotrade.authservice.model.User;
import com.algotrade.authservice.repository.UserRepository;
import com.algotrade.authservice.service.jwt.JwtService;
import com.algotrade.authservice.service.otp.OtpService;
import com.algotrade.authservice.service.password.PasswordStrengthService;
import com.algotrade.authservice.service.token.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;

@WebMvcTest(AuthController.class)
class AuthControllerCookieTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private OtpService otpService;
    @MockBean private UserRepository userRepository;
    @MockBean private PasswordEncoder passwordEncoder;
    @MockBean private PasswordStrengthService passwordStrengthService;
    @MockBean private JwtService jwtService;
    @MockBean private RefreshTokenService refreshTokenService;
    @MockBean private JwtProperties jwtProperties;   // needed for cookie maxAge

    @Autowired
    private ObjectMapper objectMapper;

    private User user;
    private final String accessToken = "mockAccessToken";
    private final String refreshTokenValue = "mockRefreshTokenValue";
    private final long refreshTokenLifetime = 604800L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user@example.com")
                .password("hashedPassword")
                .role("ROLE_USER")
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), eq(user.getPassword()))).thenReturn(true);
        when(jwtService.generateAccessToken(user.getEmail(), List.of(user.getRole()))).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(user)).thenReturn(refreshTokenValue);
        when(jwtProperties.getRefreshTokenLifetimeSeconds()).thenReturn(refreshTokenLifetime);
    }

    @Test
    @DisplayName("Login should set refresh_token cookie and return access token")
    void loginShouldSetCookieAndReturnAccessToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("user@example.com", "password");

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().path("refresh_token", "/api/auth"))
                .andExpect(cookie().maxAge("refresh_token", (int) refreshTokenLifetime))
                .andExpect(jsonPath("$.accessToken").value(accessToken))
                .andExpect(jsonPath("$.refreshToken").isEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Refresh should read cookie, rotate tokens, and set new cookie")
    void refreshShouldRotateTokensViaCookie() throws Exception {
        RefreshToken oldToken = RefreshToken.builder()
                .tokenHash("hashed")
                .user(user)
                .revoked(false)
                .expiryDate(java.time.Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenService.verifyRefreshToken(refreshTokenValue)).thenReturn(Optional.of(oldToken));
        when(refreshTokenService.createRefreshToken(user)).thenReturn("newRefreshTokenValue");
        when(jwtService.generateAccessToken(user.getEmail(), List.of("ROLE_USER"))).thenReturn("newAccessToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", refreshTokenValue)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().httpOnly("refresh_token", true))
                .andExpect(cookie().value("refresh_token", "newRefreshTokenValue"))
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("Refresh without cookie should return 401")
    void refreshWithoutCookieShouldReturn401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Logout should read cookie, revoke token, and clear cookie")
    void logoutShouldRevokeAndClearCookie() throws Exception {
        RefreshToken token = RefreshToken.builder()
                .tokenHash("hashed")
                .user(user)
                .revoked(false)
                .expiryDate(java.time.Instant.now().plusSeconds(3600))
                .build();

        when(refreshTokenService.verifyRefreshToken(refreshTokenValue)).thenReturn(Optional.of(token));

        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf())
                        .cookie(new Cookie("refresh_token", refreshTokenValue)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().maxAge("refresh_token", 0))   // cleared
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(refreshTokenService).revokeRefreshToken(token);
    }

    @Test
    @DisplayName("Logout without cookie should still return 200 and clear cookie")
    void logoutWithoutCookieShouldStillSucceed() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().maxAge("refresh_token", 0));
    }

    @Test
    @DisplayName("Logout-all should revoke all tokens and clear cookie (authenticated)")
    void logoutAllShouldRevokeAllAndClearCookie() throws Exception {
        // Simulate authenticated user
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        mockMvc.perform(post("/api/auth/logout-all")
                        .with(csrf())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().maxAge("refresh_token", 0))
                .andExpect(jsonPath("$.message").value("All sessions have been revoked"));

        verify(refreshTokenService).revokeAllUserTokens(user);
    }
}
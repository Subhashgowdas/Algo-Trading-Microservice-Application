package com.algotrade.authservice.service.password;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.algotrade.authservice.controller.AuthController;
import com.algotrade.authservice.dto.ForgotPasswordRequest;
import com.algotrade.authservice.dto.ResetPasswordRequest;
import com.algotrade.authservice.model.User;
import com.algotrade.authservice.repository.UserRepository;
import com.algotrade.authservice.service.otp.OtpService;
import com.algotrade.authservice.service.token.RefreshTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
class AuthControllerPasswordResetTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OtpService otpService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private PasswordStrengthService passwordStrengthService;
    @MockBean
    private RefreshTokenService refreshTokenService;
    @MockBean
    private com.algotrade.authservice.service.jwt.JwtService jwtService; // needed for bean creation but not used
    @MockBean
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void forgotPassword_UserExists_SendsToken() throws Exception {
        when(userRepository.findByEmail("user@example.com"))
                .thenReturn(Optional.of(new User()));

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the email is registered, a password reset token has been sent."));

        verify(otpService).sendResetToken("user@example.com");
    }

    @Test
    void forgotPassword_UserNotExists_DoesNotSendToken() throws Exception {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/auth/forgot-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ForgotPasswordRequest("unknown@example.com"))))
                .andExpect(status().isOk());

        verify(otpService, never()).sendResetToken(anyString());
    }

    @Test
    void resetPassword_ValidToken_ResetsPassword() throws Exception {
        User user = User.builder().email("user@example.com").password("oldhash").role("ROLE_USER").build();
        when(otpService.verifyResetToken("user@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordStrengthService.isStrong("NewStrong!1")).thenReturn(true);
        when(passwordEncoder.encode("NewStrong!1")).thenReturn("newhash");

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(
                                "user@example.com", "123456", "NewStrong!1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password has been reset successfully."));

        verify(passwordEncoder).encode("NewStrong!1");
        verify(userRepository).save(user);
        verify(refreshTokenService).revokeAllUserTokens(user);
    }

    @Test
    void resetPassword_InvalidToken_ThrowsException() throws Exception {
        when(otpService.verifyResetToken("user@example.com", "wrong")).thenReturn(false);

        mockMvc.perform(post("/api/auth/reset-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest(
                                "user@example.com", "wrong", "NewStrong!1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
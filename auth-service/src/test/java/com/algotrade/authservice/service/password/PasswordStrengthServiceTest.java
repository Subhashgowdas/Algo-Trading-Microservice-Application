package com.algotrade.authservice.service.password;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordStrengthServiceTest {

    private PasswordStrengthService service;

//    @BeforeEach
//    void setUp() {
//        service = new PasswordStrengthService();
//    }

    @Test
    @DisplayName("Weak password should return false")
    void shouldRejectWeakPasswords() {
    	assertFalse(service.isStrong("password"));
        assertFalse(service.isStrong("12345678"));
        assertFalse(service.isStrong("qwertyui"));
    }

    @Test
    @DisplayName("Strong password should return true")
    void shouldAcceptStrongPasswords() {
        assertTrue(service.isStrong("Str0ng!P@ssw0rd"));
        assertTrue(service.isStrong("MyC0mpl3x!Password"));
    }

    @Test
    @DisplayName("Null or blank password should return false")
    void shouldRejectNullOrBlank() {
        assertFalse(service.isStrong(null));
        assertFalse(service.isStrong(""));
        assertFalse(service.isStrong("   "));
    }

    @Test
    @DisplayName("Feedback should return a warning message for weak passwords")
    void shouldProvideFeedback() {
        String feedback = service.getFeedback("password");
        assertNotNull(feedback);
        assertFalse(feedback.isBlank());
        // The exact message depends on zxcvbn, but it should not be empty
        // For a strong password, feedback could still have a generic message if no specific warning
        // However, usually zxcvbn provides some feedback for weak passwords.
        // We just check that a non‑null string is returned.
    }
}
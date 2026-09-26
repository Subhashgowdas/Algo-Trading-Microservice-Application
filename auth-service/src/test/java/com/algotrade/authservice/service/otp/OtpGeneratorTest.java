package com.algotrade.authservice.service.otp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtpGeneratorTest {

    private OtpGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new OtpGenerator();
    }

    @Test
    @DisplayName("Should generate OTP of exact length")
    void shouldGenerateOtpOfRequestedLength() {
        String otp = generator.generate(6);
        assertNotNull(otp);
        assertEquals(6, otp.length());
    }

    @Test
    @DisplayName("Should generate numeric OTP")
    void shouldGenerateNumericOtp() {
        String otp = generator.generate(8);
        assertTrue(otp.matches("\\d+"), "OTP must be numeric");
    }

    @Test
    @DisplayName("Should generate zero-padded OTP when length > significant digits")
    void shouldZeroPadWhenNeeded() {
        // Very rarely, random might produce a small number; the method must format it with leading zeros.
        // Generate many OTPs and verify each has the exact length and numeric format.
        for (int i = 0; i < 100; i++) {
            String otp = generator.generate(4);
            assertEquals(4, otp.length());
            assertTrue(otp.matches("\\d{4}"), "OTP must be exactly 4 digits");
        }
    }

    @Test
    @DisplayName("Should throw exception for non-positive length")
    void shouldRejectInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(0));
        assertThrows(IllegalArgumentException.class, () -> generator.generate(-1));
    }

    @Test
    @DisplayName("Multiple OTPs should be reasonably random")
    void shouldBeRandom() {
        String otp1 = generator.generate(6);
        String otp2 = generator.generate(6);
        // It is possible (but astronomically unlikely) for two OTPs to be equal;
        // still, this is a sanity check.
        assertNotEquals(otp1, otp2);
    }
}
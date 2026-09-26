package com.algotrade.authservice.dto;

import java.time.LocalDateTime;

public record UserProfileResponse(
        String email,
        String role,
        LocalDateTime createdAt
) {}

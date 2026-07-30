package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.Role;

/** Issued tokens and their metadata: a short-lived access token plus a longer-lived refresh token. */
public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        String username,
        Role role,
        long accessExpiresInSeconds
) {
    public static AuthResponse of(String accessToken, String refreshToken, String username, Role role,
                                  long accessExpiresInSeconds) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", username, role, accessExpiresInSeconds);
    }
}

package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.NotBlank;

/** Exchange a refresh token for a new access token (and a rotated refresh token), or revoke it on logout. */
public record RefreshRequest(
        @NotBlank String refreshToken
) {
}

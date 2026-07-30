package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.NotBlank;

/** Credentials for token issuance. */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}

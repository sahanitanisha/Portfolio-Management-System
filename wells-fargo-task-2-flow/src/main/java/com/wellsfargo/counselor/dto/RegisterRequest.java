package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Self-service registration. New accounts are always created with the read-only VIEWER role. */
public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 64) String username,
        @NotBlank @Size(min = 8, max = 100) String password
) {
}

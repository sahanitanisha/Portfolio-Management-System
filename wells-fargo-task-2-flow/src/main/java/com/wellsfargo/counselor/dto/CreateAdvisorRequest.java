package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Inbound payload to register a new advisor. Validated at the controller boundary via {@code @Valid}. */
public record CreateAdvisorRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String address,
        @NotBlank @Pattern(regexp = "^\\+?[0-9 .()-]{7,20}$", message = "must be a valid phone number") String phone,
        @NotBlank @Email String email
) {
}

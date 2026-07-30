package com.wellsfargo.counselor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/** Inbound payload to register a new client under an advisor. */
public record CreateCustomerRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @Pattern(regexp = "^\\+?[0-9 .()-]{7,20}$", message = "must be a valid phone number") String phoneNumber,
        String address,
        @Past LocalDate dateOfBirth,
        @NotNull Long advisorId
) {
}

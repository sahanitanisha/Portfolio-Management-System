package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.Advisor;

/** Outbound representation of an advisor. Scalar fields only - no lazy associations are touched. */
public record AdvisorResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String address
) {
    public static AdvisorResponse from(Advisor advisor) {
        return new AdvisorResponse(
                advisor.getId(),
                advisor.getFirstName(),
                advisor.getLastName(),
                advisor.getEmail(),
                advisor.getPhone(),
                advisor.getAddress());
    }
}

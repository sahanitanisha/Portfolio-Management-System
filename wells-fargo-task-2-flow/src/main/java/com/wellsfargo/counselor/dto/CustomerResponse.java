package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.Customer;

import java.time.LocalDate;

/** Outbound representation of a client. {@code advisorId} reads the lazy proxy's id without a query. */
public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String address,
        LocalDate dateOfBirth,
        Long advisorId
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                customer.getDateOfBirth(),
                customer.getAdvisor() != null ? customer.getAdvisor().getId() : null);
    }
}

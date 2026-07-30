package com.wellsfargo.counselor.dto;

import com.wellsfargo.counselor.entity.Account;
import com.wellsfargo.counselor.entity.AccountType;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Outbound representation of an account, with the {@link com.wellsfargo.counselor.entity.Money} balance flattened. */
public record AccountResponse(
        Long id,
        String accountNumber,
        AccountType accountType,
        BigDecimal balanceAmount,
        String balanceCurrency,
        BigDecimal interestRate,
        LocalDate openedOn,
        boolean active,
        Long customerId
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountType(),
                account.getBalance().getAmount(),
                account.getBalance().getCurrency(),
                account.getInterestRate(),
                account.getOpenedOn(),
                account.isActive(),
                account.getCustomer() != null ? account.getCustomer().getId() : null);
    }
}

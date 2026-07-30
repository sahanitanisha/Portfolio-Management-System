package com.wellsfargo.counselor.web;

import com.wellsfargo.counselor.dto.AccountResponse;
import com.wellsfargo.counselor.dto.OpenAccountRequest;
import com.wellsfargo.counselor.dto.PostTransactionRequest;
import com.wellsfargo.counselor.dto.TransactionResponse;
import com.wellsfargo.counselor.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/customers/{customerId}/accounts")
    public ResponseEntity<AccountResponse> openAccount(@PathVariable Long customerId,
                                                       @Valid @RequestBody OpenAccountRequest request) {
        AccountResponse created = accountService.openAccount(customerId, request);
        return ResponseEntity.created(URI.create("/api/v1/accounts/" + created.id())).body(created);
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @GetMapping("/customers/{customerId}/accounts")
    public Page<AccountResponse> listByCustomer(@PathVariable Long customerId, Pageable pageable) {
        return accountService.listByCustomer(customerId, pageable);
    }

    @PostMapping("/accounts/{accountId}/transactions")
    public ResponseEntity<TransactionResponse> postTransaction(@PathVariable Long accountId,
                                                               @Valid @RequestBody PostTransactionRequest request) {
        TransactionResponse posted = accountService.postTransaction(accountId, request);
        return ResponseEntity.created(URI.create("/api/v1/transactions/" + posted.id())).body(posted);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public Page<TransactionResponse> statement(@PathVariable Long accountId, Pageable pageable) {
        return accountService.statement(accountId, pageable);
    }
}

package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.AccountResponse;
import com.wellsfargo.counselor.dto.AdvisorResponse;
import com.wellsfargo.counselor.dto.CreateAdvisorRequest;
import com.wellsfargo.counselor.dto.CreateCustomerRequest;
import com.wellsfargo.counselor.dto.CustomerResponse;
import com.wellsfargo.counselor.dto.OpenAccountRequest;
import com.wellsfargo.counselor.dto.PostTransactionRequest;
import com.wellsfargo.counselor.dto.TransactionResponse;
import com.wellsfargo.counselor.entity.AccountType;
import com.wellsfargo.counselor.entity.TransactionType;
import com.wellsfargo.counselor.exception.InsufficientFundsException;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Service-slice test wiring the real services over an embedded database. Validates the
 * integrity-critical posting behaviour: exact balance tracking, overdraft rejection, idempotency,
 * and that JPA auditing is active.
 */
@DataJpaTest
@Import({AccountService.class, CustomerService.class, AdvisorService.class, JpaAuditingConfig.class})
class AccountServiceTest {

    @Autowired
    private AdvisorService advisorService;
    @Autowired
    private CustomerService customerService;
    @Autowired
    private AccountService accountService;
    @Autowired
    private AdvisorRepository advisorRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        AdvisorResponse advisor = advisorService.create(new CreateAdvisorRequest(
                "Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@wf.test"));
        CustomerResponse customer = customerService.create(new CreateCustomerRequest(
                "Grace", "Hopper", "grace@wf.test", "+1 555 0101", "2 Compiler Rd",
                LocalDate.of(1980, 1, 1), advisor.id()));
        AccountResponse account = accountService.openAccount(customer.id(), new OpenAccountRequest(
                "ACC-001", AccountType.CHECKING, new BigDecimal("0.0125"), "USD", null));
        accountId = account.id();
    }

    @Test
    void depositThenWithdrawTracksBalanceExactly() {
        accountService.postTransaction(accountId, new PostTransactionRequest(
                TransactionType.DEPOSIT, new BigDecimal("100.00"), "USD", "opening", "ref-1"));
        TransactionResponse withdrawal = accountService.postTransaction(accountId, new PostTransactionRequest(
                TransactionType.WITHDRAWAL, new BigDecimal("30.50"), "USD", "atm", "ref-2"));

        assertThat(withdrawal.balanceAfter()).isEqualByComparingTo("69.50");
        assertThat(accountService.getAccount(accountId).balanceAmount()).isEqualByComparingTo("69.50");
    }

    @Test
    void overdraftIsRejected() {
        assertThatThrownBy(() -> accountService.postTransaction(accountId, new PostTransactionRequest(
                TransactionType.WITHDRAWAL, new BigDecimal("1.00"), "USD", "overdraw", "ref-x")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    void postingIsIdempotentOnReferenceNumber() {
        PostTransactionRequest request = new PostTransactionRequest(
                TransactionType.DEPOSIT, new BigDecimal("40.00"), "USD", "dup", "ref-dup");
        TransactionResponse first = accountService.postTransaction(accountId, request);
        TransactionResponse replay = accountService.postTransaction(accountId, request);

        assertThat(replay.id()).isEqualTo(first.id());                 // same entry, not double-posted
        assertThat(accountService.getAccount(accountId).balanceAmount()).isEqualByComparingTo("40.00");
    }

    @Test
    void statementListsPostedEntriesNewestFirst() {
        accountService.postTransaction(accountId, new PostTransactionRequest(
                TransactionType.DEPOSIT, new BigDecimal("10.00"), "USD", "seed", "ref-seed"));
        Page<TransactionResponse> statement = accountService.statement(accountId, PageRequest.of(0, 10));

        assertThat(statement.getTotalElements()).isEqualTo(1);
        assertThat(statement.getContent().get(0).executedAt()).isNotNull();
    }

    @Test
    void jpaAuditingPopulatesCreatedMetadata() {
        var advisor = advisorRepository.findByEmail("ada@wf.test").orElseThrow();
        assertThat(advisor.getCreatedAt()).isNotNull();
        assertThat(advisor.getCreatedBy()).isEqualTo("system");
    }
}

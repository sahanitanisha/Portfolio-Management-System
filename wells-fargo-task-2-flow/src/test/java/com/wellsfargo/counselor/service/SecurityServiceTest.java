package com.wellsfargo.counselor.service;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.dto.CreateSecurityRequest;
import com.wellsfargo.counselor.dto.IngestPriceRequest;
import com.wellsfargo.counselor.dto.PriceQuoteResponse;
import com.wellsfargo.counselor.dto.SecurityResponse;
import com.wellsfargo.counselor.entity.SecurityType;
import com.wellsfargo.counselor.repository.PriceQuoteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Slice test for {@link SecurityService}: creation uniqueness and idempotent price ingestion. */
@DataJpaTest
@Import({SecurityService.class, JpaAuditingConfig.class})
class SecurityServiceTest {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private PriceQuoteRepository priceQuoteRepository;

    @Test
    void createsSecurityAndRejectsDuplicateSymbol() {
        SecurityResponse created = securityService.create(
                new CreateSecurityRequest("ACME", "Acme Corp", SecurityType.COMMON_STOCK, "USD", "NYSE", null));

        assertThat(created.id()).isNotNull();
        assertThat(created.symbol()).isEqualTo("ACME");
        assertThatThrownBy(() -> securityService.create(
                new CreateSecurityRequest("ACME", "Duplicate", SecurityType.ETF, "USD", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void priceIngestionIsIdempotentOnDate() {
        SecurityResponse security = securityService.create(
                new CreateSecurityRequest("ACME", "Acme Corp", SecurityType.COMMON_STOCK, "USD", "NYSE", null));

        PriceQuoteResponse first = securityService.ingestPrice(security.id(),
                new IngestPriceRequest(LocalDate.parse("2026-06-15"), new BigDecimal("130.00"), 1000L));
        PriceQuoteResponse repeat = securityService.ingestPrice(security.id(),
                new IngestPriceRequest(LocalDate.parse("2026-06-15"), new BigDecimal("999.00"), 5L));

        assertThat(repeat.id()).isEqualTo(first.id());            // same stored bar, not duplicated
        assertThat(repeat.close()).isEqualByComparingTo("130.00"); // original retained (immutable)
        assertThat(priceQuoteRepository.count()).isEqualTo(1);
    }
}

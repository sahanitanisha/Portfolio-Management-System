package com.wellsfargo.counselor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate} and the
 * {@code @CreatedBy}/{@code @LastModifiedBy} fields on {@link com.wellsfargo.counselor.entity.BaseEntity}
 * are populated automatically on every insert and update.
 *
 * <p>The auditor currently resolves to a fixed {@code "system"} principal. Once Spring Security is
 * introduced, replace this bean with one that reads the authenticated user from the
 * {@code SecurityContext} so audit trails attribute changes to a real principal.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(authentication.getName());
        };
    }
}

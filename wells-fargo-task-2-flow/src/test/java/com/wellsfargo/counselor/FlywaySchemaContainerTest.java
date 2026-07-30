package com.wellsfargo.counselor;

import com.wellsfargo.counselor.entity.Advisor;
import com.wellsfargo.counselor.repository.AdvisorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: boots the full application against a real PostgreSQL container, applies the Flyway
 * migrations, and has Hibernate {@code validate} the schema against the entity mappings - the exact
 * check the production profile performs. This catches any drift between the migrations and the entities.
 *
 * <p>{@code disabledWithoutDocker = true} makes the test skip itself when Docker is unavailable, so it
 * runs in CI (and on developer machines with Docker) without breaking {@code mvn verify} elsewhere.
 */
@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class FlywaySchemaContainerTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.properties.hibernate.timezone.default_storage", () -> "NORMALIZE_UTC");
    }

    @Autowired
    private AdvisorRepository advisorRepository;

    @Test
    void migrationsApplyAndHibernateValidatesTheSchema() {
        Advisor saved = advisorRepository.save(
                new Advisor("Ada", "Lovelace", "1 Analytical Way", "+1 555 0100", "ada@it.test"));

        assertThat(saved.getId()).isNotNull();
        assertThat(advisorRepository.findByEmail("ada@it.test")).isPresent();
    }
}

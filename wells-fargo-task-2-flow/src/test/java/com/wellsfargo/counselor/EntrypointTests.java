package com.wellsfargo.counselor;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Smoke test for the build baseline: starts the full Spring context (datasource, JPA, auditing,
 * web layer). If any bean, mapping, or configuration is broken, this fails - making every later
 * refactor verifiable with a single {@code ./mvnw test}.
 */
@SpringBootTest
class EntrypointTests {

    @Test
    void contextLoads() {
        // Intentionally empty: success means the application context started cleanly.
    }
}

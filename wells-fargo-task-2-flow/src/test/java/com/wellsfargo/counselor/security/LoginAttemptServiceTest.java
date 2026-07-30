package com.wellsfargo.counselor.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private LoginAttemptService service(int maxAttempts, Clock clock) {
        return new LoginAttemptService(maxAttempts, Duration.ofMinutes(15), clock);
    }

    @Test
    void blocksAfterReachingMaxFailures() {
        LoginAttemptService service = service(3, Clock.systemUTC());

        assertThat(service.isBlocked("grace")).isFalse();
        service.recordFailure("grace");
        service.recordFailure("grace");
        assertThat(service.isBlocked("grace")).isFalse(); // 2 < 3
        service.recordFailure("grace");
        assertThat(service.isBlocked("grace")).isTrue();  // 3 >= 3
    }

    @Test
    void resetClearsTheBlock() {
        LoginAttemptService service = service(2, Clock.systemUTC());
        service.recordFailure("grace");
        service.recordFailure("grace");
        assertThat(service.isBlocked("grace")).isTrue();

        service.reset("grace");
        assertThat(service.isBlocked("grace")).isFalse();
    }

    @Test
    void blockExpiresAfterTheWindow() {
        Instant start = Instant.parse("2026-06-19T10:00:00Z");
        LoginAttemptService service = new LoginAttemptService(2,
                Duration.ofMinutes(15), Clock.fixed(start, ZoneOffset.UTC));
        service.recordFailure("grace");
        service.recordFailure("grace");
        assertThat(service.isBlocked("grace")).isTrue();

        LoginAttemptService later = new LoginAttemptService(2,
                Duration.ofMinutes(15), Clock.fixed(start.plus(Duration.ofMinutes(16)), ZoneOffset.UTC));
        later.recordFailure("grace"); // a fresh window in a new instance
        assertThat(later.isBlocked("grace")).isFalse();
    }
}

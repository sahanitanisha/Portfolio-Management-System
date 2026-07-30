package com.wellsfargo.counselor.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory, fixed-window login throttle keyed by username. After {@code maxAttempts} failures within
 * the window, the key is temporarily blocked; a successful login resets it. Thread-safe and dependency
 * free; for multi-instance deployments back it with the shared Redis cache.
 *
 * <p>Blocking is temporary (not a permanent lockout) to bound the account-lockout denial-of-service a
 * username key implies; pair with edge rate limiting / WAF for IP-level protection.
 */
@Component
public class LoginAttemptService {

    private final int maxAttempts;
    private final Duration window;
    private final Clock clock;
    private final ConcurrentMap<String, Attempts> attempts = new ConcurrentHashMap<>();

    public LoginAttemptService(@Value("${app.security.login.max-attempts:5}") int maxAttempts,
                               @Value("${app.security.login.window-seconds:900}") long windowSeconds) {
        this(maxAttempts, Duration.ofSeconds(windowSeconds), Clock.systemUTC());
    }

    // Package-private for tests (deterministic clock).
    LoginAttemptService(int maxAttempts, Duration window, Clock clock) {
        this.maxAttempts = maxAttempts;
        this.window = window;
        this.clock = clock;
    }

    public boolean isBlocked(String key) {
        Attempts current = attempts.get(key);
        if (current == null) {
            return false;
        }
        if (isExpired(current)) {
            attempts.remove(key);
            return false;
        }
        return current.count() >= maxAttempts;
    }

    public void recordFailure(String key) {
        attempts.compute(key, (ignored, current) -> {
            if (current == null || isExpired(current)) {
                return new Attempts(1, clock.instant());
            }
            return new Attempts(current.count() + 1, current.windowStart());
        });
    }

    public void reset(String key) {
        attempts.remove(key);
    }

    private boolean isExpired(Attempts current) {
        return clock.instant().isAfter(current.windowStart().plus(window));
    }

    private record Attempts(int count, Instant windowStart) {
    }
}

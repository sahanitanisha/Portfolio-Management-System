package com.wellsfargo.counselor.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

/**
 * A server-side refresh token, enabling short-lived access tokens without forcing frequent re-login and
 * supporting revocation (logout) and single-use rotation. Only the SHA-256 hash of the opaque token is
 * stored - never the raw value.
 */
@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(name = "idx_refresh_token_hash", columnList = "token_hash", unique = true)
})
public class RefreshToken extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private AppUser user;

    @NotBlank
    @Column(name = "token_hash", nullable = false, unique = true, updatable = false, length = 64)
    private String tokenHash;

    @NotNull
    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked;

    /** Required by JPA. */
    protected RefreshToken() {
    }

    private RefreshToken(AppUser user, String tokenHash, Instant expiresAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.revoked = false;
    }

    public static RefreshToken create(AppUser user, String tokenHash, Instant expiresAt) {
        return new RefreshToken(user, tokenHash, expiresAt);
    }

    public boolean isActive(Instant now) {
        return !revoked && expiresAt.isAfter(now);
    }

    public void revoke() {
        this.revoked = true;
    }

    public AppUser getUser() {
        return user;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }
}

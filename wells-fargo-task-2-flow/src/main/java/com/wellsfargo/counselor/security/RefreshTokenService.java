package com.wellsfargo.counselor.security;

import com.wellsfargo.counselor.entity.AppUser;
import com.wellsfargo.counselor.entity.RefreshToken;
import com.wellsfargo.counselor.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Manages opaque, server-side refresh tokens. The raw token is a 256-bit random value returned to the
 * client once; only its SHA-256 hash is persisted. Tokens are single-use: {@link #rotate} validates and
 * revokes the presented token (the caller then issues a fresh one), which limits the blast radius of a
 * leaked token. {@link #revoke} powers logout.
 */
@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final long ttlSeconds;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               @Value("${app.security.jwt.refresh-expiration-seconds:604800}") long ttlSeconds) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public String issue(AppUser user) {
        String rawToken = generateRawToken();
        refreshTokenRepository.save(RefreshToken.create(user, hash(rawToken), Instant.now().plusSeconds(ttlSeconds)));
        return rawToken;
    }

    /** Validates the presented token, revokes it (single-use rotation), and returns its owner. */
    @Transactional
    public AppUser rotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
        if (!token.isActive(Instant.now())) {
            throw new BadCredentialsException("Refresh token has expired or been revoked");
        }
        token.revoke();
        return token.getUser();
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(RefreshToken::revoke);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}

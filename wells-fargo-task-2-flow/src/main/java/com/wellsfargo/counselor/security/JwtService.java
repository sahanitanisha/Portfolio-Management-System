package com.wellsfargo.counselor.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

/**
 * Issues and verifies asymmetric (RS256) JWT <em>access</em> tokens. Signing uses an RSA private key,
 * verification a public key, so verifiers never hold signing material.
 *
 * <p>Keys are loaded from configuration as Base64-encoded DER (PKCS#8 private, X.509 public). When none
 * are configured the service generates an <strong>ephemeral</strong> key pair at startup (development
 * only - tokens do not survive a restart); production must supply persistent keys.
 *
 * <p>Access tokens are deliberately short-lived; longer-lived, revocable refresh tokens are handled by
 * {@link RefreshTokenService}.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long accessExpirationSeconds;

    public JwtService(@Value("${app.security.jwt.private-key:}") String privateKeyBase64,
                      @Value("${app.security.jwt.public-key:}") String publicKeyBase64,
                      @Value("${app.security.jwt.access-expiration-seconds:900}") long accessExpirationSeconds) {
        this.accessExpirationSeconds = accessExpirationSeconds;
        if (privateKeyBase64.isBlank() || publicKeyBase64.isBlank()) {
            KeyPair ephemeral = generateEphemeralKeyPair();
            this.privateKey = ephemeral.getPrivate();
            this.publicKey = ephemeral.getPublic();
            log.warn("No RSA JWT keys configured - generated an EPHEMERAL key pair (development only). "
                    + "Set app.security.jwt.private-key / public-key in production.");
        } else {
            this.privateKey = loadPrivateKey(privateKeyBase64);
            this.publicKey = loadPublicKey(publicKeyBase64);
        }
    }

    public String generateAccessToken(String username, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("type", "access")
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(accessExpirationSeconds)))
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /** Verifies a token against the public key and returns its claims; throws on an invalid token. */
    public Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationSeconds;
    }

    private static KeyPair generateEphemeralKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA key generation is unavailable", e);
        }
    }

    private static PrivateKey loadPrivateKey(String base64Der) {
        try {
            byte[] der = Base64.getDecoder().decode(base64Der.replaceAll("\\s", ""));
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid RSA private key (expected Base64 PKCS#8 DER)", e);
        }
    }

    private static PublicKey loadPublicKey(String base64Der) {
        try {
            byte[] der = Base64.getDecoder().decode(base64Der.replaceAll("\\s", ""));
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid RSA public key (expected Base64 X.509 DER)", e);
        }
    }
}

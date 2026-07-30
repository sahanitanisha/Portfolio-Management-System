package com.wellsfargo.counselor.security;

import com.wellsfargo.counselor.dto.AuthResponse;
import com.wellsfargo.counselor.dto.LoginRequest;
import com.wellsfargo.counselor.dto.RefreshRequest;
import com.wellsfargo.counselor.dto.RegisterRequest;
import com.wellsfargo.counselor.entity.AppUser;
import com.wellsfargo.counselor.entity.Role;
import com.wellsfargo.counselor.exception.TooManyAttemptsException;
import com.wellsfargo.counselor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Registration, authentication, and token lifecycle. Passwords are BCrypt-verified with a uniform
 * "invalid credentials" message (no user enumeration) and login throttling (brute-force defence). Each
 * successful authentication issues a short-lived RS256 access token plus a rotating refresh token;
 * {@link #refresh} exchanges a refresh token for a new pair, {@link #logout} revokes it.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final LoginAttemptService loginAttemptService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenService refreshTokenService, LoginAttemptService loginAttemptService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.loginAttemptService = loginAttemptService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }
        AppUser user = new AppUser(request.username(), passwordEncoder.encode(request.password()), Role.VIEWER);
        userRepository.save(user);
        log.info("Registered user '{}' with role {}", user.getUsername(), user.getRole());
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String username = request.username();
        if (loginAttemptService.isBlocked(username)) {
            throw new TooManyAttemptsException("Too many failed login attempts for this account; try again later");
        }
        Optional<AppUser> user = userRepository.findByUsername(username);
        if (user.isEmpty() || !user.get().isEnabled()
                || !passwordEncoder.matches(request.password(), user.get().getPasswordHash())) {
            loginAttemptService.recordFailure(username);
            throw new BadCredentialsException("Invalid username or password");
        }
        loginAttemptService.reset(username);
        return issueTokens(user.get());
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        AppUser user = refreshTokenService.rotate(request.refreshToken());
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }

    private AuthResponse issueTokens(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user.getUsername(), user.getRole().name());
        String refreshToken = refreshTokenService.issue(user);
        return AuthResponse.of(accessToken, refreshToken, user.getUsername(), user.getRole(),
                jwtService.getAccessExpirationSeconds());
    }
}

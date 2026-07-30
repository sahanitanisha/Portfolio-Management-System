package com.wellsfargo.counselor.security;

import com.wellsfargo.counselor.config.JpaAuditingConfig;
import com.wellsfargo.counselor.entity.AppUser;
import com.wellsfargo.counselor.entity.Role;
import com.wellsfargo.counselor.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.BadCredentialsException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Slice test for {@link RefreshTokenService}: issue, single-use rotation, and revocation. */
@DataJpaTest
@Import({RefreshTokenService.class, JpaAuditingConfig.class})
class RefreshTokenServiceTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RefreshTokenService refreshTokenService;

    private AppUser newUser() {
        return userRepository.save(new AppUser("grace", "bcrypt-hash", Role.VIEWER));
    }

    @Test
    void issuesThenRotatesSingleUse() {
        AppUser user = newUser();
        String raw = refreshTokenService.issue(user);
        assertThat(raw).isNotBlank();

        AppUser rotated = refreshTokenService.rotate(raw);
        assertThat(rotated.getUsername()).isEqualTo("grace");

        // The token is single-use: a second rotation must fail (it was revoked).
        assertThatThrownBy(() -> refreshTokenService.rotate(raw)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void revokeInvalidatesTheToken() {
        String raw = refreshTokenService.issue(newUser());
        refreshTokenService.revoke(raw);
        assertThatThrownBy(() -> refreshTokenService.rotate(raw)).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void unknownTokenIsRejected() {
        assertThatThrownBy(() -> refreshTokenService.rotate("not-a-real-token"))
                .isInstanceOf(BadCredentialsException.class);
    }
}

package com.wellsfargo.counselor.config;

import com.wellsfargo.counselor.entity.AppUser;
import com.wellsfargo.counselor.entity.Role;
import com.wellsfargo.counselor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds a single ADMIN account from configuration on startup, only if one is configured and absent.
 * There is no API to self-assign privileged roles, so this is the controlled way to bootstrap the
 * first administrator. The dev defaults must be changed (or the properties removed) in production.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminUsername;
    private final String adminPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           @Value("${app.security.bootstrap-admin.username:}") String adminUsername,
                           @Value("${app.security.bootstrap-admin.password:}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminUsername.isBlank() || adminPassword.isBlank() || userRepository.existsByUsername(adminUsername)) {
            return;
        }
        userRepository.save(new AppUser(adminUsername, passwordEncoder.encode(adminPassword), Role.ADMIN));
        log.warn("Bootstrapped ADMIN user '{}' from configuration - change this password in production", adminUsername);
    }
}

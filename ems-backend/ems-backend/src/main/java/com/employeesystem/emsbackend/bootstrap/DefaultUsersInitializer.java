package com.employeesystem.emsbackend.bootstrap;

import com.employeesystem.emsbackend.entity.Role;
import com.employeesystem.emsbackend.entity.User;
import com.employeesystem.emsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultUsersInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin@local";
    private static final String DEFAULT_ADMIN_PASSWORD = "Admin123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    @Value("${CREATE_DEFAULT_USERS:false}")
    private boolean createDefaultUsers;

    @Value("${DEFAULT_ADMIN_USERNAME:}")
    private String configuredAdminUsername;

    @Value("${DEFAULT_ADMIN_PASSWORD:}")
    private String configuredAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!createDefaultUsers) {
            return;
        }

        String adminUsername = isBlank(configuredAdminUsername) ? DEFAULT_ADMIN_USERNAME : configuredAdminUsername.trim();
        boolean usingDefaultPassword = isBlank(configuredAdminPassword);
        String adminPassword = usingDefaultPassword ? DEFAULT_ADMIN_PASSWORD : configuredAdminPassword;

        Set<Role> adminRoles = EnumSet.of(Role.ADMIN, Role.HR, Role.MANAGER, Role.EMPLOYEE);

        User user = userRepository.findByUsername(adminUsername).orElse(null);
        if (user == null) {
            User created = new User();
            created.setUsername(adminUsername);
            created.setPassword(passwordEncoder.encode(adminPassword));
            created.getRoles().addAll(adminRoles);
            userRepository.save(created);

            log.warn("Created default admin user '{}' because CREATE_DEFAULT_USERS=true. Disable this in production.", adminUsername);
            if (usingDefaultPassword) {
                if (isProdProfileActive()) {
                    log.warn("Default admin password was used (DEFAULT_ADMIN_PASSWORD not set). Password is NOT printed because a prod profile is active.");
                } else {
                    log.warn("Default admin password: {} (set DEFAULT_ADMIN_PASSWORD to override; do not use this in production)", DEFAULT_ADMIN_PASSWORD);
                }
            } else {
                log.info("Default admin password was provided via DEFAULT_ADMIN_PASSWORD (not logging it).");
            }
            return;
        }

        boolean changed = false;
        if (!user.getRoles().containsAll(adminRoles)) {
            user.getRoles().addAll(adminRoles);
            changed = true;
        }
        if (changed) {
            userRepository.save(user);
            log.info("Updated existing user '{}' to include required roles: {}", adminUsername, adminRoles);
        }
    }

    private boolean isProdProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(p -> p == null ? "" : p.toLowerCase(Locale.ROOT))
                .anyMatch(p -> p.equals("prod") || p.equals("production"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

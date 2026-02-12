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
import java.util.UUID;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultUsersInitializer implements ApplicationRunner {

    private static final String DEFAULT_ADMIN_USERNAME = "admin@local";

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

        String adminUsername = isBlank(configuredAdminUsername) ? DEFAULT_ADMIN_USERNAME
                : configuredAdminUsername.trim();
        boolean passwordProvided = !isBlank(configuredAdminPassword);
        boolean prod = isProdProfileActive();

        String adminPassword;
        if (passwordProvided) {
            adminPassword = configuredAdminPassword;
        } else {
            if (prod) {
                log.error(
                        "CREATE_DEFAULT_USERS=true but DEFAULT_ADMIN_PASSWORD is not set while a prod profile is active. Refusing to create a default admin user.");
                return;
            }
            adminPassword = generateDevPassword();
        }

        Set<Role> adminRoles = EnumSet.of(Role.ADMIN, Role.HR, Role.MANAGER, Role.EMPLOYEE);

        User user = userRepository.findByUsername(adminUsername).orElse(null);
        if (user == null) {
            User created = new User();
            created.setUsername(adminUsername);
            created.setPassword(passwordEncoder.encode(adminPassword));
            created.getRoles().addAll(adminRoles);
            userRepository.save(created);

            log.warn("Created default admin user '{}' because CREATE_DEFAULT_USERS=true. Disable this in production.",
                    adminUsername);

            if (passwordProvided) {
                log.info("Default admin password was provided via DEFAULT_ADMIN_PASSWORD (not logging it). ");
            } else {
                log.warn("Generated a one-time dev admin password (DEFAULT_ADMIN_PASSWORD not set): {}", adminPassword);
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

    private static String generateDevPassword() {
        // Not intended for production. Example format: Dev-<uuid>
        return "Dev-" + UUID.randomUUID();
    }
}

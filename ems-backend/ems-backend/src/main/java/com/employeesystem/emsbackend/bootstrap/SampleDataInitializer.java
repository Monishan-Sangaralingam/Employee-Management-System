package com.employeesystem.emsbackend.bootstrap;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.Role;
import com.employeesystem.emsbackend.entity.User;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sample-data.enabled", havingValue = "true")
public class SampleDataInitializer implements ApplicationRunner {

    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Employee managerEmployee = ensureEmployee(
                "Manny",
                "Manager",
                "manny.manager@example.com",
                "Management");

        Employee employeeEmployee = ensureEmployee(
                "Eve",
                "Employee",
                "eve.employee@example.com",
                "Engineering");

        Employee hrEmployee = ensureEmployee(
                "Hannah",
                "HR",
                "hannah.hr@example.com",
                "HR");

        ensureUser("manager1", "secret", EnumSet.of(Role.MANAGER), managerEmployee);
        ensureUser("employee1", "secret", EnumSet.of(Role.EMPLOYEE), employeeEmployee);
        ensureUser("hr1", "secret", EnumSet.of(Role.HR), hrEmployee);

        log.info("Sample data ensured (employees + users). Demo users: manager1/secret, employee1/secret, hr1/secret");
    }

    private Employee ensureEmployee(String firstName, String lastName, String email, String department) {
        Employee existing = employeeRepository.findByEmail(email);
        if (existing != null) {
            boolean changed = false;
            if (existing.getDepartment() == null || existing.getDepartment().isBlank()) {
                existing.setDepartment(department);
                changed = true;
            }
            if (changed) {
                return employeeRepository.save(existing);
            }
            return existing;
        }

        Employee created = new Employee();
        created.setFirstName(firstName);
        created.setLastName(lastName);
        created.setEmail(email);
        created.setDepartment(department);
        return employeeRepository.save(created);
    }

    private void ensureUser(String username, String rawPassword, EnumSet<Role> roles, Employee employee) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            User created = new User();
            created.setUsername(username);
            created.setPassword(passwordEncoder.encode(rawPassword));
            created.getRoles().addAll(roles);
            created.setEmployee(employee);
            userRepository.save(created);
            return;
        }

        boolean changed = false;
        if (!user.getRoles().containsAll(roles)) {
            user.getRoles().addAll(roles);
            changed = true;
        }

        if (user.getEmployee() == null) {
            user.setEmployee(employee);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
        }
    }
}

package com.employeesystem.emsbackend.controller;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.Role;
import com.employeesystem.emsbackend.entity.User;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.UserRepository;
import com.employeesystem.emsbackend.security.JwtUtils;
import com.employeesystem.emsbackend.security.UserPrincipal;
import com.employeesystem.emsbackend.web.auth.LoginRequest;
import com.employeesystem.emsbackend.web.auth.LoginResponse;
import com.employeesystem.emsbackend.web.auth.MeResponse;
import com.employeesystem.emsbackend.web.auth.RegisterRequest;
import com.employeesystem.emsbackend.web.auth.RegisterResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String username = authentication.getName();
        Set<String> roles = authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                .collect(Collectors.toSet());

        Long employeeId = userRepository.findByUsername(username)
                .map(User::getEmployee)
                .map(Employee::getId)
                .orElse(null);

        String token = jwtUtils.generateToken(username, roles, employeeId);
        return ResponseEntity.ok(new LoginResponse(token, username, roles, employeeId));
    }

        @GetMapping("/me")
        @PreAuthorize("isAuthenticated()")
        public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
                Set<String> roles = principal.getAuthorities()
                                .stream()
                                .map(GrantedAuthority::getAuthority)
                                .map(a -> a.startsWith("ROLE_") ? a.substring("ROLE_".length()) : a)
                                .collect(Collectors.toSet());

                Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
                return ResponseEntity.ok(new MeResponse(principal.getUsername(), roles, employeeId));
        }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new RegisterResponse(null, request.getUsername(), Set.of(), "Username already exists"));
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        Set<Role> roles = (request.getRoles() == null || request.getRoles().isEmpty())
                ? Set.of(Role.EMPLOYEE)
                : request.getRoles();
        user.getRoles().addAll(roles);

        if (request.getEmployeeId() != null) {
            Employee employee = employeeRepository.findById(request.getEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee Id " + request.getEmployeeId() + " not found"));
            user.setEmployee(employee);
        }

        User saved = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterResponse(saved.getId(), saved.getUsername(), saved.getRoles(), "Registered"));
    }
}

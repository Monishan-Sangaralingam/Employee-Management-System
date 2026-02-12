package com.employeesystem.emsbackend.security;

import com.employeesystem.emsbackend.entity.Role;
import com.employeesystem.emsbackend.entity.User;
import com.employeesystem.emsbackend.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        Long employeeId = user.getEmployee() != null ? user.getEmployee().getId() : null;

        return new UserPrincipal(
            user.getUsername(),
            user.getPassword(),
            employeeId,
            user.getRoles().stream()
                .map(Role::name)
                .map(r -> "ROLE_" + r)
                .collect(Collectors.toSet())
        );
    }
}

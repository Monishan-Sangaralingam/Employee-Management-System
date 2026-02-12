package com.employeesystem.emsbackend.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final String username;
    private final String password;
    private final EmployeeInfo employee;
    private final Set<String> roleAuthorities;

    public UserPrincipal(String username, String password, Long employeeId, Set<String> roleAuthorities) {
        this.username = username;
        this.password = password;
        this.employee = new EmployeeInfo(employeeId);
        this.roleAuthorities = roleAuthorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roleAuthorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Getter
    public static class EmployeeInfo {
        private final Long id;

        public EmployeeInfo(Long id) {
            this.id = id;
        }
    }
}

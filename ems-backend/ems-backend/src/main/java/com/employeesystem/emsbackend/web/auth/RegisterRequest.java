package com.employeesystem.emsbackend.web.auth;

import com.employeesystem.emsbackend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    private String username;
    private String password;
    private Set<Role> roles;
    private Long employeeId;
}

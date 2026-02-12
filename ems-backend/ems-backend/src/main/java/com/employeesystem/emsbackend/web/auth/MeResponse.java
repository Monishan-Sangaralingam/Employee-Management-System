package com.employeesystem.emsbackend.web.auth;

import java.util.Set;

public record MeResponse(String username, Set<String> roles, Long employeeId) {
}

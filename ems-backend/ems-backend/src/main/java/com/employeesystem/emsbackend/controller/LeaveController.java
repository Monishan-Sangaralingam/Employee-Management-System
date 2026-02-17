package com.employeesystem.emsbackend.controller;

import com.employeesystem.emsbackend.entity.LeaveRequest;
import com.employeesystem.emsbackend.entity.LeaveStatus;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.security.UserPrincipal;
import com.employeesystem.emsbackend.service.LeaveService;
import com.employeesystem.emsbackend.web.leave.DecideLeaveRequest;
import com.employeesystem.emsbackend.web.leave.LeaveApplyRequest;
import com.employeesystem.emsbackend.web.leave.LeaveResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/leave")
@AllArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping("/me/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponse> apply(@AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LeaveApplyRequest request) {
        Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (employeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }
        LeaveRequest saved = leaveService.applyLeave(employeeId, request.getStartDate(), request.getEndDate(),
                request.getType(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveResponse.from(saved));
    }

    // Alias: POST /api/leave/apply (optionally apply for a specific employeeId if
    // caller is HR/MANAGER/ADMIN)
    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<LeaveResponse> applyAlias(@AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long employeeId,
            @RequestBody LeaveApplyRequest request) {
        Long principalEmployeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (principalEmployeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }

        boolean privileged = principal.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority())
                || "ROLE_HR".equals(a.getAuthority()) || "ROLE_MANAGER".equals(a.getAuthority()));

        Long targetEmployeeId = principalEmployeeId;
        if (employeeId != null) {
            if (!privileged) {
                throw new BadRequestException("Not allowed to apply leave for other employees");
            }
            targetEmployeeId = employeeId;
        }

        LeaveRequest saved = leaveService.applyLeave(targetEmployeeId, request.getStartDate(), request.getEndDate(),
                request.getType(), request.getNote());
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveResponse.from(saved));
    }

    @PutMapping("/{id}/decide")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<LeaveResponse> decide(@PathVariable("id") Long id,
            @RequestBody DecideLeaveRequest request) {
        LeaveStatus status = request.getStatus();
        LeaveRequest decided = leaveService.decide(id, status, request.getNote());
        return ResponseEntity.ok(LeaveResponse.from(decided));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<LeaveResponse>> myRequests(@AuthenticationPrincipal UserPrincipal principal) {
        Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (employeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }

        List<LeaveResponse> rows = leaveService.getMyRequests(employeeId)
                .stream()
                .map(LeaveResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR')")
    public ResponseEntity<List<LeaveResponse>> pending() {
        List<LeaveResponse> rows = leaveService.getPendingRequests()
                .stream()
                .map(LeaveResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @DeleteMapping("/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancel(@AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("id") Long id) {
        Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (employeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }

        leaveService.cancelPending(id, employeeId);
        return ResponseEntity.ok(java.util.Map.of("message", "Cancelled"));
    }
}

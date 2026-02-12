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
        LeaveRequest saved = leaveService.applyLeave(employeeId, request.getStartDate(), request.getEndDate(), request.getType(), request.getNote());
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
}

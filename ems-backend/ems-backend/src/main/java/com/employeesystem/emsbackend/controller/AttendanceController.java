package com.employeesystem.emsbackend.controller;

import com.employeesystem.emsbackend.entity.Attendance;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.security.UserPrincipal;
import com.employeesystem.emsbackend.service.AttendanceService;
import com.employeesystem.emsbackend.web.attendance.AttendanceResponse;
import com.employeesystem.emsbackend.web.attendance.MonthlyTotalsResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/attendance")
@AllArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/me/check-in")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkIn(@AuthenticationPrincipal UserPrincipal principal) {
        Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (employeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }
        Attendance attendance = attendanceService.checkIn(employeeId);
        return ResponseEntity.ok(AttendanceResponse.from(attendance));
    }

    @PostMapping("/me/check-out")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkOut(@AuthenticationPrincipal UserPrincipal principal) {
        Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (employeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }
        Attendance attendance = attendanceService.checkOut(employeeId);
        return ResponseEntity.ok(AttendanceResponse.from(attendance));
    }

    @GetMapping("/me/monthly")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> myMonthly(@AuthenticationPrincipal UserPrincipal principal,
                                                             @RequestParam int year,
                                                             @RequestParam int month) {
        Long employeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (employeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }
        List<AttendanceResponse> rows = attendanceService.getMonthlyAttendance(employeeId, year, month)
                .stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/report/{employeeId}/monthly")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<List<AttendanceResponse>> employeeMonthly(@PathVariable Long employeeId,
                                                                   @RequestParam int year,
                                                                   @RequestParam int month) {
        List<AttendanceResponse> rows = attendanceService.getMonthlyAttendance(employeeId, year, month)
                .stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/report/{employeeId}/totals")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<MonthlyTotalsResponse> employeeMonthlyTotals(@PathVariable Long employeeId,
                                                                       @RequestParam int year,
                                                                       @RequestParam int month) {
        Long total = attendanceService.getMonthlyTotalWorkedMinutes(employeeId, year, month);
        return ResponseEntity.ok(new MonthlyTotalsResponse(employeeId, year, month, total));
    }
}

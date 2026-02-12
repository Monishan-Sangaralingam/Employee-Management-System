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

import java.time.YearMonth;
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

    // Alias for frontend: POST /api/attendance/checkin
    @PostMapping("/checkin")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkInAlias(@AuthenticationPrincipal UserPrincipal principal) {
        return checkIn(principal);
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

    // Alias for frontend: POST /api/attendance/checkout
    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> checkOutAlias(@AuthenticationPrincipal UserPrincipal principal) {
        return checkOut(principal);
    }

    // Alias for frontend: GET /api/attendance/employee/{employeeId}?date=today
    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AttendanceResponse> todayForEmployee(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long employeeId,
                                                              @RequestParam(required = false) String date) {
        Long principalEmployeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (principalEmployeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }

        boolean adminOrHr = principal.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_HR".equals(a.getAuthority()));

        if (!adminOrHr && !principalEmployeeId.equals(employeeId)) {
            throw new BadRequestException("Not allowed to view other employees");
        }

        // Only 'today' is supported for now; other values return today's record as well.
        var row = attendanceService.getTodayAttendance(employeeId);
        return ResponseEntity.ok(row == null ? null : AttendanceResponse.from(row));
    }

    // Alias for frontend: GET /api/attendance/monthly/{yyyy-MM}
    @GetMapping("/monthly/{yyyyMM}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AttendanceResponse>> monthlyAlias(@AuthenticationPrincipal UserPrincipal principal,
                                                                @PathVariable String yyyyMM,
                                                                @RequestParam(required = false) Long employeeId) {
        Long principalEmployeeId = principal.getEmployee() != null ? principal.getEmployee().getId() : null;
        if (principalEmployeeId == null) {
            throw new BadRequestException("No employee linked to this user");
        }

        boolean adminOrHr = principal.getAuthorities().stream().anyMatch(a ->
                "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_HR".equals(a.getAuthority()));

        Long targetEmployeeId = principalEmployeeId;
        if (employeeId != null) {
            if (!adminOrHr && !principalEmployeeId.equals(employeeId)) {
                throw new BadRequestException("Not allowed to view other employees");
            }
            targetEmployeeId = employeeId;
        }

        YearMonth ym;
        try {
            ym = YearMonth.parse(yyyyMM);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid month format. Expected yyyy-MM");
        }

        List<AttendanceResponse> rows = attendanceService.getMonthlyAttendance(targetEmployeeId, ym.getYear(), ym.getMonthValue())
                .stream()
                .map(AttendanceResponse::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(rows);
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

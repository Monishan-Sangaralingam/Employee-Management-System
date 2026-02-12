package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Attendance;
import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.AttendanceRepository;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@AllArgsConstructor
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public Attendance checkIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Id " + employeeId + " not found"));

        attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(employeeId)
                .ifPresent(open -> {
                    throw new BadRequestException("Cannot check-in twice without checkout");
                });

        LocalDateTime now = LocalDateTime.now();
        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setWorkDate(now.toLocalDate());
        attendance.setCheckIn(now);
        attendance.setWorkedMinutes(0L);
        return attendanceRepository.save(attendance);
    }

    public Attendance checkOut(Long employeeId) {
        Attendance open = attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(employeeId)
                .orElseThrow(() -> new BadRequestException("No active check-in found to checkout"));

        LocalDate today = LocalDate.now();
        if (!today.equals(open.getWorkDate())) {
            throw new BadRequestException("Cannot checkout for a previous day");
        }

        LocalDateTime now = LocalDateTime.now();
        open.setCheckOut(now);
        long workedMinutes = Duration.between(open.getCheckIn(), now).toMinutes();
        open.setWorkedMinutes(Math.max(0L, workedMinutes));
        return attendanceRepository.save(open);
    }

    public List<Attendance> getMonthlyAttendance(Long employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        return attendanceRepository.findByEmployeeIdAndWorkDateBetweenOrderByCheckInAsc(employeeId, start, end);
    }

    public Attendance getTodayAttendance(Long employeeId) {
        LocalDate today = LocalDate.now();
        return attendanceRepository.findTopByEmployeeIdAndWorkDateOrderByCheckInDesc(employeeId, today)
                .orElse(null);
    }

    public Long getMonthlyTotalWorkedMinutes(Long employeeId, int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        Long total = attendanceRepository.sumWorkedMinutesForEmployeeBetween(employeeId, start, end);
        return total == null ? 0L : total;
    }
}

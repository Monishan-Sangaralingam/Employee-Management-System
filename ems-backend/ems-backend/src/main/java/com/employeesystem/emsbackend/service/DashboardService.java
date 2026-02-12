package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.LeaveStatus;
import com.employeesystem.emsbackend.repository.AttendanceRepository;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.LeaveRequestRepository;
import com.employeesystem.emsbackend.repository.PayrollRecordRepository;
import com.employeesystem.emsbackend.web.dashboard.DashboardResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PayrollRecordRepository payrollRecordRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                            AttendanceRepository attendanceRepository,
                            LeaveRequestRepository leaveRequestRepository,
                            PayrollRecordRepository payrollRecordRepository) {
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.payrollRecordRepository = payrollRecordRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard() {
        long totalEmployees = employeeRepository.count();

        YearMonth ym = YearMonth.now();
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        long activeThisMonth = attendanceRepository.countDistinctEmployeesActiveBetween(start, end);

        Double avgWorkedMinutes = attendanceRepository.avgWorkedMinutesBetween(start, end);
        double avgAttendanceHours = 0.0;
        if (avgWorkedMinutes != null) {
            avgAttendanceHours = BigDecimal.valueOf(avgWorkedMinutes)
                    .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        long leavesPending = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);
        Map<String, Long> leaveStatusCounts = toLeaveStatusCounts(leaveRequestRepository.countByStatusGrouped());

        BigDecimal salaryExpenseThisMonth = payrollRecordRepository.sumNetPayForMonth(ym.getYear(), ym.getMonthValue());

        Map<String, Long> departmentCounts = computeDepartmentCounts(employeeRepository.findAll());

        return new DashboardResponse(
                totalEmployees,
                activeThisMonth,
                avgAttendanceHours,
                leavesPending,
                salaryExpenseThisMonth,
                departmentCounts,
                leaveStatusCounts
        );
    }

    private Map<String, Long> computeDepartmentCounts(List<Employee> employees) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Employee e : employees) {
            String dept = "Unassigned";
            String value = e.getDepartment();
            if (value != null && !value.isBlank()) {
                dept = value.trim();
            }
            counts.put(dept, counts.getOrDefault(dept, 0L) + 1);
        }
        return counts;
    }

    private Map<String, Long> toLeaveStatusCounts(List<Object[]> rows) {
        Map<String, Long> out = new LinkedHashMap<>();
        // Ensure stable order
        out.put("PENDING", 0L);
        out.put("APPROVED", 0L);
        out.put("REJECTED", 0L);

        for (Object[] row : rows) {
            if (row == null || row.length < 2) continue;
            Object status = row[0];
            Object count = row[1];
            if (status == null || count == null) continue;
            out.put(String.valueOf(status), ((Number) count).longValue());
        }
        return out;
    }
}

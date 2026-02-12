package com.employeesystem.emsbackend.web.dashboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalEmployees;
    private long activeThisMonth;
    private double avgAttendanceHours;
    private long leavesPending;
    private BigDecimal salaryExpenseThisMonth;
    private Map<String, Long> departmentCounts;

    // Needed for donut chart in UI
    private Map<String, Long> leaveStatusCounts;
}

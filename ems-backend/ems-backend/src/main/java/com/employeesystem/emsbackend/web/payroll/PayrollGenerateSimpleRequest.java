package com.employeesystem.emsbackend.web.payroll;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayrollGenerateSimpleRequest {
    private Long employeeId;
    private String payrollMonth; // YYYY-MM
    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
}

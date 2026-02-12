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
public class PayrollGenerateRequest {
    private Integer year;
    private Integer month;
    private BigDecimal baseSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
}

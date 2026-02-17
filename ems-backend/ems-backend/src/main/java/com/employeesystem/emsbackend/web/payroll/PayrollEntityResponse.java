package com.employeesystem.emsbackend.web.payroll;

import com.employeesystem.emsbackend.entity.Payroll;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayrollEntityResponse {

    private Long id;
    private Long employeeId;
    private String payrollMonth;

    private BigDecimal basicSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;

    private BigDecimal epfEmployeePercent;
    private BigDecimal epfEmployerPercent;
    private BigDecimal etfEmployerPercent;

    private BigDecimal epfEmployeeAmount;
    private BigDecimal epfEmployerAmount;
    private BigDecimal etfEmployerAmount;

    private BigDecimal grossSalary;
    private BigDecimal netSalary;

    private LocalDateTime generatedAt;
    private String generatedBy;

    public static PayrollEntityResponse from(Payroll p) {
        return new PayrollEntityResponse(
                p.getId(),
                p.getEmployee() != null ? p.getEmployee().getId() : null,
                p.getPayrollMonth(),
                p.getBasicSalary(),
                p.getAllowances(),
                p.getDeductions(),
                p.getEpfEmployeePercent(),
                p.getEpfEmployerPercent(),
                p.getEtfEmployerPercent(),
                p.getEpfEmployeeAmount(),
                p.getEpfEmployerAmount(),
                p.getEtfEmployerAmount(),
                p.getGrossSalary(),
                p.getNetSalary(),
                p.getGeneratedAt(),
                p.getGeneratedBy());
    }
}

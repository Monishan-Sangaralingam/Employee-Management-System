package com.employeesystem.emsbackend.web.payroll;

import com.employeesystem.emsbackend.entity.PayrollRecord;
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
public class PayrollResponse {
    private Long id;
    private Long employeeId;
    private Integer year;
    private Integer month;
    private BigDecimal baseSalary;
    private BigDecimal allowances;
    private BigDecimal deductions;
    private BigDecimal grossPay;
    private BigDecimal epfEmployee;
    private BigDecimal epfEmployer;
    private BigDecimal netPay;
    private LocalDateTime createdAt;

    public static PayrollResponse from(PayrollRecord r) {
        return new PayrollResponse(
                r.getId(),
                r.getEmployee() != null ? r.getEmployee().getId() : null,
                r.getYear(),
                r.getMonth(),
                r.getBaseSalary(),
                r.getAllowances(),
                r.getDeductions(),
                r.getGrossPay(),
                r.getEpfEmployee(),
                r.getEpfEmployer(),
                r.getNetPay(),
                r.getCreatedAt()
        );
    }
}

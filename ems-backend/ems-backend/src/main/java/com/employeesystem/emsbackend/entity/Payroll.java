package com.employeesystem.emsbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Payroll month in format YYYY-MM.
     */
    @Column(name = "payroll_month", nullable = false, length = 7)
    private String payrollMonth;

    // Salary components

    @Column(name = "basic_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal basicSalary;

    @Column(name = "allowances", nullable = false, precision = 12, scale = 2)
    private BigDecimal allowances = BigDecimal.ZERO;

    @Column(name = "deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal deductions = BigDecimal.ZERO;

    // EPF/ETF percentages

    @Column(name = "epf_employee_percent", nullable = false, precision = 12, scale = 2)
    private BigDecimal epfEmployeePercent = new BigDecimal("8.00");

    @Column(name = "epf_employer_percent", nullable = false, precision = 12, scale = 2)
    private BigDecimal epfEmployerPercent = new BigDecimal("12.00");

    @Column(name = "etf_employer_percent", nullable = false, precision = 12, scale = 2)
    private BigDecimal etfEmployerPercent = new BigDecimal("3.00");

    // Calculated amounts

    @Column(name = "epf_employee_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal epfEmployeeAmount = BigDecimal.ZERO;

    @Column(name = "epf_employer_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal epfEmployerAmount = BigDecimal.ZERO;

    @Column(name = "etf_employer_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal etfEmployerAmount = BigDecimal.ZERO;

    @Column(name = "gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal grossSalary = BigDecimal.ZERO;

    @Column(name = "net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal netSalary = BigDecimal.ZERO;

    // Metadata

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @Column(name = "generated_by", length = 120)
    private String generatedBy;

    @PrePersist
    @PreUpdate
    private void recalculate() {
        if (allowances == null)
            allowances = BigDecimal.ZERO;
        if (deductions == null)
            deductions = BigDecimal.ZERO;
        if (epfEmployeePercent == null)
            epfEmployeePercent = new BigDecimal("8.00");
        if (epfEmployerPercent == null)
            epfEmployerPercent = new BigDecimal("12.00");
        if (etfEmployerPercent == null)
            etfEmployerPercent = new BigDecimal("3.00");

        final BigDecimal hundred = new BigDecimal("100.00");

        // Gross salary = basic + allowances
        grossSalary = money(safeMoney(basicSalary).add(safeMoney(allowances)));

        // EPF/ETF contributions are based on basic salary (not gross)
        BigDecimal basic = money(safeMoney(basicSalary));
        epfEmployeeAmount = money(basic.multiply(epfEmployeePercent).divide(hundred, 2, RoundingMode.HALF_UP));
        epfEmployerAmount = money(basic.multiply(epfEmployerPercent).divide(hundred, 2, RoundingMode.HALF_UP));
        etfEmployerAmount = money(basic.multiply(etfEmployerPercent).divide(hundred, 2, RoundingMode.HALF_UP));

        // Net salary = gross - EPF employee - deductions
        netSalary = money(grossSalary.subtract(safeMoney(epfEmployeeAmount)).subtract(safeMoney(deductions)));

        if (generatedAt == null) {
            generatedAt = LocalDateTime.now();
        }
    }

    private static BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static BigDecimal money(BigDecimal value) {
        return safeMoney(value).setScale(2, RoundingMode.HALF_UP);
    }
}

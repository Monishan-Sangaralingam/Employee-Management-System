package com.employeesystem.emsbackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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
@Entity
@Table(name = "payroll_record")
public class PayrollRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(name = "pay_year", nullable = false)
    private Integer year;

    @Column(name = "pay_month", nullable = false)
    private Integer month;

    @Column(name = "base_salary", nullable = false, precision = 19, scale = 2)
    private BigDecimal baseSalary;

    @Column(name = "allowances", nullable = false, precision = 19, scale = 2)
    private BigDecimal allowances;

    @Column(name = "deductions", nullable = false, precision = 19, scale = 2)
    private BigDecimal deductions;

    @Column(name = "gross_pay", nullable = false, precision = 19, scale = 2)
    private BigDecimal grossPay;

    @Column(name = "epf_employee", nullable = false, precision = 19, scale = 2)
    private BigDecimal epfEmployee;

    @Column(name = "epf_employer", nullable = false, precision = 19, scale = 2)
    private BigDecimal epfEmployer;

    @Column(name = "net_pay", nullable = false, precision = 19, scale = 2)
    private BigDecimal netPay;

    @Lob
    @Column(name = "payslip_pdf", nullable = false)
    private byte[] payslipPdf;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}

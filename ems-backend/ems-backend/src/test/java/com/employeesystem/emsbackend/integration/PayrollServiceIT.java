package com.employeesystem.emsbackend.integration;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.PayrollRecord;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.PayrollRecordRepository;
import com.employeesystem.emsbackend.service.PayrollService;
import com.employeesystem.emsbackend.web.payroll.PayrollGenerateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PayrollServiceIT extends MySqlTestcontainersBase {

    @Autowired
    private PayrollService payrollService;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private PayrollRecordRepository payrollRecordRepository;

    @Test
    void generatePayroll_persistsRecordAndPdf() {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "firstName", "A");
        ReflectionTestUtils.setField(e, "lastName", "B");
        ReflectionTestUtils.setField(e, "email", "a@b.com");
        Employee emp = employeeRepository.save(e);

        PayrollGenerateRequest req = new PayrollGenerateRequest();
        ReflectionTestUtils.setField(req, "year", 2026);
        ReflectionTestUtils.setField(req, "month", 2);
        ReflectionTestUtils.setField(req, "baseSalary", new BigDecimal("1000"));
        ReflectionTestUtils.setField(req, "allowances", new BigDecimal("200"));
        ReflectionTestUtils.setField(req, "deductions", new BigDecimal("50"));

        PayrollRecord record = payrollService.generatePayroll(emp.getId(), req);

        assertThat(record.getId()).isNotNull();
        assertThat(record.getNetPay()).isNotNull();
        assertThat(record.getPayslipPdf()).isNotNull();
        assertThat(record.getPayslipPdf().length).isGreaterThan(200);

        assertThat(payrollRecordRepository.findById(record.getId())).isPresent();
    }
}

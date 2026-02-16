package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.PayrollRecord;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.PayrollRepository;
import com.employeesystem.emsbackend.repository.PayrollRecordRepository;
import com.employeesystem.emsbackend.web.payroll.PayrollGenerateRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayrollServiceTest {

    @Mock
    private PayrollRecordRepository payrollRecordRepository;
    @Mock
    private PayrollRepository payrollRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    @Test
    void generatePayroll_calculatesGrossEpfAndNet_andSavesPdf() {
        PayrollService service = new PayrollService(
                payrollRecordRepository,
                payrollRepository,
                employeeRepository,
                new BigDecimal("8"),
                new BigDecimal("12"));

        Employee emp = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(payrollRecordRepository.save(any(PayrollRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        PayrollGenerateRequest req = new PayrollGenerateRequest();
        ReflectionTestUtils.setField(req, "year", 2026);
        ReflectionTestUtils.setField(req, "month", 2);
        ReflectionTestUtils.setField(req, "baseSalary", new BigDecimal("1000"));
        ReflectionTestUtils.setField(req, "allowances", new BigDecimal("200"));
        ReflectionTestUtils.setField(req, "deductions", new BigDecimal("50"));

        PayrollRecord record = service.generatePayroll(1L, req);

        assertThat(record.getGrossPay()).isEqualByComparingTo(new BigDecimal("1200.00"));
        assertThat(record.getEpfEmployee()).isEqualByComparingTo(new BigDecimal("96.00"));
        assertThat(record.getEpfEmployer()).isEqualByComparingTo(new BigDecimal("144.00"));
        assertThat(record.getNetPay()).isEqualByComparingTo(new BigDecimal("1054.00"));
        assertThat(record.getPayslipPdf()).isNotNull();
        assertThat(record.getPayslipPdf().length).isGreaterThan(200);
        assertThat(record.getCreatedAt()).isNotNull();

        ArgumentCaptor<PayrollRecord> captor = ArgumentCaptor.forClass(PayrollRecord.class);
        verify(payrollRecordRepository).save(captor.capture());
        assertThat(captor.getValue().getEmployee().getId()).isEqualTo(1L);
    }

    @Test
    void generatePayroll_whenNegativeBaseSalary_throws() {
        PayrollService service = new PayrollService(
                payrollRecordRepository,
                payrollRepository,
                employeeRepository,
                new BigDecimal("8"),
                new BigDecimal("12"));

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L)));

        PayrollGenerateRequest req = new PayrollGenerateRequest();
        ReflectionTestUtils.setField(req, "year", 2026);
        ReflectionTestUtils.setField(req, "month", 2);
        ReflectionTestUtils.setField(req, "baseSalary", new BigDecimal("-1"));

        assertThatThrownBy(() -> service.generatePayroll(1L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("baseSalary cannot be negative");
    }

    private Employee employee(Long id) {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "id", id);
        ReflectionTestUtils.setField(e, "firstName", "A");
        ReflectionTestUtils.setField(e, "lastName", "B");
        ReflectionTestUtils.setField(e, "email", "a@b.com");
        return e;
    }
}

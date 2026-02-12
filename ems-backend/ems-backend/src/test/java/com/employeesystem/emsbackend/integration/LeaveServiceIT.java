package com.employeesystem.emsbackend.integration;

import com.employeesystem.emsbackend.entity.*;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.LeaveBalanceRepository;
import com.employeesystem.emsbackend.repository.LeaveRequestRepository;
import com.employeesystem.emsbackend.service.LeaveService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class LeaveServiceIT extends MySqlTestcontainersBase {

    @Autowired
    private LeaveService leaveService;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private LeaveBalanceRepository leaveBalanceRepository;
    @Autowired
    private LeaveRequestRepository leaveRequestRepository;

    @Test
    void applyAndApprove_deductsBalance() {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "firstName", "A");
        ReflectionTestUtils.setField(e, "lastName", "B");
        ReflectionTestUtils.setField(e, "email", "a@b.com");
        Employee emp = employeeRepository.save(e);

        LeaveBalance bal = new LeaveBalance();
        bal.setEmployee(emp);
        bal.setAnnualDays(10);
        bal.setSickDays(0);
        bal.setCasualDays(0);
        leaveBalanceRepository.save(bal);

        LeaveRequest req = leaveService.applyLeave(
                emp.getId(),
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 12),
                LeaveType.ANNUAL,
                "trip"
        );
        assertThat(req.getDays()).isEqualTo(3);

        LeaveRequest decided = leaveService.decide(req.getId(), LeaveStatus.APPROVED, "ok");
        assertThat(decided.getStatus()).isEqualTo(LeaveStatus.APPROVED);

        LeaveBalance after = leaveBalanceRepository.findByEmployeeId(emp.getId()).orElseThrow();
        assertThat(after.getAnnualDays()).isEqualTo(7);

        assertThat(leaveRequestRepository.findById(req.getId())).isPresent();
    }
}

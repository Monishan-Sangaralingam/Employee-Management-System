package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.*;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.LeaveBalanceRepository;
import com.employeesystem.emsbackend.repository.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private LeaveBalanceRepository leaveBalanceRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    @Test
    void applyLeave_calculatesInclusiveDays_andSavesPending() {
        LeaveService service = new LeaveService(leaveRequestRepository, leaveBalanceRepository, employeeRepository);
        Employee emp = employee(1L);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest saved = service.applyLeave(
                1L,
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 12),
                LeaveType.UNPAID,
                "note");

        assertThat(saved.getDays()).isEqualTo(3);
        assertThat(saved.getStatus()).isEqualTo(LeaveStatus.PENDING);
        assertThat(saved.getType()).isEqualTo(LeaveType.UNPAID);
        assertThat(saved.getAppliedAt()).isNotNull();
        verify(leaveRequestRepository).save(any(LeaveRequest.class));
    }

    @Test
    void applyLeave_whenInsufficientBalance_throws() {
        LeaveService service = new LeaveService(leaveRequestRepository, leaveBalanceRepository, employeeRepository);
        Employee emp = employee(1L);

        LeaveBalance bal = new LeaveBalance();
        bal.setEmployee(emp);
        bal.setAnnualDays(1);
        bal.setSickDays(0);
        bal.setCasualDays(0);

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(bal));

        assertThatThrownBy(() -> service.applyLeave(
                1L,
                LocalDate.of(2026, 2, 10),
                LocalDate.of(2026, 2, 12),
                LeaveType.ANNUAL,
                null)).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient leave balance");
    }

    @Test
    void decide_approved_deductsBalance() {
        LeaveService service = new LeaveService(leaveRequestRepository, leaveBalanceRepository, employeeRepository);
        Employee emp = employee(1L);

        LeaveRequest req = new LeaveRequest();
        req.setId(100L);
        req.setEmployee(emp);
        req.setStartDate(LocalDate.of(2026, 2, 10));
        req.setEndDate(LocalDate.of(2026, 2, 11));
        req.setDays(2);
        req.setType(LeaveType.ANNUAL);
        req.setStatus(LeaveStatus.PENDING);

        LeaveBalance bal = new LeaveBalance();
        bal.setEmployee(emp);
        bal.setAnnualDays(5);
        bal.setSickDays(0);
        bal.setCasualDays(0);

        when(leaveRequestRepository.findById(100L)).thenReturn(Optional.of(req));
        when(leaveBalanceRepository.findByEmployeeId(1L)).thenReturn(Optional.of(bal));
        when(leaveBalanceRepository.save(any(LeaveBalance.class))).thenAnswer(inv -> inv.getArgument(0));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest decided = service.decide(100L, LeaveStatus.APPROVED, "ok");

        assertThat(decided.getStatus()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(decided.getDecidedAt()).isNotNull();
        assertThat(bal.getAnnualDays()).isEqualTo(3);
        verify(leaveBalanceRepository).save(bal);

        ArgumentCaptor<LeaveRequest> captor = ArgumentCaptor.forClass(LeaveRequest.class);
        verify(leaveRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getDecisionNote()).isEqualTo("ok");
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

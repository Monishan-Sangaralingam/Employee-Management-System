package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.*;
import com.employeesystem.emsbackend.audit.Auditable;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.LeaveBalanceRepository;
import com.employeesystem.emsbackend.repository.LeaveRequestRepository;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class LeaveService {

    private static final Logger log = LoggerFactory.getLogger(LeaveService.class);

    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveBalanceRepository leaveBalanceRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    @Auditable(action = "CREATE", entity = "LeaveRequest")
    public LeaveRequest applyLeave(Long employeeId, LocalDate startDate, LocalDate endDate, LeaveType type, String note) {
        if (startDate == null || endDate == null || type == null) {
            throw new BadRequestException("startDate, endDate and type are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("endDate must be on/after startDate");
        }

        int days = Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
        if (days <= 0) {
            throw new BadRequestException("days must be positive");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Id " + employeeId + " not found"));

        if (type != LeaveType.UNPAID) {
            LeaveBalance balance = getOrCreateBalance(employee);
            int available = getAvailable(balance, type);
            if (available < days) {
                throw new BadRequestException("Insufficient leave balance for " + type + ": available=" + available + ", requested=" + days);
            }
        }

        LeaveRequest request = new LeaveRequest();
        request.setEmployee(employee);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setDays(days);
        request.setType(type);
        request.setStatus(LeaveStatus.PENDING);
        request.setNote(note);
        request.setAppliedAt(LocalDateTime.now());
        LeaveRequest saved = leaveRequestRepository.save(request);

        log.info("EMAIL STUB: Leave applied employeeId={}, leaveId={}, type={}, days={}", employeeId, saved.getId(), type, days);
        return saved;
    }

    @Transactional
    @Auditable(action = "UPDATE", entity = "LeaveRequest", entityIdArgIndex = 0)
    public LeaveRequest decide(Long leaveId, LeaveStatus status, String decisionNote) {
        if (status == null || (status != LeaveStatus.APPROVED && status != LeaveStatus.REJECTED)) {
            throw new BadRequestException("status must be APPROVED or REJECTED");
        }

        LeaveRequest request = leaveRequestRepository.findById(leaveId)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest Id " + leaveId + " not found"));

        if (request.getStatus() != LeaveStatus.PENDING) {
            throw new BadRequestException("LeaveRequest already decided");
        }

        request.setStatus(status);
        request.setDecisionNote(decisionNote);
        request.setDecidedAt(LocalDateTime.now());

        if (status == LeaveStatus.APPROVED && request.getType() != LeaveType.UNPAID) {
            LeaveBalance balance = getOrCreateBalance(request.getEmployee());
            int available = getAvailable(balance, request.getType());
            if (available < request.getDays()) {
                throw new BadRequestException("Insufficient leave balance at approval time for " + request.getType());
            }
            deduct(balance, request.getType(), request.getDays());
            leaveBalanceRepository.save(balance);
        }

        LeaveRequest saved = leaveRequestRepository.save(request);
        log.info("EMAIL STUB: Leave decision leaveId={}, employeeId={}, status={}", saved.getId(), saved.getEmployee().getId(), status);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getMyRequests(Long employeeId) {
        return leaveRequestRepository.findByEmployeeIdOrderByAppliedAtDesc(employeeId);
    }

    @Transactional(readOnly = true)
    public List<LeaveRequest> getPendingRequests() {
        return leaveRequestRepository.findByStatusOrderByAppliedAtDesc(LeaveStatus.PENDING);
    }

    private LeaveBalance getOrCreateBalance(Employee employee) {
        return leaveBalanceRepository.findByEmployeeId(employee.getId())
                .orElseGet(() -> {
                    LeaveBalance b = new LeaveBalance();
                    b.setEmployee(employee);
                    b.setAnnualDays(0);
                    b.setSickDays(0);
                    b.setCasualDays(0);
                    return leaveBalanceRepository.save(b);
                });
    }

    private int getAvailable(LeaveBalance balance, LeaveType type) {
        return switch (type) {
            case ANNUAL -> balance.getAnnualDays();
            case SICK -> balance.getSickDays();
            case CASUAL -> balance.getCasualDays();
            case UNPAID -> Integer.MAX_VALUE;
        };
    }

    private void deduct(LeaveBalance balance, LeaveType type, int days) {
        switch (type) {
            case ANNUAL -> balance.setAnnualDays(balance.getAnnualDays() - days);
            case SICK -> balance.setSickDays(balance.getSickDays() - days);
            case CASUAL -> balance.setCasualDays(balance.getCasualDays() - days);
            case UNPAID -> {
            }
        }
    }
}

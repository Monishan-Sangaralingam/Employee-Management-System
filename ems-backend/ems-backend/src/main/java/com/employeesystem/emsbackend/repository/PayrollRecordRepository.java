package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);
}

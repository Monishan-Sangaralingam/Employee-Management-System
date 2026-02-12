package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.PayrollRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface PayrollRecordRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByEmployeeIdOrderByYearDescMonthDesc(Long employeeId);

    @Query("select coalesce(sum(p.netPay), 0) from PayrollRecord p where p.year = :year and p.month = :month")
    BigDecimal sumNetPayForMonth(@Param("year") int year, @Param("month") int month);
}

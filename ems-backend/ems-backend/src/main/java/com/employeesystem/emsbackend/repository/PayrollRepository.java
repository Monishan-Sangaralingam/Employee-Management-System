package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    @Query("select p from Payroll p where p.employee.id = :employeeId and p.payrollMonth = :payrollMonth")
    Optional<Payroll> findByEmployeeIdAndPayrollMonth(
            @Param("employeeId") Long employeeId,
            @Param("payrollMonth") String payrollMonth);

    @Query("select p from Payroll p where p.employee.id = :employeeId")
    List<Payroll> findByEmployeeId(@Param("employeeId") Long employeeId);

    List<Payroll> findByPayrollMonth(String payrollMonth);
}

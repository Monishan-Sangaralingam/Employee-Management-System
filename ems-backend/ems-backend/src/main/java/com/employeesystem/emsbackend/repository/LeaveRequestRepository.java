package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.LeaveRequest;
import com.employeesystem.emsbackend.entity.LeaveStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(Long employeeId);

    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(LeaveStatus status);

    long countByStatus(LeaveStatus status);

    @Query("select r.status, count(r) from LeaveRequest r group by r.status")
    List<Object[]> countByStatusGrouped();
}

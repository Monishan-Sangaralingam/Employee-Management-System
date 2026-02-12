package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}

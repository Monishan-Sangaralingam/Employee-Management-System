package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.AuditLog;
import com.employeesystem.emsbackend.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog save(AuditLog log) {
        return auditLogRepository.save(log);
    }
}

package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    Optional<EmployeeDocument> findByIdAndEmployeeId(Long id, Long employeeId);
}

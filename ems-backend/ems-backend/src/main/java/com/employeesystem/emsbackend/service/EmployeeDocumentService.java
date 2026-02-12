package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.EmployeeDocument;
import com.employeesystem.emsbackend.audit.Auditable;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.EmployeeDocumentRepository;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

@Service
public class EmployeeDocumentService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeDocumentService.class);

    private final EmployeeRepository employeeRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final String uploadDir;

    public EmployeeDocumentService(EmployeeRepository employeeRepository,
                                   EmployeeDocumentRepository employeeDocumentRepository,
                                   @Value("${app.upload.dir}") String uploadDir) {
        this.employeeRepository = employeeRepository;
        this.employeeDocumentRepository = employeeDocumentRepository;
        this.uploadDir = uploadDir;
    }

    @Transactional
    @Auditable(action = "CREATE", entity = "EmployeeDocument")
    public EmployeeDocument upload(Long employeeId, MultipartFile file, String uploadedBy) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("file is required");
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Id " + employeeId + " not found"));

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String mime = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();

        log.warn("VIRUS SCAN PLACEHOLDER: scanning upload employeeId={}, filename={}, sizeBytes={}", employeeId, originalName, size);

        EmployeeDocument doc = new EmployeeDocument();
        doc.setEmployee(employee);
        doc.setFilename(originalName);
        doc.setMime(mime);
        doc.setSize(size);
        doc.setUploadedBy(uploadedBy == null ? "" : uploadedBy);
        doc.setUploadedAt(LocalDateTime.now());

        // Create DB record first to obtain ID for stable storage filename
        doc.setStoredFilename("PENDING");
        EmployeeDocument saved = employeeDocumentRepository.save(doc);

        String storedFilename = saved.getId() + "_" + originalName;
        Path employeeDir = Path.of(uploadDir, String.valueOf(employeeId));
        try {
            Files.createDirectories(employeeDir);
            Path target = employeeDir.resolve(storedFilename);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new BadRequestException("Failed to store file");
        }

        saved.setStoredFilename(storedFilename);
        return employeeDocumentRepository.save(saved);
    }

    public EmployeeDocument getMetadata(Long employeeId, Long documentId) {
        return employeeDocumentRepository.findByIdAndEmployeeId(documentId, employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
    }

    public Path resolveStoredPath(Long employeeId, EmployeeDocument doc) {
        return Path.of(uploadDir, String.valueOf(employeeId), doc.getStoredFilename());
    }

    private String sanitizeFilename(String filename) {
        String name = (filename == null || filename.isBlank()) ? "file" : filename;
        name = Path.of(name).getFileName().toString();
        // Keep it simple and safe for filesystem use
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name;
    }
}

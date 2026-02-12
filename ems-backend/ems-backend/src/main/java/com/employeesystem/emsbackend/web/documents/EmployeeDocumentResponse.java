package com.employeesystem.emsbackend.web.documents;

import com.employeesystem.emsbackend.entity.EmployeeDocument;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeDocumentResponse {
    private Long id;
    private Long employeeId;
    private String filename;
    private String mime;
    private Long size;
    private String uploadedBy;
    private LocalDateTime uploadedAt;

    public static EmployeeDocumentResponse from(EmployeeDocument d) {
        return new EmployeeDocumentResponse(
                d.getId(),
                d.getEmployee() != null ? d.getEmployee().getId() : null,
                d.getFilename(),
                d.getMime(),
                d.getSize(),
                d.getUploadedBy(),
                d.getUploadedAt()
        );
    }
}

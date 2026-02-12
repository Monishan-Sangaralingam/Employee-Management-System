package com.employeesystem.emsbackend.web.audit;

import com.employeesystem.emsbackend.entity.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long actorUserId;
    private String action;
    private String entity;
    private Long entityId;
    private LocalDateTime timestamp;
    private String details;

    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getActorUserId(),
                a.getAction(),
                a.getEntity(),
                a.getEntityId(),
                a.getTimestamp(),
                a.getDetails()
        );
    }
}

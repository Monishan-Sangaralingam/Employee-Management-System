package com.employeesystem.emsbackend.web.leave;

import com.employeesystem.emsbackend.entity.LeaveRequest;
import com.employeesystem.emsbackend.entity.LeaveStatus;
import com.employeesystem.emsbackend.entity.LeaveType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LeaveResponse {
    private Long id;
    private Long employeeId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer days;
    private LeaveType type;
    private LeaveStatus status;
    private String note;
    private String decisionNote;
    private LocalDateTime appliedAt;
    private LocalDateTime decidedAt;

    public static LeaveResponse from(LeaveRequest r) {
        return new LeaveResponse(
                r.getId(),
                r.getEmployee() != null ? r.getEmployee().getId() : null,
                r.getStartDate(),
                r.getEndDate(),
                r.getDays(),
                r.getType(),
                r.getStatus(),
                r.getNote(),
                r.getDecisionNote(),
                r.getAppliedAt(),
                r.getDecidedAt()
        );
    }
}

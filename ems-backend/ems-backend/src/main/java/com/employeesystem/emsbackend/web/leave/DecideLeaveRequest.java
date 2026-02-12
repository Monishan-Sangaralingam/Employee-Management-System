package com.employeesystem.emsbackend.web.leave;

import com.employeesystem.emsbackend.entity.LeaveStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DecideLeaveRequest {
    private LeaveStatus status;
    private String note;
}

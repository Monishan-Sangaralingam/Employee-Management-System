package com.employeesystem.emsbackend.web.attendance;

import com.employeesystem.emsbackend.entity.Attendance;
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
public class AttendanceResponse {
    private Long id;
    private Long employeeId;
    private LocalDate workDate;
    private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    private Long workedMinutes;

    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getId(),
                a.getEmployee() != null ? a.getEmployee().getId() : null,
                a.getWorkDate(),
                a.getCheckIn(),
                a.getCheckOut(),
                a.getWorkedMinutes()
        );
    }
}

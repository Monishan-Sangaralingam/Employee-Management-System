package com.employeesystem.emsbackend.web.attendance;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTotalsResponse {
    private Long employeeId;
    private int year;
    private int month;
    private Long totalWorkedMinutes;
}

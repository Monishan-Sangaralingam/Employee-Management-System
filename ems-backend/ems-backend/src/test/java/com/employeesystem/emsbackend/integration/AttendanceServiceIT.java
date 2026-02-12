package com.employeesystem.emsbackend.integration;

import com.employeesystem.emsbackend.entity.Attendance;
import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.repository.AttendanceRepository;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.service.AttendanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AttendanceServiceIT extends MySqlTestcontainersBase {

    @Autowired
    private AttendanceService attendanceService;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private AttendanceRepository attendanceRepository;

    @Test
    void checkInThenCheckOut_persistsAndClosesOpenEntry() {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "firstName", "A");
        ReflectionTestUtils.setField(e, "lastName", "B");
        ReflectionTestUtils.setField(e, "email", "a@b.com");
        Employee emp = employeeRepository.save(e);

        Attendance in = attendanceService.checkIn(emp.getId());
        assertThat(in.getId()).isNotNull();

        Attendance out = attendanceService.checkOut(emp.getId());
        assertThat(out.getCheckOut()).isNotNull();
        assertThat(out.getWorkedMinutes()).isGreaterThanOrEqualTo(0L);

        assertThat(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(emp.getId()))
                .isEmpty();
    }
}

package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Attendance;
import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.AttendanceRepository;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Test
    void checkIn_whenEmployeeMissing_throws() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkIn(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee Id 1");
    }

    @Test
    void checkIn_whenOpenAttendanceExists_throws() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee(1L)));
        when(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(1L))
                .thenReturn(Optional.of(new Attendance()));

        assertThatThrownBy(() -> service.checkIn(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot check-in twice");
    }

    @Test
    void checkIn_savesAttendanceWithWorkedMinutesZero() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        Employee emp = employee(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(emp));
        when(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(1L))
                .thenReturn(Optional.empty());
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = service.checkIn(1L);

        assertThat(result.getEmployee().getId()).isEqualTo(1L);
        assertThat(result.getWorkDate()).isEqualTo(LocalDate.now());
        assertThat(result.getCheckIn()).isNotNull();
        assertThat(result.getWorkedMinutes()).isEqualTo(0L);
    }

    @Test
    void checkOut_whenNoOpenAttendance_throws() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        when(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkOut(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No active check-in");
    }

    @Test
    void checkOut_whenWorkDateNotToday_throws() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        Attendance open = new Attendance();
        open.setWorkDate(LocalDate.now().minusDays(1));
        open.setCheckIn(LocalDateTime.now().minusHours(1));
        when(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(1L))
                .thenReturn(Optional.of(open));

        assertThatThrownBy(() -> service.checkOut(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("previous day");
    }

    @Test
    void checkOut_calculatesWorkedMinutes() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        Attendance open = new Attendance();
        open.setWorkDate(LocalDate.now());
        LocalDateTime now = LocalDateTime.now();
        open.setCheckIn(now.minusMinutes(90));

        when(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(1L))
                .thenReturn(Optional.of(open));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = service.checkOut(1L);

        assertThat(result.getCheckOut()).isNotNull();
        assertThat(result.getWorkedMinutes()).isEqualTo(90L);
    }

    @Test
    void checkOut_neverReturnsNegativeMinutes() {
        AttendanceService service = new AttendanceService(attendanceRepository, employeeRepository);
        Attendance open = new Attendance();
        open.setWorkDate(LocalDate.now());
        open.setCheckIn(LocalDateTime.now().plusMinutes(1));

        when(attendanceRepository.findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(1L))
                .thenReturn(Optional.of(open));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = service.checkOut(1L);

        assertThat(result.getWorkedMinutes()).isEqualTo(0L);
    }

    private Employee employee(Long id) {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "id", id);
        ReflectionTestUtils.setField(e, "firstName", "A");
        ReflectionTestUtils.setField(e, "lastName", "B");
        ReflectionTestUtils.setField(e, "email", "a@b.com");
        return e;
    }
}

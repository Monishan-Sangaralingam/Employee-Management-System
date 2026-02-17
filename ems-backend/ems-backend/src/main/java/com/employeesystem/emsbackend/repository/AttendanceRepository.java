package com.employeesystem.emsbackend.repository;

import com.employeesystem.emsbackend.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findTopByEmployeeIdAndCheckOutIsNullOrderByCheckInDesc(Long employeeId);

    Optional<Attendance> findTopByEmployeeIdAndWorkDateOrderByCheckInDesc(Long employeeId, LocalDate workDate);

    List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByCheckInAsc(Long employeeId, LocalDate start,
            LocalDate end);

    @Query("select coalesce(sum(a.workedMinutes), 0) from Attendance a where a.employee.id = :employeeId and a.workDate between :start and :end")
    Long sumWorkedMinutesForEmployeeBetween(@Param("employeeId") Long employeeId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("select count(distinct a.employee.id) from Attendance a where a.workDate between :start and :end")
    long countDistinctEmployeesActiveBetween(@Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query("select avg(a.workedMinutes) from Attendance a where a.workDate between :start and :end and a.workedMinutes is not null")
    Double avgWorkedMinutesBetween(@Param("start") LocalDate start,
            @Param("end") LocalDate end);
}

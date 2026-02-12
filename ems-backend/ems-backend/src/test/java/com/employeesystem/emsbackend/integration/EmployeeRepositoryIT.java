package com.employeesystem.emsbackend.integration;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class EmployeeRepositoryIT extends MySqlTestcontainersBase {

    @Autowired
    private EmployeeRepository employeeRepository;

    @Test
    void saveAndFindByEmail_roundTrip() {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "firstName", "A");
        ReflectionTestUtils.setField(e, "lastName", "B");
        ReflectionTestUtils.setField(e, "email", "a@b.com");
        Employee saved = employeeRepository.save(e);

        Employee found = employeeRepository.findByEmail("a@b.com");

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getFirstName()).isEqualTo("A");
    }
}

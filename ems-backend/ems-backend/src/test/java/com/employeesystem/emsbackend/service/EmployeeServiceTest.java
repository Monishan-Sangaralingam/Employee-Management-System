package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Test
    void addEmployee_savesAndReturns() {
        EmployeeService service = new EmployeeService(employeeRepository);

        Employee input = employee(null, "A", "B", "a@b.com");
        Employee saved = employee(10L, "A", "B", "a@b.com");

        when(employeeRepository.save(any(Employee.class))).thenReturn(saved);

        Employee result = service.addEmployee(input);

        assertThat(result.getId()).isEqualTo(10L);
        verify(employeeRepository).save(input);
    }

    @Test
    void findEmployeeById_whenMissing_throws() {
        EmployeeService service = new EmployeeService(employeeRepository);

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findEmployeeById(99L))
                .isInstanceOf(com.employeesystem.emsbackend.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Employee Id 99");
    }

    @Test
    void getAllEmployee_returnsFromRepo() {
        EmployeeService service = new EmployeeService(employeeRepository);

        when(employeeRepository.findAll()).thenReturn(Arrays.asList(
                employee(1L, "A", "B", "a@b.com"),
                employee(2L, "C", "D", "c@d.com")));

        List<Employee> result = service.getAllEmployee();

        assertThat(result).hasSize(2);
        verify(employeeRepository).findAll();
    }

    @Test
    void updateEmployee_updatesFieldsAndSaves() {
        EmployeeService service = new EmployeeService(employeeRepository);

        Employee existing = employee(5L, "Old", "Name", "old@x.com");
        Employee update = employee(null, "New", "Name2", "new@x.com");

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));

        Employee result = service.updateEmployee(5L, update);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getFirstName()).isEqualTo("New");
        assertThat(result.getLastName()).isEqualTo("Name2");
        assertThat(result.getEmail()).isEqualTo("new@x.com");

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("new@x.com");
    }

    @Test
    void deleteEmployeeById_whenNotExists_throws() {
        EmployeeService service = new EmployeeService(employeeRepository);

        when(employeeRepository.existsById(7L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteEmployeeById(7L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Employee not found");

        verify(employeeRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteEmployeeById_whenExists_deletes() {
        EmployeeService service = new EmployeeService(employeeRepository);

        when(employeeRepository.existsById(7L)).thenReturn(true);

        service.deleteEmployeeById(7L);

        verify(employeeRepository).deleteById(7L);
    }

    private Employee employee(Long id, String firstName, String lastName, String email) {
        Employee e = new Employee();
        ReflectionTestUtils.setField(e, "id", id);
        ReflectionTestUtils.setField(e, "firstName", firstName);
        ReflectionTestUtils.setField(e, "lastName", lastName);
        ReflectionTestUtils.setField(e, "email", email);
        return e;
    }
}

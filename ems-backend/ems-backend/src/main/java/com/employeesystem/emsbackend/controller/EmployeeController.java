package com.employeesystem.emsbackend.controller;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.EmployeeDocument;
import com.employeesystem.emsbackend.security.UserPrincipal;
import com.employeesystem.emsbackend.service.EmployeeService;
import com.employeesystem.emsbackend.service.EmployeeDocumentService;
import com.employeesystem.emsbackend.web.documents.EmployeeDocumentResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping(path = "/api/emp")
@AllArgsConstructor
public class EmployeeController {
    private final EmployeeService employeeService;
    private final EmployeeDocumentService employeeDocumentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<Employee> createEmployee(@RequestBody Employee employee) {
        Employee emp = employeeService.addEmployee(employee);
        return new ResponseEntity<>(emp, HttpStatus.CREATED);
    }

    @GetMapping(path = "/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or #id == principal.employee.id")
    public ResponseEntity<Employee> findEmployeeById(@PathVariable("id") Long id) {
        Employee emp = employeeService.findEmployeeById(id);
        return ResponseEntity.ok(emp);
    }

    @GetMapping
    public ResponseEntity<List<Employee>> getAllEmployee() {
        List<Employee> e = employeeService.getAllEmployee();
        return ResponseEntity.ok(e);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or #id == principal.employee.id")
    public ResponseEntity<Employee> updateEmployee(@PathVariable("id") Long id,
            @RequestBody Employee updateEmployee) {
        Employee emp = employeeService.updateEmployee(id, updateEmployee);
        return ResponseEntity.ok(emp);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<String> deleteById(@PathVariable("id") Long id) {
        employeeService.deleteEmployeeById(id);
        return ResponseEntity.ok("Employee Deleted Successfully");
    }

    // @GetMapping("/{email}")
    // public ResponseEntity<Employee> findFirstNameAndEmail(@RequestBody String
    // firstname,@RequestBody String email){
    // Employee emp = employeeService.findFirstNameAndEmail(firstname,email);
    // return ResponseEntity.ok(emp);
    // }
    @GetMapping("/email-id/{mail}")
    public ResponseEntity<Employee> findByEmployeeEmail(@PathVariable("mail") String email) {
        return ResponseEntity.ok(employeeService.findEmployeeByEmail(email));
    }

    @PostMapping(path = "/{id}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or #id == principal.employee.id")
    public ResponseEntity<EmployeeDocumentResponse> uploadDocument(@PathVariable("id") Long id,
                                                                   @RequestParam("file") MultipartFile file,
                                                                   @AuthenticationPrincipal UserPrincipal principal) {
        String uploadedBy = principal != null ? principal.getUsername() : "";
        EmployeeDocument saved = employeeDocumentService.upload(id, file, uploadedBy);
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeDocumentResponse.from(saved));
    }

    @GetMapping(path = "/{id}/documents/{docId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR') or #id == principal.employee.id")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable("id") Long id,
                                                   @PathVariable("docId") Long docId) {
        EmployeeDocument doc = employeeDocumentService.getMetadata(id, docId);
        Path path = employeeDocumentService.resolveStoredPath(id, doc);
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(doc.getMime()));
        headers.setContentDisposition(ContentDisposition.attachment().filename(doc.getFilename()).build());

        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}

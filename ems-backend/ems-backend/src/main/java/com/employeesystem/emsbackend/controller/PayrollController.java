package com.employeesystem.emsbackend.controller;

import com.employeesystem.emsbackend.entity.Payroll;
import com.employeesystem.emsbackend.entity.PayrollRecord;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.PayrollRepository;
import com.employeesystem.emsbackend.service.PayrollService;
import com.employeesystem.emsbackend.service.PayslipService;
import com.employeesystem.emsbackend.web.payroll.PayrollEntityResponse;
import com.employeesystem.emsbackend.web.payroll.PayrollGenerateRequest;
import com.employeesystem.emsbackend.web.payroll.PayrollGenerateSimpleRequest;
import com.employeesystem.emsbackend.web.payroll.PayrollResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/payroll")
@AllArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollRepository payrollRepository;
    private final PayslipService payslipService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<PayrollEntityResponse> generateSimple(@RequestBody PayrollGenerateSimpleRequest request) {
        Payroll payroll = payrollService.generatePayroll(
                request.getEmployeeId(),
                request.getPayrollMonth(),
                request.getBasicSalary(),
                request.getAllowances(),
                request.getDeductions());
        return ResponseEntity.ok(PayrollEntityResponse.from(payroll));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<java.util.List<PayrollEntityResponse>> byEmployee(@PathVariable Long employeeId) {
        java.util.List<PayrollEntityResponse> rows = payrollRepository.findByEmployeeId(employeeId)
                .stream()
                .map(PayrollEntityResponse::from)
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/employee/{employeeId}/{month}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<PayrollEntityResponse> byEmployeeAndMonth(
            @PathVariable Long employeeId,
            @PathVariable("month") String payrollMonth) {
        Payroll payroll = payrollRepository.findByEmployeeIdAndPayrollMonth(employeeId, payrollMonth)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll not found for employeeId=" + employeeId + " and payrollMonth=" + payrollMonth));
        return ResponseEntity.ok(PayrollEntityResponse.from(payroll));
    }

    @PostMapping("/employee/{employeeId}/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<PayrollResponse> generate(@PathVariable Long employeeId,
            @RequestBody PayrollGenerateRequest request) {
        PayrollRecord record = payrollService.generatePayroll(employeeId, request);
        return ResponseEntity.ok(PayrollResponse.from(record));
    }

    @PostMapping("/generate/{employeeId}/{period}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<PayrollResponse> generateForPeriod(@PathVariable Long employeeId,
            @PathVariable String period,
            @RequestBody PayrollGenerateRequest request) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(period, DateTimeFormatter.ofPattern("yyyy-MM"));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        request.setYear(ym.getYear());
        request.setMonth(ym.getMonthValue());
        if (request.getBaseSalary() == null) {
            request.setBaseSalary(BigDecimal.ZERO);
        }

        PayrollRecord record = payrollService.generatePayroll(employeeId, request);
        return ResponseEntity.ok(PayrollResponse.from(record));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") Long id) {
        PayrollRecord record = payrollService.getPayroll(id);

        String filename = "payslip-emp-" + record.getEmployee().getId() + "-" + record.getYear() + "-"
                + String.format("%02d", record.getMonth()) + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(record.getPayslipPdf());
    }

    @GetMapping("/payslip/{employeeId}/{month}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<byte[]> downloadPayslipForEmployeeAndMonth(
            @PathVariable Long employeeId,
            @PathVariable("month") String payrollMonth) {
        byte[] pdf = payslipService.generateAndSavePayslip(employeeId, payrollMonth);

        String filename = employeeId + "-" + payrollMonth + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}

package com.employeesystem.emsbackend.controller;

import com.employeesystem.emsbackend.entity.PayrollRecord;
import com.employeesystem.emsbackend.service.PayrollService;
import com.employeesystem.emsbackend.web.payroll.PayrollGenerateRequest;
import com.employeesystem.emsbackend.web.payroll.PayrollResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/payroll")
@AllArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping("/employee/{employeeId}/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<PayrollResponse> generate(@PathVariable Long employeeId,
                                                    @RequestBody PayrollGenerateRequest request) {
        PayrollRecord record = payrollService.generatePayroll(employeeId, request);
        return ResponseEntity.ok(PayrollResponse.from(record));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable("id") Long id) {
        PayrollRecord record = payrollService.getPayroll(id);

        String filename = "payslip-emp-" + record.getEmployee().getId() + "-" + record.getYear() + "-" + String.format("%02d", record.getMonth()) + ".pdf";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

        return ResponseEntity.ok()
                .headers(headers)
                .body(record.getPayslipPdf());
    }
}

package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.PayrollRecord;
import com.employeesystem.emsbackend.audit.Auditable;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.EmployeeRepository;
import com.employeesystem.emsbackend.repository.PayrollRecordRepository;
import com.employeesystem.emsbackend.web.payroll.PayrollGenerateRequest;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class PayrollService {

    private final PayrollRecordRepository payrollRecordRepository;
    private final EmployeeRepository employeeRepository;

    private final BigDecimal epfEmployeePercent;
    private final BigDecimal epfEmployerPercent;

    public PayrollService(PayrollRecordRepository payrollRecordRepository,
                          EmployeeRepository employeeRepository,
                          @Value("${app.payroll.epf-employee-percent}") BigDecimal epfEmployeePercent,
                          @Value("${app.payroll.epf-employer-percent}") BigDecimal epfEmployerPercent) {
        this.payrollRecordRepository = payrollRecordRepository;
        this.employeeRepository = employeeRepository;
        this.epfEmployeePercent = epfEmployeePercent;
        this.epfEmployerPercent = epfEmployerPercent;
    }

    @Transactional
    @Auditable(action = "CREATE", entity = "PayrollRecord")
    public PayrollRecord generatePayroll(Long employeeId, PayrollGenerateRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee Id " + employeeId + " not found"));

        int year = request.getYear() != null ? request.getYear() : LocalDate.now().getYear();
        int month = request.getMonth() != null ? request.getMonth() : LocalDate.now().getMonthValue();
        if (month < 1 || month > 12) {
            throw new BadRequestException("month must be 1-12");
        }

        BigDecimal baseSalary = requireMoney(request.getBaseSalary(), "baseSalary");
        BigDecimal allowances = defaultMoney(request.getAllowances());
        BigDecimal deductions = defaultMoney(request.getDeductions());

        BigDecimal grossPay = baseSalary.add(allowances).setScale(2, RoundingMode.HALF_UP);

        BigDecimal epfEmployee = percentOf(grossPay, epfEmployeePercent);
        BigDecimal epfEmployer = percentOf(grossPay, epfEmployerPercent);

        BigDecimal netPay = grossPay
                .subtract(deductions)
                .subtract(epfEmployee)
                .setScale(2, RoundingMode.HALF_UP);

        byte[] pdfBytes = generatePayslipPdf(employee, year, month, baseSalary, allowances, deductions, grossPay, epfEmployee, epfEmployer, netPay);

        PayrollRecord record = new PayrollRecord();
        record.setEmployee(employee);
        record.setYear(year);
        record.setMonth(month);
        record.setBaseSalary(baseSalary);
        record.setAllowances(allowances);
        record.setDeductions(deductions);
        record.setGrossPay(grossPay);
        record.setEpfEmployee(epfEmployee);
        record.setEpfEmployer(epfEmployer);
        record.setNetPay(netPay);
        record.setPayslipPdf(pdfBytes);
        record.setCreatedAt(LocalDateTime.now());

        return payrollRecordRepository.save(record);
    }

    public PayrollRecord getPayroll(Long payrollId) {
        return payrollRecordRepository.findById(payrollId)
                .orElseThrow(() -> new ResourceNotFoundException("PayrollRecord Id " + payrollId + " not found"));
    }

    private BigDecimal percentOf(BigDecimal amount, BigDecimal percent) {
        if (percent == null) {
            throw new BadRequestException("EPF percent not configured");
        }
        return amount
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal requireMoney(BigDecimal value, String field) {
        if (value == null) {
            throw new BadRequestException(field + " is required");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException(field + " cannot be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("amount cannot be negative");
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private byte[] generatePayslipPdf(Employee employee,
                                      int year,
                                      int month,
                                      BigDecimal baseSalary,
                                      BigDecimal allowances,
                                      BigDecimal deductions,
                                      BigDecimal grossPay,
                                      BigDecimal epfEmployee,
                                      BigDecimal epfEmployer,
                                      BigDecimal netPay) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float y = 770;
                content.beginText();
                content.setFont(PDType1Font.HELVETICA_BOLD, 16);
                content.newLineAtOffset(50, y);
                content.showText("Payslip");
                content.endText();

                y -= 30;
                writeLine(content, 50, y, "Employee ID: " + employee.getId());
                y -= 18;
                writeLine(content, 50, y, "Employee: " + safe(employee.getFirstName()) + " " + safe(employee.getLastName()));
                y -= 18;
                writeLine(content, 50, y, "Email: " + safe(employee.getEmail()));
                y -= 18;
                writeLine(content, 50, y, "Period: " + year + "-" + String.format("%02d", month));

                y -= 28;
                writeLine(content, 50, y, "Base Salary: " + baseSalary);
                y -= 18;
                writeLine(content, 50, y, "Allowances: " + allowances);
                y -= 18;
                writeLine(content, 50, y, "Deductions: " + deductions);
                y -= 18;
                writeLine(content, 50, y, "Gross Pay: " + grossPay);

                y -= 28;
                writeLine(content, 50, y, "EPF (Employee " + epfEmployeePercent + "%): " + epfEmployee);
                y -= 18;
                writeLine(content, 50, y, "EPF (Employer " + epfEmployerPercent + "%): " + epfEmployer);

                y -= 28;
                writeLineBold(content, 50, y, "Net Pay: " + netPay);
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate payslip PDF");
        }
    }

    private void writeLine(PDPageContentStream content, float x, float y, String text) throws Exception {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA, 12);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private void writeLineBold(PDPageContentStream content, float x, float y, String text) throws Exception {
        content.beginText();
        content.setFont(PDType1Font.HELVETICA_BOLD, 12);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

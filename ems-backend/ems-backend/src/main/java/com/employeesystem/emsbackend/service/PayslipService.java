package com.employeesystem.emsbackend.service;

import com.employeesystem.emsbackend.entity.Employee;
import com.employeesystem.emsbackend.entity.Payroll;
import com.employeesystem.emsbackend.exception.BadRequestException;
import com.employeesystem.emsbackend.exception.ResourceNotFoundException;
import com.employeesystem.emsbackend.repository.PayrollRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class PayslipService {

    private final PayrollRepository payrollRepository;
    private final String companyName;
    private final Path payslipDir;

    public PayslipService(
            PayrollRepository payrollRepository,
            @Value("${app.company.name:Employee Management System}") String companyName,
            @Value("${app.payslips.dir:/data/payslips}") String payslipDir) {
        this.payrollRepository = payrollRepository;
        this.companyName = companyName;
        this.payslipDir = Paths.get(payslipDir);
    }

    @Transactional(readOnly = true)
    public byte[] generateAndSavePayslip(Long employeeId, String payrollMonth) {
        if (employeeId == null) {
            throw new BadRequestException("employeeId is required");
        }
        if (payrollMonth == null || payrollMonth.isBlank() || !payrollMonth.matches("\\d{4}-\\d{2}")) {
            throw new BadRequestException("month must be in format YYYY-MM");
        }

        Payroll payroll = payrollRepository.findByEmployeeIdAndPayrollMonth(employeeId, payrollMonth)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Payroll not found for employeeId=" + employeeId + " and payrollMonth=" + payrollMonth));

        byte[] pdf = generatePayslipPdf(payroll);
        writeToDisk(employeeId, payrollMonth, pdf);
        return pdf;
    }

    private void writeToDisk(Long employeeId, String payrollMonth, byte[] pdf) {
        try {
            Files.createDirectories(payslipDir);
            Path target = payslipDir.resolve(employeeId + "-" + payrollMonth + ".pdf");
            Files.write(target, pdf);
        } catch (IOException e) {
            throw new BadRequestException("Failed to save payslip PDF to disk");
        }
    }

    private byte[] generatePayslipPdf(Payroll payroll) {
        Employee employee = payroll.getEmployee();
        String employeeName = employee != null
                ? (safe(employee.getFirstName()) + " " + safe(employee.getLastName())).trim()
                : "";
        String employeeId = employee != null && employee.getId() != null ? String.valueOf(employee.getId()) : "";

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float left = 50;
                float y = 780;

                // Header
                y = writeText(content, PDType1Font.HELVETICA_BOLD, 18, left, y, companyName);
                y = y - 6;
                drawLine(content, left, y, 545, y);
                y = y - 18;

                y = writeLabelValue(content, left, y, "Employee Name", employeeName);
                y = writeLabelValue(content, left, y, "Employee ID", employeeId);
                y = writeLabelValue(content, left, y, "Payroll Month", safe(payroll.getPayrollMonth()));

                y = y - 14;
                y = writeText(content, PDType1Font.HELVETICA_BOLD, 13, left, y, "Salary Breakdown");
                y = y - 10;

                y = writeMoneyLine(content, left, y, "Basic Salary", payroll.getBasicSalary());
                y = writeMoneyLine(content, left, y, "Allowances", payroll.getAllowances());
                y = writeMoneyLine(content, left, y, "Gross Salary", payroll.getGrossSalary());

                y = y - 10;
                y = writeText(content, PDType1Font.HELVETICA_BOLD, 13, left, y, "Statutory Contributions");
                y = y - 10;

                y = writeMoneyLine(content, left, y, "EPF Employee (8%)", payroll.getEpfEmployeeAmount());
                y = writeMoneyLine(content, left, y, "EPF Employer (12%)", payroll.getEpfEmployerAmount());
                y = writeMoneyLine(content, left, y, "ETF Employer (3%)", payroll.getEtfEmployerAmount());

                y = y - 10;
                y = writeMoneyLine(content, left, y, "Deductions", payroll.getDeductions());

                y = y - 14;
                drawLine(content, left, y, 545, y);
                y = y - 18;

                // Net salary highlighted
                y = writeMoneyLineBold(content, left, y, "Net Salary", payroll.getNetSalary());

                y = y - 22;
                writeText(content, PDType1Font.HELVETICA_OBLIQUE, 9, left, y,
                        "This payslip is system generated.");
            }

            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new BadRequestException("Failed to generate payslip PDF");
        }
    }

    private float writeLabelValue(PDPageContentStream content, float left, float y, String label, String value)
            throws IOException {
        writeText(content, PDType1Font.HELVETICA_BOLD, 11, left, y, label + ":");
        writeText(content, PDType1Font.HELVETICA, 11, left + 120, y, safe(value));
        return y - 16;
    }

    private float writeMoneyLine(PDPageContentStream content, float left, float y, String label, BigDecimal value)
            throws IOException {
        writeText(content, PDType1Font.HELVETICA, 11, left, y, label);
        writeText(content, PDType1Font.HELVETICA, 11, 450, y, formatMoney(value));
        return y - 16;
    }

    private float writeMoneyLineBold(PDPageContentStream content, float left, float y, String label, BigDecimal value)
            throws IOException {
        writeText(content, PDType1Font.HELVETICA_BOLD, 12, left, y, label);
        writeText(content, PDType1Font.HELVETICA_BOLD, 12, 450, y, formatMoney(value));
        return y - 18;
    }

    private float writeText(PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - (fontSize + 4);
    }

    private void drawLine(PDPageContentStream content, float x1, float y1, float x2, float y2) throws IOException {
        content.moveTo(x1, y1);
        content.lineTo(x2, y2);
        content.stroke();
    }

    private String formatMoney(BigDecimal v) {
        if (v == null)
            return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

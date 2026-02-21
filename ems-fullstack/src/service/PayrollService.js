import { api } from './axiosInstance';

export async function generatePayroll(employeeId, periodYYYYMM, allowances, deductions) {
  const response = await api.post(`/api/payroll/generate/${employeeId}/${periodYYYYMM}`, {
    allowances,
    deductions,
  });
  return response.data;
}

export async function downloadPayslipPdf(payrollId) {
  const response = await api.get(`/api/payroll/${payrollId}/pdf`, {
    responseType: 'blob',
  });
  return {
    blob: response.data,
    headers: response.headers,
  };
}

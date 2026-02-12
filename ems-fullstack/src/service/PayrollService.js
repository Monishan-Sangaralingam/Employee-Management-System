import axios from 'axios';

import { getToken } from './AuthService';

const api = axios.create({
  baseURL: 'http://localhost:8080',
});

api.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

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

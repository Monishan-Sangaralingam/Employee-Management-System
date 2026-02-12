import { api } from './axiosInstance';

export async function checkIn() {
  const response = await api.post('/api/attendance/checkin');
  return response.data;
}

export async function checkOut() {
  const response = await api.post('/api/attendance/checkout');
  return response.data;
}

export async function getToday(employeeId) {
  const response = await api.get(`/api/attendance/employee/${employeeId}`, {
    params: { date: 'today' },
  });
  return response.data;
}

export async function getMonthly(employeeId, yyyyMM) {
  const response = await api.get(`/api/attendance/monthly/${yyyyMM}`, {
    params: employeeId == null ? undefined : { employeeId },
  });
  return response.data;
}

export function formatMinutesToHHMM(minutes) {
  const total = Number.isFinite(minutes) ? Math.max(0, Math.floor(minutes)) : 0;
  const hh = String(Math.floor(total / 60)).padStart(2, '0');
  const mm = String(total % 60).padStart(2, '0');
  return `${hh}:${mm}`;
}

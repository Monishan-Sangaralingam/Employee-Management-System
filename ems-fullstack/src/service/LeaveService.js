import { api } from './axiosInstance';

/**
 * Apply for leave.
 *
 * Server response: a LeaveResponse object { id, employeeId, startDate, endDate, days, type, status, note, appliedAt, ... }
 *
 * Note: backend infers the employee from the JWT for /api/leave/me/apply.
 * The `employeeId` parameter is accepted for convenience, but may be ignored unless backend supports alias.
 *
 * @param {Object} params
 * @param {number} [params.employeeId] - Optional employee ID (alias endpoint may support this).
 * @param {string} params.startDate - yyyy-MM-dd
 * @param {string} params.endDate - yyyy-MM-dd
 * @param {'ANNUAL'|'SICK'|'CASUAL'|'UNPAID'} params.type
 * @param {string} [params.note]
 * @returns {Promise<any>} Parsed response data
 */
export async function applyLeave({ employeeId, startDate, endDate, type, note }) {
  // Prefer backend canonical endpoint (employee inferred from token)
  const response = await api.post('/api/leave/me/apply', {
    startDate,
    endDate,
    type,
    note,
    // safe even if backend ignores unknown fields
    employeeId,
  });
  return response.data;
}

/**
 * Get the authenticated user's leave requests.
 *
 * Server response: LeaveResponse[]
 * @param {number} [employeeId] - Optional (backend infers from token for /api/leave/me)
 * @returns {Promise<any[]>}
 */
export async function getMyLeaves(employeeId) {
  const response = await api.get('/api/leave/me', {
    params: employeeId == null ? undefined : { employeeId },
  });
  return response.data;
}

/**
 * Get pending leave requests for approval (MANAGER/HR).
 *
 * Server response: LeaveResponse[]
 * @returns {Promise<any[]>}
 */
export async function getPending() {
  const response = await api.get('/api/leave/pending');
  return response.data;
}

/**
 * Decide a leave request.
 *
 * Server response: LeaveResponse
 * @param {number|string} leaveId
 * @param {Object} decision
 * @param {'APPROVED'|'REJECTED'} decision.status
 * @param {string} [decision.note]
 * @returns {Promise<any>}
 */
export async function decideLeave(leaveId, { status, note }) {
  const response = await api.put(`/api/leave/${leaveId}/decide`, { status, note });
  return response.data;
}

/**
 * Cancel a pending leave request (employee-owned).
 *
 * Server response: { message: string }
 * @param {number|string} leaveId
 * @returns {Promise<any>}
 */
export async function cancelLeave(leaveId) {
  const response = await api.delete(`/api/leave/${leaveId}/cancel`);
  return response.data;
}

import { describe, expect, it, vi } from 'vitest';

const post = vi.fn(async () => ({ data: { ok: true } }));
const get = vi.fn(async () => ({ data: { ok: true } }));

vi.mock('./axiosInstance', () => ({
    api: {
        post,
        get,
    },
}));

describe('AttendanceService', () => {
    it('checkIn() posts to /api/attendance/checkin', async () => {
        const { checkIn } = await import('./AttendanceService');
        await checkIn();
        expect(post).toHaveBeenCalledWith('/api/attendance/checkin');
    });

    it('checkOut() posts to /api/attendance/checkout', async () => {
        const { checkOut } = await import('./AttendanceService');
        await checkOut();
        expect(post).toHaveBeenCalledWith('/api/attendance/checkout');
    });

    it('getToday(employeeId) gets /api/attendance/employee/{employeeId}?date=today', async () => {
        const { getToday } = await import('./AttendanceService');
        await getToday(123);
        expect(get).toHaveBeenCalledWith('/api/attendance/employee/123', {
            params: { date: 'today' },
        });
    });

    it('getMonthly(employeeId, yyyyMM) gets /api/attendance/monthly/{yyyy-MM}', async () => {
        const { getMonthly } = await import('./AttendanceService');
        await getMonthly(123, '2026-02');
        expect(get).toHaveBeenCalledWith('/api/attendance/monthly/2026-02', {
            params: { employeeId: 123 },
        });
    });
});

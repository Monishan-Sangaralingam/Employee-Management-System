import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { act } from 'react';

import Attendance from './Attendance';
import { ToastProvider } from './ToastProvider';

vi.mock('../service/AuthService', () => ({
    getEmployeeIdFromToken: () => 1,
}));

vi.mock('../service/AttendanceService', () => ({
    checkIn: vi.fn(async () => ({ ok: true })),
    checkOut: vi.fn(async () => ({ ok: true })),
    getToday: vi.fn(async () => null),
    getMonthly: vi.fn(async () => []),
    formatMinutesToHHMM: (m) => String(m),
}));

describe('Attendance', () => {
    it('renders attendance UI', async () => {
        render(
            <ToastProvider>
                <Attendance />
            </ToastProvider>
        );

        await act(async () => {
            await Promise.resolve();
        });

        expect(screen.getByRole('heading', { name: /attendance/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /check in/i })).toBeInTheDocument();
        expect(screen.getByRole('button', { name: /check out/i })).toBeInTheDocument();
    });
});

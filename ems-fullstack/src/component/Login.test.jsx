import React from 'react';
import { describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';

import Login from './Login';
import { ToastProvider } from './ToastProvider';

vi.mock('../service/AuthService', () => ({
    login: vi.fn(),
}));

import { login } from '../service/AuthService';

function renderAtLogin() {
    return render(
        <ToastProvider>
            <MemoryRouter initialEntries={['/login']}>
                <Routes>
                    <Route path="/login" element={<Login />} />
                    <Route path="/dashboard" element={<div>Dashboard</div>} />
                </Routes>
            </MemoryRouter>
        </ToastProvider>
    );
}

describe('Login', () => {
    it('logs in and navigates to /dashboard', async () => {
        const user = userEvent.setup();
        login.mockResolvedValueOnce({ token: 'x.y.z' });

        renderAtLogin();

        await user.type(screen.getByLabelText(/username/i), 'admin');
        await user.type(screen.getByLabelText(/password/i), 'secret');
        await user.click(screen.getByRole('button', { name: /login/i }));

        expect(login).toHaveBeenCalledWith('admin', 'secret');
        expect(await screen.findByText('Dashboard')).toBeInTheDocument();
    });

    it('shows error message when login fails', async () => {
        const user = userEvent.setup();
        login.mockRejectedValueOnce({ response: { data: { message: 'Bad credentials' } } });

        renderAtLogin();

        await user.type(screen.getByLabelText(/username/i), 'admin');
        await user.type(screen.getByLabelText(/password/i), 'wrong');
        await user.click(screen.getByRole('button', { name: /login/i }));

        expect(await screen.findByText('Bad credentials')).toBeInTheDocument();
    });
});

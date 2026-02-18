import React, { useEffect, useMemo, useState } from 'react';

import { useToast } from './ToastProvider';
import { api } from '../service/axiosInstance';
import { getEmployeeIdFromToken } from '../service/AuthService';
import {
    checkIn,
    checkOut,
    formatMinutesToHHMM,
    getMonthly,
    getToday,
} from '../service/AttendanceService';

import '../styles/attendance.css';

function pad2(n) {
    return String(n).padStart(2, '0');
}

function toLocalTime(value) {
    if (!value) return null;
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return null;
    return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function toLocalDate(value) {
    if (!value) return null;
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return null;
    return d.toLocaleDateString();
}

function getErrorMessage(err) {
    return (
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        'Request failed'
    );
}

async function resolveEmployeeId() {
    const fromToken = getEmployeeIdFromToken();
    if (fromToken) return fromToken;

    const response = await api.get('/api/auth/me');
    const id = response?.data?.employeeId;
    const asNumber = Number(id);
    return Number.isFinite(asNumber) ? asNumber : null;
}

function AttendanceComponent() {
    const { show } = useToast();

    const [employeeId, setEmployeeId] = useState(null);
    const [today, setToday] = useState(null);
    const [monthlyRows, setMonthlyRows] = useState([]);

    const [loading, setLoading] = useState(true);
    const [actionLoading, setActionLoading] = useState(false);

    const now = useMemo(() => new Date(), []);
    const yyyyMM = useMemo(() => {
        const year = now.getFullYear();
        const month = pad2(now.getMonth() + 1);
        return `${year}-${month}`;
    }, [now]);

    const recentRows = useMemo(() => {
        const sorted = [...(monthlyRows || [])].sort((a, b) => {
            const da = new Date(a?.workDate || 0).getTime();
            const db = new Date(b?.workDate || 0).getTime();
            return db - da;
        });
        return sorted.slice(0, 7);
    }, [monthlyRows]);

    const monthlyTotalMinutes = useMemo(() => {
        return (monthlyRows || []).reduce((acc, r) => acc + Number(r?.workedMinutes || 0), 0);
    }, [monthlyRows]);

    const checkedInAt = toLocalTime(today?.checkIn);
    const checkedOutAt = toLocalTime(today?.checkOut);
    const workedHHMM = formatMinutesToHHMM(Number(today?.workedMinutes || 0));

    const isCheckedIn = Boolean(today?.checkIn);
    const isCheckedOut = Boolean(today?.checkOut);

    const canCheckIn = !actionLoading && !isCheckedIn;
    const canCheckOut = !actionLoading && isCheckedIn && !isCheckedOut;

    async function refreshAll(id) {
        const eid = id ?? employeeId;
        if (!eid) return;

        const [todayRow, monthRows] = await Promise.all([
            getToday(eid),
            getMonthly(eid, yyyyMM),
        ]);

        setToday(todayRow || null);
        setMonthlyRows(Array.isArray(monthRows) ? monthRows : []);
    }

    useEffect(() => {
        let cancelled = false;

        (async () => {
            try {
                setLoading(true);
                const eid = await resolveEmployeeId();
                if (cancelled) return;

                setEmployeeId(eid);
                if (!eid) {
                    show({ title: 'Error', message: 'Your account is not linked to an employee.', variant: 'danger' });
                    return;
                }

                await refreshAll(eid);
            } catch (err) {
                if (!cancelled) {
                    show({ title: 'Error', message: getErrorMessage(err), variant: 'danger' });
                }
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();

        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [show, yyyyMM]);

    async function handleCheckIn() {
        try {
            setActionLoading(true);
            await checkIn();
            show({ title: 'Success', message: 'Checked in successfully.', variant: 'success' });
            await refreshAll();
        } catch (err) {
            show({ title: 'Error', message: getErrorMessage(err), variant: 'danger' });
        } finally {
            setActionLoading(false);
        }
    }

    async function handleCheckOut() {
        try {
            setActionLoading(true);
            await checkOut();
            show({ title: 'Success', message: 'Checked out successfully.', variant: 'success' });
            await refreshAll();
        } catch (err) {
            show({ title: 'Error', message: getErrorMessage(err), variant: 'danger' });
        } finally {
            setActionLoading(false);
        }
    }

    return (
        <div className="container ems-page-container">
            <div className="ems-page-header d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
                <h3>Attendance</h3>
                {employeeId ? (
                    <div className="text-muted" style={{ fontSize: '0.9rem' }}>Employee ID: <strong>{employeeId}</strong></div>
                ) : null}
            </div>

            <div className="row g-3">
                <div className="col-12 col-lg-5">
                    <div className="card ems-checkin-card">
                        <div className="card-body">
                            <h5 className="card-title">Check-in / Check-out</h5>

                            <div className="attendance-status" aria-label="Today's attendance status">
                                {loading ? (
                                    <div className="d-flex align-items-center gap-2 text-muted">
                                        <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                        <span>Loading today’s status…</span>
                                    </div>
                                ) : today ? (
                                    <dl className="row mb-0">
                                        <dt className="col-5">Date</dt>
                                        <dd className="col-7">{toLocalDate(today.workDate) || '—'}</dd>

                                        <dt className="col-5">Checked in</dt>
                                        <dd className="col-7">{checkedInAt || '—'}</dd>

                                        <dt className="col-5">Checked out</dt>
                                        <dd className="col-7">{checkedOutAt || '—'}</dd>

                                        <dt className="col-5">Worked</dt>
                                        <dd className="col-7">{isCheckedOut ? workedHHMM : '—'}</dd>
                                    </dl>
                                ) : (
                                    <div className="text-muted">No record for today yet.</div>
                                )}
                            </div>

                            <div className="d-grid gap-2 mt-3">
                                <button
                                    type="button"
                                    className="btn btn-primary btn-lg"
                                    onClick={handleCheckIn}
                                    disabled={!canCheckIn || loading}
                                    aria-label="Check in"
                                >
                                    {actionLoading && !isCheckedIn ? (
                                        <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                                    ) : null}
                                    Check In
                                </button>

                                <button
                                    type="button"
                                    className="btn btn-outline-primary btn-lg"
                                    onClick={handleCheckOut}
                                    disabled={!canCheckOut || loading}
                                    aria-label="Check out"
                                >
                                    {actionLoading && isCheckedIn && !isCheckedOut ? (
                                        <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                                    ) : null}
                                    Check Out
                                </button>
                            </div>

                            <div className="small text-muted mt-3" aria-label="Help text">
                                Tip: You can only check out after checking in.
                            </div>
                        </div>
                    </div>
                </div>

                <div className="col-12 col-lg-7">
                    <div className="card mb-3">
                        <div className="card-body">
                            <h5 className="card-title">Recent Records (Last 7 Days)</h5>

                            {loading ? (
                                <div className="d-flex align-items-center gap-2 text-muted">
                                    <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                    <span>Loading records…</span>
                                </div>
                            ) : (
                                <div className="table-responsive">
                                    <table className="ems-table" aria-label="Recent attendance records">
                                        <caption className="visually-hidden">Recent attendance records</caption>
                                        <thead>
                                            <tr>
                                                <th scope="col">Date</th>
                                                <th scope="col">Check in</th>
                                                <th scope="col">Check out</th>
                                                <th scope="col" className="text-end">Worked</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {recentRows.length === 0 ? (
                                                <tr>
                                                    <td colSpan={4} className="text-muted text-center" style={{ padding: '1.5rem' }}>
                                                        No records yet.
                                                    </td>
                                                </tr>
                                            ) : (
                                                recentRows.map((r) => (
                                                    <tr key={r.id ?? `${r.workDate}-${r.checkIn}`}
                                                        tabIndex={0}
                                                        aria-label={`Attendance record for ${toLocalDate(r.workDate) || ''}`}
                                                    >
                                                        <td>{toLocalDate(r.workDate) || '—'}</td>
                                                        <td>{toLocalTime(r.checkIn) || '—'}</td>
                                                        <td>{toLocalTime(r.checkOut) || '—'}</td>
                                                        <td className="text-end" style={{ fontWeight: 600 }}>{formatMinutesToHHMM(Number(r.workedMinutes || 0))}</td>
                                                    </tr>
                                                ))
                                            )}
                                        </tbody>
                                    </table>
                                </div>
                            )}
                        </div>
                    </div>

                    <div className="card ems-monthly-summary">
                        <div className="card-body">
                            <h5 className="card-title">Monthly Summary</h5>
                            <div className="d-flex align-items-baseline justify-content-between flex-wrap gap-2">
                                <div className="text-muted">Total worked hours ({yyyyMM})</div>
                                <div className="ems-stat-value" aria-label="Total worked hours this month">
                                    {formatMinutesToHHMM(monthlyTotalMinutes)}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default AttendanceComponent;

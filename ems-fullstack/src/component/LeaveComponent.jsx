import React, { useEffect, useMemo, useState } from 'react';

import { useToast } from './ToastProvider';
import { getEmployeeIdFromToken, getRolesFromToken } from '../service/AuthService';
import { cancelLeave, decideLeave, getMyLeaves, getPending } from '../service/LeaveService';
import LeaveModal from './LeaveModal';

function getErrorMessage(err) {
    return (
        err?.response?.data?.message ||
        err?.response?.data?.error ||
        err?.message ||
        'Request failed'
    );
}

function toLocalDate(value) {
    if (!value) return '—';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleDateString();
}

function toLocalDateTime(value) {
    if (!value) return '—';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return String(value);
    return d.toLocaleString();
}

export default function LeaveComponent() {
    const toast = useToast();

    const roles = useMemo(() => getRolesFromToken(), []);
    const employeeId = useMemo(() => getEmployeeIdFromToken(), []);

    const canApprove = useMemo(() => roles.includes('HR') || roles.includes('MANAGER'), [roles]);

    const [activeTab, setActiveTab] = useState('my');
    const [myLeaves, setMyLeaves] = useState([]);
    const [pendingLeaves, setPendingLeaves] = useState([]);

    const [loading, setLoading] = useState(true);
    const [busyId, setBusyId] = useState(null);

    const [showModal, setShowModal] = useState(false);
    const [decisionNotes, setDecisionNotes] = useState({});

    async function refresh() {
        const [mine, pending] = await Promise.all([
            getMyLeaves(employeeId),
            canApprove ? getPending() : Promise.resolve([]),
        ]);

        setMyLeaves(Array.isArray(mine) ? mine : []);
        setPendingLeaves(Array.isArray(pending) ? pending : []);
    }

    useEffect(() => {
        let cancelled = false;
        (async () => {
            try {
                setLoading(true);
                await refresh();
            } catch (err) {
                if (!cancelled) toast.show({ title: 'Error', message: getErrorMessage(err), variant: 'danger' });
            } finally {
                if (!cancelled) setLoading(false);
            }
        })();
        return () => {
            cancelled = true;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [canApprove]);

    async function handleCancel(leaveId) {
        try {
            setBusyId(leaveId);
            await cancelLeave(leaveId);
            toast.show({ title: 'Success', message: 'Leave cancelled.', variant: 'success' });
            await refresh();
        } catch (err) {
            toast.show({ title: 'Error', message: getErrorMessage(err), variant: 'danger' });
        } finally {
            setBusyId(null);
        }
    }

    async function handleDecision(leaveId, status) {
        try {
            setBusyId(leaveId);
            const note = decisionNotes[leaveId] || '';
            await decideLeave(leaveId, { status, note });
            toast.show({ title: 'Success', message: `Leave ${status.toLowerCase()}.`, variant: 'success' });
            await refresh();
        } catch (err) {
            toast.show({ title: 'Error', message: getErrorMessage(err), variant: 'danger' });
        } finally {
            setBusyId(null);
        }
    }

    return (
        <div className="container ems-page-container">
            <div className="ems-page-header d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
                <h3>Leave</h3>
                {employeeId ? <div className="text-muted" style={{ fontSize: '0.9rem' }}>Employee ID: <strong>{employeeId}</strong></div> : null}
            </div>

            <ul className="nav nav-tabs" role="tablist" aria-label="Leave sections">
                <li className="nav-item" role="presentation">
                    <button
                        type="button"
                        className={`nav-link ${activeTab === 'my' ? 'active' : ''}`}
                        onClick={() => setActiveTab('my')}
                        role="tab"
                        aria-selected={activeTab === 'my'}
                    >
                        My Leaves
                    </button>
                </li>
                {canApprove ? (
                    <li className="nav-item" role="presentation">
                        <button
                            type="button"
                            className={`nav-link ${activeTab === 'pending' ? 'active' : ''}`}
                            onClick={() => setActiveTab('pending')}
                            role="tab"
                            aria-selected={activeTab === 'pending'}
                        >
                            Pending Approvals
                        </button>
                    </li>
                ) : null}
            </ul>

            <div className="tab-content pt-3">
                {activeTab === 'my' ? (
                    <div role="tabpanel" aria-label="My leaves">
                        <div className="card">
                            <div className="card-body">
                                <h5 className="card-title">My Leave Requests</h5>

                                {loading ? (
                                    <div className="d-flex align-items-center gap-2 text-muted">
                                        <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                        <span>Loading…</span>
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="ems-table" aria-label="My leave requests">
                                            <thead>
                                                <tr>
                                                    <th scope="col">Type</th>
                                                    <th scope="col">Start</th>
                                                    <th scope="col">End</th>
                                                    <th scope="col" className="text-end">Days</th>
                                                    <th scope="col">Status</th>
                                                    <th scope="col">Applied At</th>
                                                    <th scope="col">Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {myLeaves.length === 0 ? (
                                                    <tr>
                                                        <td colSpan={7} className="text-muted">No leave requests yet.</td>
                                                    </tr>
                                                ) : (
                                                    myLeaves.map((r) => (
                                                        <tr key={r.id}>
                                                            <td>{r.type}</td>
                                                            <td>{toLocalDate(r.startDate)}</td>
                                                            <td>{toLocalDate(r.endDate)}</td>
                                                            <td className="text-end">{r.days ?? '—'}</td>
                                                            <td>{r.status}</td>
                                                            <td>{toLocalDateTime(r.appliedAt)}</td>
                                                            <td>
                                                                {r.status === 'PENDING' ? (
                                                                    <button
                                                                        type="button"
                                                                        className="btn btn-sm btn-outline-danger"
                                                                        onClick={() => handleCancel(r.id)}
                                                                        disabled={busyId === r.id}
                                                                        aria-label={`Cancel leave ${r.id}`}
                                                                    >
                                                                        {busyId === r.id ? (
                                                                            <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                                                                        ) : null}
                                                                        Cancel
                                                                    </button>
                                                                ) : (
                                                                    <span className="text-muted">—</span>
                                                                )}
                                                            </td>
                                                        </tr>
                                                    ))
                                                )}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>

                        <button
                            type="button"
                            className="btn btn-primary ems-fab"
                            onClick={() => setShowModal(true)}
                            aria-label="Apply leave"
                        >
                            + Apply Leave
                        </button>
                    </div>
                ) : null}

                {activeTab === 'pending' && canApprove ? (
                    <div role="tabpanel" aria-label="Pending approvals">
                        <div className="card">
                            <div className="card-body">
                                <h5 className="card-title">Pending Approvals</h5>

                                {loading ? (
                                    <div className="d-flex align-items-center gap-2 text-muted">
                                        <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                                        <span>Loading…</span>
                                    </div>
                                ) : (
                                    <div className="table-responsive">
                                        <table className="ems-table" aria-label="Pending leave requests">
                                            <thead>
                                                <tr>
                                                    <th scope="col">Employee</th>
                                                    <th scope="col">Type</th>
                                                    <th scope="col">Start</th>
                                                    <th scope="col">End</th>
                                                    <th scope="col" className="text-end">Days</th>
                                                    <th scope="col">Applied At</th>
                                                    <th scope="col">Comment</th>
                                                    <th scope="col">Actions</th>
                                                </tr>
                                            </thead>
                                            <tbody>
                                                {pendingLeaves.length === 0 ? (
                                                    <tr>
                                                        <td colSpan={8} className="text-muted">No pending requests.</td>
                                                    </tr>
                                                ) : (
                                                    pendingLeaves.map((r) => (
                                                        <tr key={r.id}>
                                                            <td>{r.employeeId ?? '—'}</td>
                                                            <td>{r.type}</td>
                                                            <td>{toLocalDate(r.startDate)}</td>
                                                            <td>{toLocalDate(r.endDate)}</td>
                                                            <td className="text-end">{r.days ?? '—'}</td>
                                                            <td>{toLocalDateTime(r.appliedAt)}</td>
                                                            <td style={{ minWidth: 220 }}>
                                                                <label className="visually-hidden" htmlFor={`note-${r.id}`}>Decision comment</label>
                                                                <input
                                                                    id={`note-${r.id}`}
                                                                    type="text"
                                                                    className="form-control form-control-sm"
                                                                    placeholder="Optional comment"
                                                                    value={decisionNotes[r.id] || ''}
                                                                    onChange={(e) => setDecisionNotes((prev) => ({ ...prev, [r.id]: e.target.value }))}
                                                                    disabled={busyId === r.id}
                                                                />
                                                            </td>
                                                            <td className="d-flex gap-2">
                                                                <button
                                                                    type="button"
                                                                    className="btn btn-sm btn-success"
                                                                    onClick={() => handleDecision(r.id, 'APPROVED')}
                                                                    disabled={busyId === r.id}
                                                                    aria-label={`Approve leave ${r.id}`}
                                                                >
                                                                    Approve
                                                                </button>
                                                                <button
                                                                    type="button"
                                                                    className="btn btn-sm btn-danger"
                                                                    onClick={() => handleDecision(r.id, 'REJECTED')}
                                                                    disabled={busyId === r.id}
                                                                    aria-label={`Reject leave ${r.id}`}
                                                                >
                                                                    Reject
                                                                </button>
                                                            </td>
                                                        </tr>
                                                    ))
                                                )}
                                            </tbody>
                                        </table>
                                    </div>
                                )}
                            </div>
                        </div>
                    </div>
                ) : null}
            </div>

            <LeaveModal
                show={showModal}
                onClose={() => setShowModal(false)}
                onSuccess={async () => {
                    toast.show({ title: 'Success', message: 'Leave applied successfully.', variant: 'success' });
                    await refresh();
                }}
                employeeId={employeeId}
            />
        </div>
    );
}

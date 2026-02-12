import React, { useEffect, useMemo, useRef, useState } from 'react';

import { applyLeave } from '../service/LeaveService';

const LEAVE_TYPES = [
  { value: 'ANNUAL', label: 'Annual' },
  { value: 'SICK', label: 'Sick' },
  { value: 'CASUAL', label: 'Casual' },
  { value: 'UNPAID', label: 'Unpaid' },
];

function daysInclusive(startDate, endDate) {
  if (!startDate || !endDate) return null;
  const a = new Date(startDate);
  const b = new Date(endDate);
  if (Number.isNaN(a.getTime()) || Number.isNaN(b.getTime())) return null;
  const ms = b.setHours(0, 0, 0, 0) - a.setHours(0, 0, 0, 0);
  const days = Math.floor(ms / (1000 * 60 * 60 * 24)) + 1;
  return Number.isFinite(days) ? days : null;
}

function getErrorMessage(err) {
  return (
    err?.response?.data?.message ||
    err?.response?.data?.error ||
    err?.message ||
    'Request failed'
  );
}

export default function LeaveModal({ show, onClose, onSuccess, employeeId }) {
  const firstFieldRef = useRef(null);

  const [type, setType] = useState('UNPAID');
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [note, setNote] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const computedDays = useMemo(() => daysInclusive(startDate, endDate), [startDate, endDate]);

  const canSubmit = useMemo(() => {
    if (!type || !startDate || !endDate) return false;
    const days = computedDays;
    return days != null && days > 0;
  }, [type, startDate, endDate, computedDays]);

  useEffect(() => {
    if (!show) return;
    setError('');
    setSubmitting(false);

    const t = setTimeout(() => {
      firstFieldRef.current?.focus();
    }, 0);
    return () => clearTimeout(t);
  }, [show]);

  useEffect(() => {
    if (!show) return;
    function onKeyDown(e) {
      if (e.key === 'Escape') {
        e.preventDefault();
        onClose?.();
      }
    }
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [show, onClose]);

  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!canSubmit) {
      setError('Please fill required fields and ensure start date is not after end date.');
      return;
    }

    try {
      setSubmitting(true);
      await applyLeave({ employeeId, startDate, endDate, type, note });
      onSuccess?.();
      onClose?.();

      // reset
      setType('UNPAID');
      setStartDate('');
      setEndDate('');
      setNote('');
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (!show) return null;

  return (
    <div
      className="modal fade show"
      style={{ display: 'block' }}
      role="dialog"
      aria-modal="true"
      aria-labelledby="leaveModalTitle"
      tabIndex={-1}
    >
      <div className="modal-dialog" role="document">
        <div className="modal-content">
          <form onSubmit={handleSubmit}>
            <div className="modal-header">
              <h5 className="modal-title" id="leaveModalTitle">Apply Leave</h5>
              <button
                type="button"
                className="btn-close"
                aria-label="Close"
                onClick={onClose}
                disabled={submitting}
              />
            </div>

            <div className="modal-body">
              {error ? (
                <div className="alert alert-danger" role="alert">{error}</div>
              ) : null}

              <div className="mb-3">
                <label className="form-label" htmlFor="leaveType">Leave Type</label>
                <select
                  id="leaveType"
                  ref={firstFieldRef}
                  className="form-select"
                  value={type}
                  onChange={(e) => setType(e.target.value)}
                  required
                >
                  {LEAVE_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>{t.label}</option>
                  ))}
                </select>
              </div>

              <div className="row g-2">
                <div className="col-12 col-sm-6">
                  <div className="mb-3">
                    <label className="form-label" htmlFor="leaveStart">Start Date</label>
                    <input
                      id="leaveStart"
                      type="date"
                      className="form-control"
                      value={startDate}
                      onChange={(e) => setStartDate(e.target.value)}
                      required
                    />
                  </div>
                </div>
                <div className="col-12 col-sm-6">
                  <div className="mb-3">
                    <label className="form-label" htmlFor="leaveEnd">End Date</label>
                    <input
                      id="leaveEnd"
                      type="date"
                      className="form-control"
                      value={endDate}
                      onChange={(e) => setEndDate(e.target.value)}
                      required
                    />
                  </div>
                </div>
              </div>

              <div className="mb-3">
                <div className="form-text" aria-live="polite">
                  Calculated days: <strong>{computedDays ?? '—'}</strong>
                </div>
              </div>

              <div className="mb-3">
                <label className="form-label" htmlFor="leaveNote">Note (optional)</label>
                <textarea
                  id="leaveNote"
                  className="form-control"
                  rows={3}
                  value={note}
                  onChange={(e) => setNote(e.target.value)}
                />
              </div>
            </div>

            <div className="modal-footer">
              <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={submitting}>
                Cancel
              </button>
              <button type="submit" className="btn btn-primary" disabled={!canSubmit || submitting}>
                {submitting ? (
                  <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />
                ) : null}
                Submit
              </button>
            </div>
          </form>
        </div>
      </div>

      <div className="modal-backdrop fade show" />
    </div>
  );
}

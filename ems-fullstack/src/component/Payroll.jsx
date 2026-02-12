import React, { useMemo, useState } from 'react';

import { editEmployee } from '../service/EmployeeService';
import { downloadPayslipPdf, generatePayroll } from '../service/PayrollService';
import { useToasts } from './ToastProvider';

function parseContentDispositionFilename(contentDisposition) {
  if (!contentDisposition) return null;
  const match = /filename\*?=(?:UTF-8''|\")?([^;\"\n]+)\"?/i.exec(contentDisposition);
  if (!match) return null;
  try {
    return decodeURIComponent(match[1]);
  } catch {
    return match[1];
  }
}

function Payroll() {
  const { pushToast } = useToasts();
  const [employeeId, setEmployeeId] = useState('');
  const [period, setPeriod] = useState('');
  const [allowances, setAllowances] = useState('');
  const [deductions, setDeductions] = useState('');

  const [employee, setEmployee] = useState(null);
  const [payroll, setPayroll] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [isOpening, setIsOpening] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);

  const parsed = useMemo(() => {
    const id = Number(employeeId);
    const allowancesNum = allowances === '' ? null : Number(allowances);
    const deductionsNum = deductions === '' ? 0 : Number(deductions);
    return {
      id: Number.isFinite(id) ? id : NaN,
      allowances: allowancesNum,
      deductions: Number.isFinite(deductionsNum) ? deductionsNum : NaN,
    };
  }, [employeeId, allowances, deductions]);

  const validate = () => {
    if (!employeeId || !Number.isFinite(parsed.id) || parsed.id <= 0) return 'Employee ID must be a positive number';
    if (!period) return 'Payroll month is required';
    if (parsed.allowances === null || !Number.isFinite(parsed.allowances) || parsed.allowances < 0) return 'Allowances must be a non-negative number';
    if (!Number.isFinite(parsed.deductions) || parsed.deductions < 0) return 'Deductions must be a non-negative number';
    return null;
  };

  const openEmployee = async () => {
    setError('');
    setSuccess('');
    setPayroll(null);

    if (!employeeId || !Number.isFinite(parsed.id) || parsed.id <= 0) {
      setError('Enter a valid Employee ID to open');
      return;
    }

    setIsOpening(true);
    try {
      const res = await editEmployee(parsed.id);
      setEmployee(res.data);
      setSuccess('Employee loaded');
      pushToast({ type: 'success', message: 'Employee loaded' });
    } catch (err) {
      setEmployee(null);
      const message = err?.response?.data?.message || 'Failed to load employee';
      setError(message);
      pushToast({ type: 'error', message });
    } finally {
      setIsOpening(false);
    }
  };

  const onGenerate = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    setPayroll(null);

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setIsGenerating(true);
    try {
      const created = await generatePayroll(parsed.id, period, parsed.allowances, parsed.deductions);
      setPayroll(created);
      setSuccess('Payroll generated successfully');
      pushToast({ type: 'success', message: 'Payroll generated successfully' });
    } catch (err) {
      const message = err?.response?.data?.message || 'Failed to generate payroll';
      setError(message);
      pushToast({ type: 'error', message });
    } finally {
      setIsGenerating(false);
    }
  };

  const onDownload = async () => {
    if (!payroll?.id) return;
    setError('');
    setSuccess('');

    setIsDownloading(true);
    try {
      const { blob, headers } = await downloadPayslipPdf(payroll.id);
      const cd = headers?.['content-disposition'] || headers?.['Content-Disposition'];
      const filename = parseContentDispositionFilename(cd) || `payslip-${payroll.employeeId}-${payroll.year}-${String(payroll.month).padStart(2, '0')}.pdf`;

      const pdfBlob = new Blob([blob], { type: 'application/pdf' });
      const url = window.URL.createObjectURL(pdfBlob);
      const a = document.createElement('a');
      a.href = url;
      a.download = filename;
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      pushToast({ type: 'success', message: 'Payslip downloaded' });
    } catch (err) {
      const message = err?.response?.data?.message || 'Failed to download payslip';
      setError(message);
      pushToast({ type: 'error', message });
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="container" style={{ marginTop: 24, maxWidth: 820 }}>
      <h3>Payroll</h3>

      {error ? <div className="alert alert-danger mt-3">{error}</div> : null}
      {success ? <div className="alert alert-success mt-3">{success}</div> : null}

      <div className="card mt-3">
        <div className="card-body">
          <div className="row g-3 align-items-end">
            <div className="col-md-4">
              <label className="form-label" htmlFor="payroll-employee-id">Employee ID</label>
              <input
                id="payroll-employee-id"
                className="form-control"
                value={employeeId}
                onChange={(e) => setEmployeeId(e.target.value)}
                inputMode="numeric"
                placeholder="e.g. 1"
                aria-label="Employee ID"
                required
              />
            </div>

            <div className="col-md-4">
              <button className="btn btn-outline-primary" type="button" onClick={openEmployee} disabled={isOpening}>
                {isOpening ? (
                  <span className="d-inline-flex align-items-center gap-2">
                    <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                    Opening…
                  </span>
                ) : 'Open Employee'}
              </button>
            </div>
          </div>

          {employee ? (
            <div className="mt-3">
              <div><strong>Name:</strong> {employee.firstName} {employee.lastName}</div>
              <div><strong>Email:</strong> {employee.email}</div>
            </div>
          ) : null}
        </div>
      </div>

      <div className="card mt-3">
        <div className="card-body">
          <form onSubmit={onGenerate}>
            <div className="row g-3">
              <div className="col-md-4">
                <label className="form-label" htmlFor="payroll-month">Payroll Month</label>
                <input
                  id="payroll-month"
                  type="month"
                  className="form-control"
                  value={period}
                  onChange={(e) => setPeriod(e.target.value)}
                  aria-label="Payroll month"
                  required
                />
                <div className="form-text">Format: YYYY-MM</div>
              </div>

              <div className="col-md-4">
                <label className="form-label" htmlFor="payroll-allowances">Allowances</label>
                <input
                  id="payroll-allowances"
                  type="number"
                  className="form-control"
                  value={allowances}
                  onChange={(e) => setAllowances(e.target.value)}
                  min="0"
                  step="0.01"
                  aria-label="Allowances"
                  required
                />
              </div>

              <div className="col-md-4">
                <label className="form-label" htmlFor="payroll-deductions">Deductions</label>
                <input
                  id="payroll-deductions"
                  type="number"
                  className="form-control"
                  value={deductions}
                  onChange={(e) => setDeductions(e.target.value)}
                  min="0"
                  step="0.01"
                  aria-label="Deductions"
                />
              </div>
            </div>

            <div className="mt-3 d-flex gap-2">
              <button className="btn btn-primary" disabled={isGenerating}>
                {isGenerating ? (
                  <span className="d-inline-flex align-items-center gap-2">
                    <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                    Generating…
                  </span>
                ) : 'Generate Payroll'}
              </button>

              {payroll?.id ? (
                <button type="button" className="btn btn-success" onClick={onDownload} disabled={isDownloading}>
                  {isDownloading ? (
                    <span className="d-inline-flex align-items-center gap-2">
                      <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                      Downloading…
                    </span>
                  ) : 'Download Payslip'}
                </button>
              ) : null}
            </div>
          </form>

          {payroll ? (
            <div className="mt-4">
              <h6>Generated Payroll</h6>
              <div className="row">
                <div className="col-md-6 text-muted">Payroll ID: {payroll.id}</div>
                <div className="col-md-6 text-muted">Period: {payroll.year}-{String(payroll.month).padStart(2, '0')}</div>
              </div>
              <div className="row mt-2">
                <div className="col-md-4">Gross: {String(payroll.grossPay)}</div>
                <div className="col-md-4">EPF (Employee): {String(payroll.epfEmployee)}</div>
                <div className="col-md-4"><strong>Net: {String(payroll.netPay)}</strong></div>
              </div>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}

export default Payroll;

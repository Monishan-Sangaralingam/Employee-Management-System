import React, { useEffect, useMemo, useState } from 'react';
import {
  Bar,
  BarChart,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import { getEmployeeIdFromToken, getUsernameFromToken } from '../service/AuthService';
import { getDashboard } from '../service/DashboardService';

const LEAVE_COLORS = {
  PENDING: '#F59E0B',
  APPROVED: '#10B981',
  REJECTED: '#EF4444',
};

function Dashboard() {
  const username = getUsernameFromToken();
  const employeeId = getEmployeeIdFromToken();

  const [data, setData] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      setError('');
      try {
        const result = await getDashboard();
        if (!cancelled) setData(result);
      } catch (err) {
        const message = err?.response?.data?.message || 'Failed to load dashboard';
        if (!cancelled) setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  const departmentData = useMemo(() => {
    const counts = data?.departmentCounts || {};
    return Object.entries(counts).map(([department, count]) => ({
      department,
      count,
    }));
  }, [data]);

  const leaveStatusData = useMemo(() => {
    const counts = data?.leaveStatusCounts || {};
    return Object.entries(counts).map(([status, count]) => ({
      status,
      count,
    }));
  }, [data]);

  return (
    <div className="container ems-page-container">
      <div className="ems-page-header d-flex align-items-center justify-content-between flex-wrap gap-2">
        <h3>Dashboard</h3>
        <div className="text-muted" style={{ fontSize: '0.9rem' }}>
          Signed in as <strong>{username || 'unknown'}</strong>
          {employeeId ? <span className="ms-1" style={{ opacity: 0.7 }}>(ID: {employeeId})</span> : ''}
        </div>
      </div>

      {error ? <div className="alert alert-danger mt-3">{error}</div> : null}
      {loading ? (
        <div className="mt-4 d-flex align-items-center gap-2 text-muted">
          <span className="spinner-border spinner-border-sm" aria-hidden="true" />
          Loading dashboard…
        </div>
      ) : null}

      {data && !loading ? (
        <>
          <div className="row g-3">
            <div className="col-sm-6 col-lg-4">
              <div className="card ems-stat-card">
                <div className="card-body">
                  <div className="ems-stat-label">Total Employees</div>
                  <div className="ems-stat-value">{data.totalEmployees}</div>
                </div>
              </div>
            </div>
            <div className="col-sm-6 col-lg-4">
              <div className="card ems-stat-card ems-stat-card--accent">
                <div className="card-body">
                  <div className="ems-stat-label">Active This Month</div>
                  <div className="ems-stat-value">{data.activeThisMonth}</div>
                </div>
              </div>
            </div>
            <div className="col-sm-6 col-lg-4">
              <div className="card ems-stat-card ems-stat-card--info">
                <div className="card-body">
                  <div className="ems-stat-label">Avg Attendance Hours</div>
                  <div className="ems-stat-value">{Number(data.avgAttendanceHours).toFixed(1)}</div>
                </div>
              </div>
            </div>
          </div>

          <div className="row g-3 mt-1">
            <div className="col-sm-6 col-lg-4">
              <div className="card ems-stat-card ems-stat-card--warning">
                <div className="card-body">
                  <div className="ems-stat-label">Leaves Pending</div>
                  <div className="ems-stat-value">{data.leavesPending}</div>
                </div>
              </div>
            </div>
            <div className="col-sm-6 col-lg-8">
              <div className="card ems-stat-card ems-stat-card--success">
                <div className="card-body">
                  <div className="ems-stat-label">Salary Expense This Month</div>
                  <div className="ems-stat-value" style={{ fontSize: '1.5rem' }}>{String(data.salaryExpenseThisMonth)}</div>
                </div>
              </div>
            </div>
          </div>

          <div className="row mt-4 g-3">
            <div className="col-md-7">
              <div className="card">
                <div className="card-body">
                  <h5 className="card-title">Department Counts</h5>
                  <div style={{ width: '100%', height: 280 }}>
                    <ResponsiveContainer>
                      <BarChart data={departmentData} margin={{ top: 10, right: 20, left: 0, bottom: 10 }}>
                        <XAxis dataKey="department" tick={{ fontSize: 12, fill: '#64748B' }} />
                        <YAxis allowDecimals={false} tick={{ fontSize: 12, fill: '#64748B' }} />
                        <Tooltip
                          contentStyle={{ borderRadius: 8, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,.12)' }}
                        />
                        <Legend />
                        <Bar dataKey="count" name="Employees" fill="#4F46E5" radius={[6, 6, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-md-5">
              <div className="card">
                <div className="card-body">
                  <h5 className="card-title">Leave Status</h5>
                  <div style={{ width: '100%', height: 280 }}>
                    <ResponsiveContainer>
                      <PieChart>
                        <Tooltip
                          contentStyle={{ borderRadius: 8, border: 'none', boxShadow: '0 4px 12px rgba(0,0,0,.12)' }}
                        />
                        <Legend />
                        <Pie
                          data={leaveStatusData}
                          dataKey="count"
                          nameKey="status"
                          innerRadius={60}
                          outerRadius={90}
                          paddingAngle={3}
                          cornerRadius={4}
                        >
                          {leaveStatusData.map((entry) => (
                            <Cell key={entry.status} fill={LEAVE_COLORS[entry.status] || '#64748B'} />
                          ))}
                        </Pie>
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}

export default Dashboard;

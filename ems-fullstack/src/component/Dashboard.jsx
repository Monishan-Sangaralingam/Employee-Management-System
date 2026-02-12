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
  PENDING: '#f0ad4e',
  APPROVED: '#5cb85c',
  REJECTED: '#d9534f',
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
    <div className="container" style={{ marginTop: 24 }}>
      <h3>Dashboard</h3>
      <div className="text-muted">
        Signed in as {username || 'unknown'}
        {employeeId ? ` (employeeId: ${employeeId})` : ''}
      </div>

      {error ? <div className="alert alert-danger mt-3">{error}</div> : null}
      {loading ? <div className="mt-3 text-muted">Loading…</div> : null}

      {data && !loading ? (
        <>
          <div className="row mt-3 g-3">
            <div className="col-md-4">
              <div className="card"><div className="card-body">
                <div className="text-muted">Total Employees</div>
                <div className="h4 mb-0">{data.totalEmployees}</div>
              </div></div>
            </div>
            <div className="col-md-4">
              <div className="card"><div className="card-body">
                <div className="text-muted">Active This Month</div>
                <div className="h4 mb-0">{data.activeThisMonth}</div>
              </div></div>
            </div>
            <div className="col-md-4">
              <div className="card"><div className="card-body">
                <div className="text-muted">Avg Attendance Hours</div>
                <div className="h4 mb-0">{Number(data.avgAttendanceHours).toFixed(2)}</div>
              </div></div>
            </div>
          </div>

          <div className="row mt-3 g-3">
            <div className="col-md-4">
              <div className="card"><div className="card-body">
                <div className="text-muted">Leaves Pending</div>
                <div className="h4 mb-0">{data.leavesPending}</div>
              </div></div>
            </div>
            <div className="col-md-8">
              <div className="card"><div className="card-body">
                <div className="text-muted">Salary Expense This Month</div>
                <div className="h4 mb-0">{String(data.salaryExpenseThisMonth)}</div>
              </div></div>
            </div>
          </div>

          <div className="row mt-4 g-3">
            <div className="col-md-7">
              <div className="card">
                <div className="card-body">
                  <h6 className="mb-3">Department Counts</h6>
                  <div style={{ width: '100%', height: 280 }}>
                    <ResponsiveContainer>
                      <BarChart data={departmentData} margin={{ top: 10, right: 20, left: 0, bottom: 10 }}>
                        <XAxis dataKey="department" />
                        <YAxis allowDecimals={false} />
                        <Tooltip />
                        <Legend />
                        <Bar dataKey="count" name="Employees" fill="#0d6efd" />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                </div>
              </div>
            </div>

            <div className="col-md-5">
              <div className="card">
                <div className="card-body">
                  <h6 className="mb-3">Leave Status</h6>
                  <div style={{ width: '100%', height: 280 }}>
                    <ResponsiveContainer>
                      <PieChart>
                        <Tooltip />
                        <Legend />
                        <Pie
                          data={leaveStatusData}
                          dataKey="count"
                          nameKey="status"
                          innerRadius={60}
                          outerRadius={90}
                        >
                          {leaveStatusData.map((entry) => (
                            <Cell key={entry.status} fill={LEAVE_COLORS[entry.status] || '#6c757d'} />
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

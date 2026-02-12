import React from 'react';
import { getEmployeeIdFromToken, getUsernameFromToken } from '../service/AuthService';

function Dashboard() {
  const username = getUsernameFromToken();
  const employeeId = getEmployeeIdFromToken();

  return (
    <div className="container" style={{ marginTop: 24 }}>
      <h3>Dashboard</h3>
      <div className="text-muted">Signed in as {username || 'unknown'}{employeeId ? ` (employeeId: ${employeeId})` : ''}</div>
    </div>
  );
}

export default Dashboard;

import React from 'react';
import { Navigate, Outlet } from 'react-router-dom';
import { getToken, hasAnyRole, isTokenValid } from '../service/AuthService';

function PrivateRoute({ allowedRoles }) {
  const token = getToken();

  if (!token || !isTokenValid(token)) {
    return <Navigate to="/login" replace />;
  }

  if (allowedRoles && allowedRoles.length > 0 && !hasAnyRole(allowedRoles, token)) {
    return <Navigate to="/dashboard" replace />;
  }

  return <Outlet />;
}

export default PrivateRoute;

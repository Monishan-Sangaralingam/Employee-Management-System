import React, { useEffect, useMemo, useState } from 'react'
import '../style/header.css'
import { Link, useNavigate } from 'react-router-dom'
import { getRolesFromToken, getToken, logout } from '../service/AuthService'


function Header() {
    const navigate = useNavigate();
    const [token, setToken] = useState(getToken());

    useEffect(() => {
        const handler = () => setToken(getToken());
        window.addEventListener('authChanged', handler);
        return () => window.removeEventListener('authChanged', handler);
    }, []);

    const roles = useMemo(() => getRolesFromToken(token), [token]);
    const isAuthed = Boolean(token);
    const has = (r) => roles.includes(r);
    const showEmployees = has('ADMIN') || has('HR') || has('MANAGER');
    const showPayroll = has('ADMIN') || has('HR');

    const onLogout = () => {
        logout();
        navigate('/login', { replace: true });
    };

    return (
        <>
            <nav className='navbar bg-body-primary col'>
                <div className="container d-flex justify-content-between">
                    <Link className="navbar-brand navi" to={isAuthed ? '/dashboard' : '/login'}>
                        Employee Management System
                    </Link>

                    {isAuthed ? (
                        <div className="d-flex gap-3 align-items-center">
                            <Link className="navi" to="/dashboard">Dashboard</Link>
                            {showEmployees ? <Link className="navi" to="/employees">Employees</Link> : null}
                            <Link className="navi" to="/attendance">Attendance</Link>
                            <Link className="navi" to="/leave">Leave</Link>
                            {showPayroll ? <Link className="navi" to="/payroll">Payroll</Link> : null}
                            <button className="btn btn-sm btn-outline-secondary" onClick={onLogout}>Logout</button>
                        </div>
                    ) : null}
                </div>
            </nav>
        </>
    )
}

export default Header
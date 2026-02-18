import React, { useEffect, useMemo, useState } from 'react'
import '../style/header.css'
import { Link, NavLink, useNavigate } from 'react-router-dom'
import { getRolesFromToken, getToken, logout } from '../service/AuthService'
import { getPending } from '../service/LeaveService'


function Header() {
    const navigate = useNavigate();
    const [token, setToken] = useState(getToken());
    const [navOpen, setNavOpen] = useState(false);
    const [pendingLeaveCount, setPendingLeaveCount] = useState(null);

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
    const showPendingCount = has('MANAGER');

    useEffect(() => {
        let cancelled = false;
        (async () => {
            if (!isAuthed || !showPendingCount) {
                setPendingLeaveCount(null);
                return;
            }

            try {
                const rows = await getPending();
                if (!cancelled) {
                    setPendingLeaveCount(Array.isArray(rows) ? rows.length : 0);
                }
            } catch {
                if (!cancelled) {
                    setPendingLeaveCount(null);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
    }, [isAuthed, showPendingCount, token]);

    const onLogout = () => {
        logout();
        navigate('/login', { replace: true });
    };

    const onNavItemClick = () => {
        setNavOpen(false);
    };

    return (
        <>
            <nav className='ems-navbar navbar navbar-expand-lg'>
                <div className="container">
                    <Link className="navbar-brand" to={isAuthed ? '/dashboard' : '/login'}>
                        Employee Management System
                    </Link>

                    {isAuthed ? (
                        <>
                            <button
                                className="navbar-toggler"
                                type="button"
                                aria-label="Toggle navigation"
                                aria-controls="emsNavbar"
                                aria-expanded={navOpen}
                                onClick={() => setNavOpen((v) => !v)}
                            >
                                <span className="navbar-toggler-icon" />
                            </button>

                            <div className={`collapse navbar-collapse ${navOpen ? 'show' : ''}`} id="emsNavbar">
                                <div className="navbar-nav ms-auto d-flex gap-1 align-items-lg-center">
                                    <NavLink className="nav-link" to="/dashboard" onClick={onNavItemClick}>
                                        Dashboard
                                    </NavLink>
                                    {showEmployees ? (
                                        <NavLink className="nav-link" to="/employees" onClick={onNavItemClick}>
                                            Employees
                                        </NavLink>
                                    ) : null}

                                    <NavLink className="nav-link" to="/attendance" onClick={onNavItemClick}>
                                        Attendance
                                    </NavLink>

                                    <NavLink className="nav-link" to="/leave" onClick={onNavItemClick}>
                                        <span className="d-inline-flex align-items-center gap-1">
                                            <span>Leave</span>
                                            {showPendingCount && pendingLeaveCount != null && pendingLeaveCount > 0 ? (
                                                <span className="badge" aria-label={`${pendingLeaveCount} pending leave requests`}>
                                                    {pendingLeaveCount}
                                                </span>
                                            ) : null}
                                        </span>
                                    </NavLink>

                                    {showPayroll ? (
                                        <NavLink className="nav-link" to="/payroll" onClick={onNavItemClick}>
                                            Payroll
                                        </NavLink>
                                    ) : null}

                                    <button className="btn btn-ems-logout btn-sm ms-lg-2" onClick={() => { onNavItemClick(); onLogout(); }}>
                                        Logout
                                    </button>
                                </div>
                            </div>
                        </>
                    ) : null}
                </div>
            </nav>
        </>
    )
}

export default Header
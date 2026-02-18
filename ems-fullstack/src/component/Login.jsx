import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { login } from '../service/AuthService';
import { useToasts } from './ToastProvider';

function Login() {
  const navigate = useNavigate();
  const { pushToast } = useToasts();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  const onSubmit = async (e) => {
    e.preventDefault();
    setError('');

    const trimmedUsername = username.trim();
    if (!trimmedUsername || !password) {
      setError('Username and password are required');
      pushToast({ type: 'error', message: 'Username and password are required' });
      return;
    }

    setIsSubmitting(true);

    try {
      await login(trimmedUsername, password);
      pushToast({ type: 'success', message: 'Signed in successfully' });
      navigate('/dashboard', { replace: true });
    } catch (err) {
      const message = err?.response?.data?.message || 'Invalid username or password';
      setError(message);
      pushToast({ type: 'error', message: 'Login failed. Please try again.' });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div style={{ minHeight: 'calc(100vh - 60px)', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '2rem 1rem' }}>
      <div className="card" style={{ width: '100%', maxWidth: 420, overflow: 'hidden' }}>
        <div style={{ background: 'linear-gradient(135deg, #312E81, #4F46E5, #6366F1)', padding: '1.75rem 1.5rem', textAlign: 'center' }}>
          <div style={{ width: 56, height: 56, borderRadius: '50%', background: 'rgba(255,255,255,.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto 0.75rem', fontSize: '1.5rem', color: '#fff' }}>
            &#128100;
          </div>
          <h3 style={{ color: '#fff', margin: 0, fontSize: '1.25rem' }}>Welcome Back</h3>
          <p style={{ color: 'rgba(255,255,255,.7)', margin: '0.25rem 0 0', fontSize: '0.875rem' }}>Sign in to your account</p>
        </div>

        <div className="card-body" style={{ padding: '1.75rem' }}>
          {error ? <div className="alert alert-danger" role="alert" aria-live="assertive">{error}</div> : null}

          <form onSubmit={onSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="username">Username</label>
              <input
                id="username"
                className="form-control"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                aria-label="Username"
                placeholder="Enter your username"
                required
              />
            </div>

            <div className="mb-3">
              <label className="form-label" htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                className="form-control"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                aria-label="Password"
                placeholder="Enter your password"
                required
              />
            </div>

            <button className="btn btn-primary w-100" disabled={isSubmitting} style={{ padding: '0.6rem', fontWeight: 600 }}>
              {isSubmitting ? (
                <span className="d-inline-flex align-items-center gap-2">
                  <span className="spinner-border spinner-border-sm" aria-hidden="true" />
                  Signing in…
                </span>
              ) : 'Sign In'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

export default Login;

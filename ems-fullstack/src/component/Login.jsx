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
    <div className="container" style={{ maxWidth: 420, marginTop: 48 }}>
      <h3 className="mb-3">Login</h3>
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
            required
          />
        </div>

        <button className="btn btn-primary w-100" disabled={isSubmitting}>
          {isSubmitting ? (
            <span className="d-inline-flex align-items-center gap-2">
              <span className="spinner-border spinner-border-sm" aria-hidden="true" />
              Signing in…
            </span>
          ) : 'Login'}
        </button>
      </form>
    </div>
  );
}

export default Login;

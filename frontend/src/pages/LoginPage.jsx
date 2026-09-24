import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Alert from '../components/Alert';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: '', password: '' });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);

  function validate() {
    const next = {};
    if (!form.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = 'Enter a valid email';
    if (!form.password) next.password = 'Password is required';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError('');
    if (!validate()) return;

    setLoading(true);
    const result = await login(form);
    setLoading(false);

    if (!result.ok) {
      setServerError(result.message);
      return;
    }

    if (result.user.role === 'ADMIN') {
      navigate('/admin', { replace: true });
    } else {
      navigate(location.state?.from || '/', { replace: true });
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-head">
          <span className="brand-mark large">AP</span>
          <h1>ProjectHub — ACCET</h1>
          <p>Sign in to submit, like and save projects</p>
        </div>

        <form className="auth-body" onSubmit={handleSubmit} noValidate>
          <Alert message={serverError} onClose={() => setServerError('')} />

          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              placeholder="you@student.accet.ac.in"
              autoComplete="email"
            />
            {errors.email && <small className="field-error">{errors.email}</small>}
          </label>

          <label className="field">
            <span>Password</span>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              placeholder="••••••••"
              autoComplete="current-password"
            />
            {errors.password && <small className="field-error">{errors.password}</small>}
          </label>

          <button type="submit" className="btn btn-navy full" disabled={loading}>
            {loading ? 'Signing in…' : 'Sign In'}
          </button>

          <p className="auth-switch">
            New here? <Link to="/register">Create an account</Link>
          </p>

          <div className="demo-box">
            <strong>Demo accounts</strong>
            <span>Student — arjun@student.accet.ac.in / student123</span>
            <span>Admin — admin@accet.ac.in / admin@2024</span>
          </div>
        </form>
      </div>
    </div>
  );
}

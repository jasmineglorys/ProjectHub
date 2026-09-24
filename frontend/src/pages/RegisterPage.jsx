import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Alert from '../components/Alert';
import { DEPARTMENTS } from '../constants';

export default function RegisterPage() {
  const { register } = useAuth();
  const navigate = useNavigate();

  const [form, setForm] = useState({
    name: '',
    email: '',
    rollNo: '',
    department: 'CSE',
    year: 3,
    password: '',
    confirmPassword: '',
  });
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);

  function update(field, value) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  const passwordRules = [
    { label: 'At least 8 characters', valid: form.password.length >= 8 },
    { label: 'One uppercase letter', valid: /[A-Z]/.test(form.password) },
    { label: 'One lowercase letter', valid: /[a-z]/.test(form.password) },
    { label: 'One number', valid: /\d/.test(form.password) },
    { label: 'One special character', valid: /[^A-Za-z0-9\s]/.test(form.password) },
    { label: 'No spaces', valid: !/\s/.test(form.password) },
  ];
  const passwordScore = passwordRules.filter((rule) => rule.valid).length;
  const passwordStrength = passwordScore >= 6 ? 'Strong' : passwordScore >= 4 ? 'Medium' : 'Weak';
  const confirmPasswordError =
    form.confirmPassword && form.confirmPassword !== form.password
      ? 'Passwords do not match'
      : errors.confirmPassword;

  function validate() {
    const next = {};
    if (!form.name.trim()) next.name = 'Full name is required';
    else if (form.name.trim().length < 2) next.name = 'Name is too short';

    if (!form.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = 'Enter a valid email';

    if (!form.rollNo.trim()) next.rollNo = 'Roll number is required';
    else if (!/^9176\d{7}$/.test(form.rollNo.trim()))
      next.rollNo = 'Roll number must be exactly 11 digits and start with 9176';

    if (!form.password) next.password = 'Password is required';
    else if (passwordScore < passwordRules.length)
      next.password = 'Password does not meet all requirements';

    if (form.confirmPassword !== form.password) {
      next.confirmPassword = 'Passwords do not match';
    }

    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(event) {
    event.preventDefault();
    setServerError('');
    if (!validate()) return;

    setLoading(true);
    const result = await register({
      name: form.name.trim(),
      email: form.email.trim(),
      rollNo: form.rollNo.trim(),
      department: form.department,
      year: Number(form.year),
      password: form.password,
    });
    setLoading(false);

    if (!result.ok) {
      setServerError(result.message);
      return;
    }
    navigate('/', { replace: true });
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-head">
          <span className="brand-mark large">AP</span>
          <h1>Create your account</h1>
          <p>Join ProjectHub and share your work</p>
        </div>

        <form className="auth-body" onSubmit={handleSubmit} noValidate>
          <Alert message={serverError} onClose={() => setServerError('')} />

          <label className="field">
            <span>Full Name</span>
            <input
              type="text"
              value={form.name}
              onChange={(e) => update('name', e.target.value)}
              placeholder="Arjun Selvam"
            />
            {errors.name && <small className="field-error">{errors.name}</small>}
          </label>

          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={form.email}
              onChange={(e) => update('email', e.target.value)}
              placeholder="you@student.accet.ac.in"
            />
            {errors.email && <small className="field-error">{errors.email}</small>}
          </label>

          <label className="field">
            <span>Roll Number</span>
            <input
              type="text"
              inputMode="numeric"
              maxLength={11}
              value={form.rollNo}
              onChange={(e) => update('rollNo', e.target.value.replace(/\D/g, '').slice(0, 11))}
              placeholder="91760000000"
            />
            {errors.rollNo && <small className="field-error">{errors.rollNo}</small>}
          </label>

          <div className="field-row">
            <label className="field">
              <span>Department</span>
              <select
                value={form.department}
                onChange={(e) => update('department', e.target.value)}
              >
                {DEPARTMENTS.map((d) => (
                  <option key={d} value={d}>
                    {d}
                  </option>
                ))}
              </select>
            </label>

            <label className="field">
              <span>Year</span>
              <select value={form.year} onChange={(e) => update('year', e.target.value)}>
                {[1, 2, 3, 4].map((y) => (
                  <option key={y} value={y}>
                    Year {y}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <label className="field">
            <span>Password</span>
            <div className="password-input">
              <input
                type={showPassword ? 'text' : 'password'}
                value={form.password}
                onChange={(e) => update('password', e.target.value)}
                placeholder="Project@123"
                autoComplete="new-password"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword((visible) => !visible)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? '🙈' : '👁'}
              </button>
            </div>
            <div className="password-strength" aria-live="polite">
              <div className={`strength-bar strength-${passwordStrength.toLowerCase()}`}>
                <span style={{ width: `${(passwordScore / passwordRules.length) * 100}%` }} />
              </div>
              <small className="hint">Strength: {passwordStrength}</small>
            </div>
            <ul className="password-rules">
              {passwordRules.map((rule) => (
                <li key={rule.label} className={rule.valid ? 'valid' : ''}>
                  <span aria-hidden="true">{rule.valid ? '✓' : '○'}</span> {rule.label}
                </li>
              ))}
            </ul>
            {errors.password && <small className="field-error">{errors.password}</small>}
          </label>

          <label className="field">
            <span>Confirm Password</span>
            <div className="password-input">
              <input
                type={showConfirmPassword ? 'text' : 'password'}
                value={form.confirmPassword}
                onChange={(e) => update('confirmPassword', e.target.value)}
                autoComplete="new-password"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowConfirmPassword((visible) => !visible)}
                aria-label={showConfirmPassword ? 'Hide confirm password' : 'Show confirm password'}
              >
                {showConfirmPassword ? '🙈' : '👁'}
              </button>
            </div>
            {confirmPasswordError && <small className="field-error">{confirmPasswordError}</small>}
          </label>

          <button type="submit" className="btn btn-navy full" disabled={loading}>
            {loading ? 'Creating account…' : 'Create Account'}
          </button>

          <p className="auth-switch">
            Already registered? <Link to="/login">Sign in</Link>
          </p>
        </form>
      </div>
    </div>
  );
}

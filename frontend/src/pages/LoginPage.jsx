import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import Alert from '../components/Alert';
import {
  requestPasswordReset,
  resendPasswordResetOtp,
  resetPassword,
  verifyPasswordResetOtp,
} from '../api/authApi';
import { extractError } from '../api/axiosConfig';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [form, setForm] = useState({ email: '', password: '' });
  const [step, setStep] = useState('login');
  const [otp, setOtp] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [errors, setErrors] = useState({});
  const [serverError, setServerError] = useState('');
  const [notice, setNotice] = useState(location.state?.resetSuccess || '');
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

  function validateEmail() {
    const next = {};
    if (!form.email.trim()) next.email = 'Email is required';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) next.email = 'Enter a valid email';
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleForgotPassword(event) {
    event.preventDefault();
    setServerError('');
    setNotice('');
    if (!validateEmail()) return;
    setLoading(true);
    try {
      const response = await requestPasswordReset({ email: form.email.trim() });
      setNotice(response.data.message);
      setStep('otp');
    } catch (error) {
      setServerError(extractError(error));
    } finally {
      setLoading(false);
    }
  }

  async function handleVerifyOtp(event) {
    event.preventDefault();
    setServerError('');
    setNotice('');
    if (!/^[0-9]{6}$/.test(otp)) {
      setErrors({ otp: 'Enter the 6-digit verification code' });
      return;
    }
    setErrors({});
    setLoading(true);
    try {
      await verifyPasswordResetOtp({ email: form.email.trim(), otp });
      setOtp('');
      setStep('reset');
    } catch (error) {
      setServerError(extractError(error));
    } finally {
      setLoading(false);
    }
  }

  async function handleResendOtp() {
    setServerError('');
    setNotice('');
    setLoading(true);
    try {
      const response = await resendPasswordResetOtp({ email: form.email.trim() });
      setNotice(response.data.message);
    } catch (error) {
      setServerError(extractError(error));
    } finally {
      setLoading(false);
    }
  }

  async function handleResetPassword(event) {
    event.preventDefault();
    setServerError('');
    setNotice('');
    const next = {};
    if (newPassword.length < 8 || newPassword.length > 72
        || !/[A-Z]/.test(newPassword) || !/[a-z]/.test(newPassword)
        || !/\d/.test(newPassword) || !/[^A-Za-z0-9\s]/.test(newPassword)
        || /\s/.test(newPassword)) {
      next.newPassword = 'Use 8-72 characters with uppercase, lowercase, number, and special character, without spaces';
    }
    if (confirmPassword !== newPassword) next.confirmPassword = 'Passwords do not match';
    setErrors(next);
    if (Object.keys(next).length > 0) return;

    setLoading(true);
    try {
      await resetPassword({ newPassword });
      setForm((current) => ({ ...current, password: '' }));
      setNewPassword('');
      setConfirmPassword('');
      setStep('login');
      setNotice('Your password has been reset. Sign in with your new password.');
      navigate('/login', {
        replace: true,
        state: { resetSuccess: 'Your password has been reset. Sign in with your new password.' },
      });
    } catch (error) {
      setServerError(extractError(error));
    } finally {
      setLoading(false);
    }
  }

  function returnToLogin() {
    setStep('login');
    setErrors({});
    setServerError('');
    setNotice('');
  }

  const heading = {
    login: ['ProjectHub — ACCET', 'Sign in to submit, like and save projects'],
    email: ['Reset your password', 'Enter the email linked to your account'],
    otp: ['Check your email', 'Enter the 6-digit code to continue'],
    reset: ['Choose a new password', 'Create a new password for your account'],
  }[step];

  return (
    <div className="auth-page">
      <div className="auth-card">
        <div className="auth-head">
          <h1>{heading[0]}</h1>
          <p>{heading[1]}</p>
        </div>

        <form
          className="auth-body"
          onSubmit={step === 'login' ? handleSubmit : step === 'email' ? handleForgotPassword
            : step === 'otp' ? handleVerifyOtp : handleResetPassword}
          noValidate
        >
          <Alert message={serverError} onClose={() => setServerError('')} />
          {notice && <p className="auth-notice" role="status">{notice}</p>}

          {(step === 'login' || step === 'email') && (
            <label className="field">
              <span>Email</span>
              <input
                type="email"
                value={form.email}
                onChange={(e) => setForm({ ...form, email: e.target.value })}
                placeholder="abc@gmail.com"
                autoComplete="email"
              />
              {errors.email && <small className="field-error">{errors.email}</small>}
            </label>
          )}

          {step === 'login' && (
            <>
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
              <button type="button" className="auth-link" onClick={() => {
                setStep('email');
                setServerError('');
                setNotice('');
                setErrors({});
              }}>
                Forgot Password?
              </button>
            </>
          )}

          {step === 'otp' && (
            <label className="field">
              <span>6-digit verification code</span>
              <input
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={otp}
                onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))}
                aria-label="6-digit verification code"
              />
              {errors.otp && <small className="field-error">{errors.otp}</small>}
            </label>
          )}

          {step === 'reset' && (
            <>
              <label className="field">
                <span>New Password</span>
                <input
                  type="password"
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                />
                {errors.newPassword && <small className="field-error">{errors.newPassword}</small>}
              </label>
              <label className="field">
                <span>Confirm Password</span>
                <input
                  type="password"
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                />
                {errors.confirmPassword && <small className="field-error">{errors.confirmPassword}</small>}
              </label>
            </>
          )}

          {step !== 'otp' && (
            <button type="submit" className="btn btn-navy full" disabled={loading}>
              {loading ? 'Please wait…' : step === 'login' ? 'Sign In'
                : step === 'email' ? 'Send Verification Code' : 'Reset Password'}
            </button>
          )}
          {step === 'otp' && (
            <>
              <button type="submit" className="btn btn-navy full" disabled={loading}>
                {loading ? 'Verifying…' : 'Verify Code'}
              </button>
              <button type="button" className="auth-link auth-link-centered" onClick={handleResendOtp} disabled={loading}>
                Resend code
              </button>
            </>
          )}

          {step === 'login' ? (
            <p className="auth-switch">
              New here? <Link to="/register">Create an account</Link>
            </p>
          ) : (
            <p className="auth-switch">
              <button type="button" className="auth-link" onClick={returnToLogin}>Back to sign in</button>
            </p>
          )}
        </form>
      </div>
    </div>
  );
}

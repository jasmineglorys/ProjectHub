import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import Alert from '../components/Alert';
import { resetPassword } from '../api/authApi';
import { extractError } from '../api/axiosConfig';

export default function ResetPasswordPage() {
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') || '';
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [error, setError] = useState('');
  const [passwordError, setPasswordError] = useState('');
  const [confirmError, setConfirmError] = useState('');
  const [loading, setLoading] = useState(false);
  const [complete, setComplete] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setError('');
    setPasswordError('');
    setConfirmError('');

    if (newPassword.length < 8 || newPassword.length > 72
        || !/[A-Z]/.test(newPassword) || !/[a-z]/.test(newPassword)
        || !/\d/.test(newPassword) || !/[^A-Za-z0-9\s]/.test(newPassword)
        || /\s/.test(newPassword)) {
      setPasswordError('Use 8-72 characters with uppercase, lowercase, number, and special character, without spaces.');
      return;
    }
    if (newPassword !== confirmPassword) {
      setConfirmError('Passwords do not match.');
      return;
    }

    setLoading(true);
    try {
      await resetPassword({ token, newPassword });
      setComplete(true);
    } catch (requestError) {
      setError(extractError(requestError));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-card" aria-labelledby="reset-heading">
        <div className="auth-head">
          <h1 id="reset-heading">{complete ? 'Password updated' : 'Choose a new password'}</h1>
          <p>{complete ? 'Your ProjectHub password has been reset.' : 'Create a new password for your account'}</p>
        </div>
        {complete ? (
          <div className="auth-body">
            <p className="auth-notice" role="status">You can now sign in with your new password.</p>
            <Link to="/login" className="btn btn-navy full">Back to sign in</Link>
          </div>
        ) : !token ? (
          <div className="auth-body">
            <Alert message="This password reset link is invalid or incomplete. Request a new link to continue." />
            <p className="auth-switch"><Link to="/login">Return to sign in</Link></p>
          </div>
        ) : (
          <form className="auth-body" onSubmit={handleSubmit} noValidate>
            <Alert message={error} onClose={() => setError('')} />
            <label className="field">
              <span>New password</span>
              <div className="password-input">
                <input
                  type={showPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  required
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
              {passwordError && <small className="field-error">{passwordError}</small>}
            </label>
            <label className="field">
              <span>Confirm new password</span>
              <div className="password-input">
                <input
                  type={showConfirmPassword ? 'text' : 'password'}
                  autoComplete="new-password"
                  value={confirmPassword}
                  onChange={(event) => setConfirmPassword(event.target.value)}
                  required
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
              {confirmError && <small className="field-error">{confirmError}</small>}
            </label>
            <button type="submit" className="btn btn-navy full" disabled={loading}>
              {loading ? 'Updating password…' : 'Reset Password'}
            </button>
            <p className="auth-switch"><Link to="/login">Back to sign in</Link></p>
          </form>
        )}
      </section>
    </div>
  );
}
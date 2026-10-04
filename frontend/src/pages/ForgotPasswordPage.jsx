import { useState } from 'react';
import { Link } from 'react-router-dom';
import Alert from '../components/Alert';
import { requestPasswordReset } from '../api/authApi';
import { extractError } from '../api/axiosConfig';

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState('');
  const [fieldError, setFieldError] = useState('');
  const [serverError, setServerError] = useState('');
  const [notice, setNotice] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(event) {
    event.preventDefault();
    setFieldError('');
    setServerError('');
    setNotice('');

    const normalizedEmail = email.trim();
    if (!normalizedEmail) {
      setFieldError('Email is required.');
      return;
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(normalizedEmail)) {
      setFieldError('Enter a valid email address.');
      return;
    }

    setLoading(true);
    try {
      const response = await requestPasswordReset({ email: normalizedEmail });
      setNotice(response.data.message);
    } catch (error) {
      setServerError(extractError(error));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-card" aria-labelledby="forgot-heading">
        <div className="auth-head">
          <h1 id="forgot-heading">Reset your password</h1>
          <p>Enter the email linked to your ProjectHub account</p>
        </div>
        <form className="auth-body" onSubmit={handleSubmit} noValidate>
          <Alert message={serverError} onClose={() => setServerError('')} />
          {notice && <p className="auth-notice" role="status">{notice}</p>}
          <label className="field">
            <span>Email</span>
            <input
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="abc@gmail.com"
              autoComplete="email"
              aria-invalid={Boolean(fieldError)}
              required
            />
            {fieldError && <small className="field-error">{fieldError}</small>}
          </label>
          <button type="submit" className="btn btn-navy full" disabled={loading}>
            {loading ? 'Sending…' : 'Send Reset Link'}
          </button>
          <p className="auth-switch"><Link to="/login">Back to sign in</Link></p>
        </form>
      </section>
    </div>
  );
}
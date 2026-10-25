import { useEffect, useState, FormEvent } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { authApi } from '../services/api';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const [token] = useState(() => searchParams.get('token') || '');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // Remove the token from the address bar (history / screenshots) once it has been read.
  useEffect(() => {
    if (token) {
      window.history.replaceState(null, '', window.location.pathname);
    }
  }, [token]);

  useEffect(() => {
    if (!success) return;
    const timer = setTimeout(() => navigate('/login'), 3000);
    return () => clearTimeout(timer);
  }, [success, navigate]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    if (password.length < 8 || password.length > 64 || !/\p{L}/u.test(password) || !/\d/.test(password)) {
      setError('رمز عبور باید ۸ تا ۶۴ کاراکتر و شامل حداقل یک حرف و یک عدد باشد');
      return;
    }
    if (password !== confirmPassword) {
      setError('رمز عبور و تکرار آن یکسان نیستند');
      return;
    }
    setLoading(true);
    try {
      await authApi.resetPassword(token, password);
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'خطا در تغییر رمز عبور');
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h2>تعیین رمز عبور جدید</h2>
          <div className="error-msg">لینک بازیابی نامعتبر است. لطفاً دوباره درخواست بازیابی رمز عبور بدهید.</div>
          <div className="auth-footer">
            <Link to="/forgot-password">درخواست لینک جدید</Link>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h2>تعیین رمز عبور جدید</h2>
        {error && <div className="error-msg">{error}</div>}
        {success ? (
          <>
            <div className="success-msg">رمز عبور با موفقیت تغییر کرد. در حال انتقال به صفحه ورود...</div>
            <div className="auth-footer">
              <Link to="/login">ورود به حساب کاربری</Link>
            </div>
          </>
        ) : (
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>رمز عبور جدید</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                maxLength={64}
                placeholder="حداقل ۸ کاراکتر، شامل حرف و عدد"
                autoComplete="new-password"
              />
            </div>
            <div className="form-group">
              <label>تکرار رمز عبور جدید</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                maxLength={64}
                placeholder="رمز عبور را دوباره وارد کنید"
                autoComplete="new-password"
              />
              {confirmPassword.length > 0 && password !== confirmPassword && (
                <small style={{ color: '#d32f2f' }}>رمز عبور و تکرار آن یکسان نیستند</small>
              )}
              {confirmPassword.length > 0 && password === confirmPassword && (
                <small style={{ color: '#2e7d32' }}>رمزها یکسان هستند ✓</small>
              )}
            </div>
            <button type="submit" className="btn btn-primary" disabled={loading}>
              {loading ? 'در حال ذخیره...' : 'ذخیره رمز عبور جدید'}
            </button>
          </form>
        )}
      </div>
    </div>
  );
}

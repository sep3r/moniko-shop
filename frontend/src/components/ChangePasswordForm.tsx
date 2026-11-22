import { useState, FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';

/** فرم تغییر رمز در پنل کاربری. بعد از موفقیت، سایر دستگاه‌ها logout می‌شوند. */
export default function ChangePasswordForm() {
  const { changePassword } = useAuth();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    if (next.length < 8 || next.length > 64 || !/\p{L}/u.test(next) || !/\d/.test(next)) {
      setError('رمز عبور جدید باید ۸ تا ۶۴ کاراکتر و شامل حداقل یک حرف و یک عدد باشد');
      return;
    }
    if (next !== confirm) {
      setError('رمز عبور جدید و تکرار آن یکسان نیستند');
      return;
    }
    if (next === current) {
      setError('رمز عبور جدید باید با رمز فعلی متفاوت باشد');
      return;
    }
    setLoading(true);
    try {
      await changePassword(current, next);
      setCurrent('');
      setNext('');
      setConfirm('');
      setSuccess(true);
    } catch (err: any) {
      setError(err.response?.data?.message || 'خطا در تغییر رمز عبور');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-card" style={{ maxWidth: 480, margin: '24px 0 0' }}>
      <h3 style={{ marginTop: 0 }}>تغییر رمز عبور</h3>
      {error && <div className="error-msg">{error}</div>}
      {success && (
        <div className="success-msg">
          رمز عبور با موفقیت تغییر کرد. در سایر دستگاه‌ها باید دوباره وارد شوید.
        </div>
      )}
      <form onSubmit={handleSubmit}>
        <div className="form-group">
          <label>رمز عبور فعلی</label>
          <input
            type="password"
            value={current}
            onChange={(e) => setCurrent(e.target.value)}
            required
            maxLength={128}
            autoComplete="current-password"
          />
        </div>
        <div className="form-group">
          <label>رمز عبور جدید</label>
          <input
            type="password"
            value={next}
            onChange={(e) => setNext(e.target.value)}
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
            value={confirm}
            onChange={(e) => setConfirm(e.target.value)}
            required
            maxLength={64}
            autoComplete="new-password"
          />
          {confirm.length > 0 && next !== confirm && (
            <small style={{ color: '#d32f2f' }}>رمز عبور و تکرار آن یکسان نیستند</small>
          )}
          {confirm.length > 0 && next === confirm && (
            <small style={{ color: '#2e7d32' }}>رمزها یکسان هستند ✓</small>
          )}
        </div>
        <button type="submit" className="btn btn-primary" disabled={loading}>
          {loading ? 'در حال ذخیره...' : 'تغییر رمز عبور'}
        </button>
      </form>
    </div>
  );
}

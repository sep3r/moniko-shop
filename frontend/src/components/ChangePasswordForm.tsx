import { useState, FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { authApi } from '../services/api';
import OtpField from './OtpField';
import { useCountdown } from '../utils/useCountdown';

/** فرم تغییر رمز در پنل کاربری. بعد از موفقیت، سایر دستگاه‌ها logout می‌شوند. */
export default function ChangePasswordForm() {
  const { user, changePassword } = useAuth();
  // حساب با شماره‌ی تأییدشده: برای تغییر رمز کد پیامکی هم لازم است
  const needsOtp = !!user?.phoneVerified && !!user?.phone;
  const [code, setCode] = useState('');
  const [codeSent, setCodeSent] = useState(false);
  const [info, setInfo] = useState('');
  const { left, start } = useCountdown();
  const [current, setCurrent] = useState('');
  const [next, setNext] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const validate = (): boolean => {
    if (next.length < 8 || next.length > 64 || !/\p{L}/u.test(next) || !/\d/.test(next)) {
      setError('رمز عبور جدید باید ۸ تا ۶۴ کاراکتر و شامل حداقل یک حرف و یک عدد باشد');
      return false;
    }
    if (next !== confirm) {
      setError('رمز عبور جدید و تکرار آن یکسان نیستند');
      return false;
    }
    if (next === current) {
      setError('رمز عبور جدید باید با رمز فعلی متفاوت باشد');
      return false;
    }
    return true;
  };

  const sendCode = async () => {
    setError('');
    setSuccess(false);
    if (!current) return setError('ابتدا رمز عبور فعلی را وارد کنید');
    if (!validate()) return;
    setLoading(true);
    try {
      const res = await authApi.changePasswordOtp();
      setCode('');
      setCodeSent(true);
      setInfo(res.data.message);
      start(res.data.resendAfterSeconds);
    } catch (err: any) {
      setError(err.response?.data?.message || 'ارسال کد ناموفق بود');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    if (needsOtp && !codeSent) {
      await sendCode();
      return;
    }
    if (needsOtp && code.length !== 6) {
      setError('کد ۶ رقمی را کامل وارد کنید');
      return;
    }
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
      await changePassword(current, next, needsOtp ? code : undefined);
      setCode('');
      setCodeSent(false);
      setInfo('');
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
            <small className="field-msg bad">رمز عبور و تکرار آن یکسان نیستند</small>
          )}
          {confirm.length > 0 && next === confirm && (
            <small className="field-msg good">رمزها یکسان هستند ✓</small>
          )}
        </div>
        {needsOtp && codeSent && (
          <>
            {info && <div className="success-msg">{info}</div>}
            <OtpField value={code} onChange={setCode} resendIn={left} onResend={sendCode} disabled={loading} />
          </>
        )}
        <button type="submit" className="btn btn-primary" disabled={loading || (needsOtp && codeSent && code.length !== 6)}>
          {loading
            ? 'لطفاً صبر کنید...'
            : needsOtp && !codeSent
              ? 'ارسال کد تأیید به موبایل'
              : 'تغییر رمز عبور'}
        </button>
      </form>
    </div>
  );
}

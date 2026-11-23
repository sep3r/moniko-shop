import { toEnDigits } from '../utils/phone';

interface Props {
  value: string;
  onChange: (v: string) => void;
  /** ثانیه‌های باقی‌مانده تا امکان ارسال مجدد (0 = آماده) */
  resendIn: number;
  onResend: () => void;
  disabled?: boolean;
}

/** ورودی کد ۶ رقمی پیامکی + دکمه‌ی ارسال مجدد. */
export default function OtpField({ value, onChange, resendIn, onResend, disabled }: Props) {
  return (
    <div className="form-group">
      <label>کد تأیید پیامک‌شده</label>
      <input
        type="text"
        inputMode="numeric"
        autoComplete="one-time-code"
        dir="ltr"
        maxLength={6}
        value={value}
        onChange={(e) => onChange(toEnDigits(e.target.value).replace(/[^0-9]/g, '').slice(0, 6))}
        placeholder="------"
        required
        style={{ textAlign: 'center', letterSpacing: 8, fontSize: 20 }}
      />
      <div style={{ marginTop: 6, fontSize: 13 }}>
        {resendIn > 0 ? (
          <span>ارسال مجدد کد تا {resendIn} ثانیه دیگر</span>
        ) : (
          <button type="button" className="link-btn" onClick={onResend} disabled={disabled}>
            ارسال مجدد کد
          </button>
        )}
      </div>
    </div>
  );
}

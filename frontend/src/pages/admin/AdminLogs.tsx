import { useCallback, useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { LogPage, SecurityEventEntry, VisitLogEntry } from '../../types';
import Pager from '../../components/Pager';

const nf = new Intl.NumberFormat('fa-IR');
const fmtTime = (iso: string) =>
  new Date(iso).toLocaleString('fa-IR', { timeZone: 'Asia/Tehran', dateStyle: 'short', timeStyle: 'medium' });
const fmtDay = (iso: string) =>
  new Date(iso + 'T12:00:00').toLocaleDateString('fa-IR', { year: 'numeric', month: 'short', day: 'numeric' });

const EVENT_LABELS: Record<string, string> = {
  LOGIN_SUCCESS: 'ورود موفق',
  LOGIN_FAILURE: 'ورود ناموفق',
  LOGIN_BLOCKED: 'ورود مسدودشده',
  REGISTER: 'ثبت‌نام',
  PASSWORD_CHANGED: 'تغییر رمز',
  PASSWORD_CHANGE_FAILED: 'تغییر رمز ناموفق',
  PASSWORD_RESET_REQUESTED: 'درخواست بازیابی رمز',
  PASSWORD_RESET_DONE: 'بازیابی رمز انجام شد',
  ORDER_CREATED: 'ثبت سفارش',
  ORDER_BLOCKED: 'سفارش ردشده (سقف)',
  ADMIN_PRODUCT_CREATE: 'مدیر: افزودن محصول',
  ADMIN_PRODUCT_UPDATE: 'مدیر: ویرایش محصول',
  ADMIN_PRODUCT_DELETE: 'مدیر: حذف محصول',
  ADMIN_CATEGORY_CREATE: 'مدیر: افزودن دسته',
  ADMIN_CATEGORY_UPDATE: 'مدیر: ویرایش دسته',
  ADMIN_CATEGORY_DELETE: 'مدیر: حذف دسته',
  ADMIN_ORDER_STATUS: 'مدیر: تغییر وضعیت سفارش',
  ADMIN_USER_STATUS: 'مدیر: فعال/غیرفعال کاربر',
  ADMIN_USER_ROLES: 'مدیر: تغییر نقش کاربر',
};
const WARN_EVENTS = new Set(['LOGIN_FAILURE', 'LOGIN_BLOCKED', 'PASSWORD_CHANGE_FAILED', 'ORDER_BLOCKED']);

type Tab = 'visits' | 'security';

export default function AdminLogs() {
  const [tab, setTab] = useState<Tab>('visits');

  return (
    <div>
      <h2 className="section-title">لاگ و IP ها</h2>
      <p className="visit-note">
        آدرس IP و مرورگر بازدیدکنندگان (مهمان و کاربر) و رویدادهای امنیتی ثبت می‌شود. IP جزو داده‌ی شخصی است؛
        لاگ بازدید بعد از ۹۰ روز و رویدادها بعد از ۳۶۵ روز خودکار پاک می‌شوند.
      </p>
      <div className="log-tabs">
        <button type="button" className={tab === 'visits' ? 'active' : ''} onClick={() => setTab('visits')}>
          بازدیدها (IP)
        </button>
        <button type="button" className={tab === 'security' ? 'active' : ''} onClick={() => setTab('security')}>
          رویدادهای امنیتی
        </button>
      </div>
      {tab === 'visits' ? <VisitsTab /> : <SecurityTab />}
    </div>
  );
}

function VisitsTab() {
  const [data, setData] = useState<LogPage<VisitLogEntry> | null>(null);
  const [page, setPage] = useState(0);
  const [kind, setKind] = useState('all');
  const [ip, setIp] = useState('');
  const [date, setDate] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    adminApi
      .getVisitLogs({ page, size: 50, kind, ip: ip.trim() || undefined, date: date || undefined })
      .then((res) => { setData(res.data); setError(''); })
      .catch(() => setError('خطا در دریافت لاگ بازدید'));
  }, [page, kind, ip, date]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="log-filters">
        <select value={kind} onChange={(e) => { setPage(0); setKind(e.target.value); }} aria-label="نوع بازدیدکننده">
          <option value="all">همه</option>
          <option value="guest">فقط مهمان</option>
          <option value="user">فقط کاربر</option>
        </select>
        <input
          type="text" inputMode="decimal" placeholder="IP (ابتدای آدرس)" value={ip} dir="ltr"
          onChange={(e) => { setPage(0); setIp(e.target.value); }} aria-label="فیلتر IP"
        />
        <input type="date" value={date} onChange={(e) => { setPage(0); setDate(e.target.value); }} aria-label="تاریخ" />
        <button type="button" className="btn" style={{ width: 'auto' }} onClick={load}>تازه‌سازی</button>
        {data && <span className="visit-note">{nf.format(data.totalItems)} مورد</span>}
      </div>
      {error && <div className="error-msg">{error}</div>}
      {!data ? <div className="loading">در حال بارگذاری...</div> : (
        <>
          <div className="log-table-wrap">
            <table className="admin-table">
              <thead>
                <tr><th>روز</th><th>نوع</th><th>کاربر</th><th>IP</th><th>مرورگر</th><th>اولین</th><th>آخرین</th></tr>
              </thead>
              <tbody>
                {data.items.length === 0 && <tr><td colSpan={7}>موردی یافت نشد</td></tr>}
                {data.items.map((v) => (
                  <tr key={v.id}>
                    <td>{fmtDay(v.date)}</td>
                    <td>
                      {v.userId ? <span className="log-chip">کاربر</span> : <span className="log-chip guest">مهمان</span>}
                    </td>
                    <td>{v.userEmail ?? '—'}</td>
                    <td className="log-ip">{v.ip}</td>
                    <td className="log-ua" title={v.userAgent ?? ''}>{v.userAgent ?? '—'}</td>
                    <td>{fmtTime(v.firstSeen)}</td>
                    <td>{fmtTime(v.lastSeen)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}

function SecurityTab() {
  const [data, setData] = useState<LogPage<SecurityEventEntry> | null>(null);
  const [page, setPage] = useState(0);
  const [type, setType] = useState('');
  const [q, setQ] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    adminApi
      .getSecurityLogs({ page, size: 50, type: type || undefined, q: q.trim() || undefined })
      .then((res) => { setData(res.data); setError(''); })
      .catch(() => setError('خطا در دریافت رویدادها'));
  }, [page, type, q]);

  useEffect(() => { load(); }, [load]);

  return (
    <div>
      <div className="log-filters">
        <select value={type} onChange={(e) => { setPage(0); setType(e.target.value); }} aria-label="نوع رویداد">
          <option value="">همه‌ی رویدادها</option>
          {Object.entries(EVENT_LABELS).map(([k, label]) => <option key={k} value={k}>{label}</option>)}
        </select>
        <input
          type="text" placeholder="ایمیل یا IP" value={q} dir="ltr"
          onChange={(e) => { setPage(0); setQ(e.target.value); }} aria-label="جستجو"
        />
        <button type="button" className="btn" style={{ width: 'auto' }} onClick={load}>تازه‌سازی</button>
        {data && <span className="visit-note">{nf.format(data.totalItems)} مورد</span>}
      </div>
      {error && <div className="error-msg">{error}</div>}
      {!data ? <div className="loading">در حال بارگذاری...</div> : (
        <>
          <div className="log-table-wrap">
            <table className="admin-table">
              <thead>
                <tr><th>زمان</th><th>رویداد</th><th>ایمیل</th><th>IP</th><th>جزئیات</th><th>مرورگر</th></tr>
              </thead>
              <tbody>
                {data.items.length === 0 && <tr><td colSpan={6}>موردی یافت نشد</td></tr>}
                {data.items.map((e) => (
                  <tr key={e.id}>
                    <td>{fmtTime(e.createdAt)}</td>
                    <td>
                      <span className={`log-chip${WARN_EVENTS.has(e.type) ? ' warn' : ''}`}>
                        {EVENT_LABELS[e.type] ?? e.type}
                      </span>
                    </td>
                    <td>{e.email ?? '—'}</td>
                    <td className="log-ip">{e.ip ?? '—'}</td>
                    <td>{e.details ?? '—'}</td>
                    <td className="log-ua" title={e.userAgent ?? ''}>{e.userAgent ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Pager page={data.page} totalPages={data.totalPages} onChange={setPage} />
        </>
      )}
    </div>
  );
}

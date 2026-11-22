import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { HomeProductItem, Product } from '../../types';

type Kind = 'daily-deal' | 'featured';

/** ISO (UTC) ← مقدار datetime-local مرورگر (ساعت محلی) و برعکس */
const toLocalInput = (iso?: string | null) => {
  if (!iso) return '';
  const d = new Date(iso);
  const off = d.getTimezoneOffset() * 60000;
  return new Date(d.getTime() - off).toISOString().slice(0, 16);
};
const toIso = (local: string) => (local ? new Date(local).toISOString() : null);

function Picker({ kind, title, hint, products }: { kind: Kind; title: string; hint: string; products: Product[] }) {
  const [selected, setSelected] = useState<number[]>([]);
  const [endsAt, setEndsAt] = useState('');
  const [query, setQuery] = useState('');
  const [msg, setMsg] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    adminApi.getHomeProducts(kind).then((r) => {
      const items: HomeProductItem[] = r.data;
      setSelected(items.map((i) => i.productId));
      setEndsAt(toLocalInput(items.find((i) => i.endsAt)?.endsAt));
    }).catch(() => setError('خطا در دریافت اطلاعات'));
  }, [kind]);

  const byId = new Map(products.map((p) => [p.id, p]));
  const toggle = (id: number) =>
    setSelected((s) => (s.includes(id) ? s.filter((x) => x !== id) : s.length >= 30 ? s : [...s, id]));
  const move = (id: number, d: number) =>
    setSelected((s) => {
      const i = s.indexOf(id), j = i + d;
      if (i < 0 || j < 0 || j >= s.length) return s;
      const c = [...s]; [c[i], c[j]] = [c[j], c[i]]; return c;
    });

  const save = async () => {
    setMsg(''); setError('');
    if (kind === 'daily-deal' && selected.length > 0 && !endsAt) { setError('زمان پایان حراج را مشخص کنید'); return; }
    try {
      await adminApi.saveHomeProducts(kind, selected.map((productId) => ({ productId, endsAt: kind === 'daily-deal' ? toIso(endsAt) : null })));
      setMsg('ذخیره شد');
    } catch (e: any) { setError(e.response?.data?.message || 'ذخیره ممکن نشد'); }
  };

  const q = query.trim().toLowerCase();
  const candidates = products.filter((p) => p.active && !selected.includes(p.id) && (!q || `${p.name} ${p.brand || ''}`.toLowerCase().includes(q))).slice(0, 12);

  return (
    <div className="admin-form" style={{ marginBottom: 24 }}>
      <h3 style={{ marginBottom: 6 }}>{title}</h3>
      <p className="admin-hint">{hint}</p>
      {kind === 'daily-deal' && (
        <div className="form-group" style={{ maxWidth: 280 }}>
          <label>پایان حراج</label>
          <input type="datetime-local" value={endsAt} onChange={(e) => setEndsAt(e.target.value)} />
        </div>
      )}
      <div className="form-group">
        <label>افزودن محصول</label>
        <input placeholder="جستجوی نام محصول یا برند..." value={query} onChange={(e) => setQuery(e.target.value)} />
        {q && (
          <ul className="admin-picklist">
            {candidates.map((p) => <li key={p.id}><button type="button" onClick={() => { toggle(p.id); setQuery(''); }}>+ {p.name}</button></li>)}
            {candidates.length === 0 && <li className="admin-hint">موردی پیدا نشد</li>}
          </ul>
        )}
      </div>
      <ol className="admin-selected">
        {selected.map((id, i) => (
          <li key={id}>
            <span>{byId.get(id)?.name || `محصول ${id}`}</span>
            <span>
              <button type="button" onClick={() => move(id, -1)} disabled={i === 0}>↑</button>
              <button type="button" onClick={() => move(id, 1)} disabled={i === selected.length - 1}>↓</button>
              <button type="button" onClick={() => toggle(id)}>✕</button>
            </span>
          </li>
        ))}
        {selected.length === 0 && <li className="admin-hint">محصولی انتخاب نشده است.</li>}
      </ol>
      {error && <div className="error-msg">{error}</div>}
      {msg && <div className="success-msg">{msg}</div>}
      <button className="btn btn-primary" type="button" onClick={save}>ذخیره</button>
    </div>
  );
}

export default function AdminHomeProducts() {
  const [products, setProducts] = useState<Product[]>([]);
  useEffect(() => { adminApi.getProducts().then((r) => setProducts(r.data)).catch(() => undefined); }, []);

  return (
    <div>
      <h2 className="section-title">حراج روزانه و منتخب‌ها</h2>
      <Picker kind="daily-deal" title="ترین حراج روزانه" products={products}
              hint="این محصولات در نوار صورتی با شمارش معکوس نمایش داده می‌شوند و بعد از زمان پایان خودکار از صفحه اصلی برداشته می‌شوند. قیمت تخفیف‌خورده‌ی هر محصول را در بخش محصولات تنظیم کنید." />
      <Picker kind="featured" title="پیشنهاد مونا بیوتی (تب اول بخش منتخب)" products={products}
              hint="محصولاتی که دستی برای تب «پیشنهاد مونا بیوتی» انتخاب می‌کنید؛ ترتیب با دکمه‌های ↑ ↓ تغییر می‌کند." />
    </div>
  );
}

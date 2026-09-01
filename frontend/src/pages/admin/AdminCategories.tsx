import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { Category } from '../../types';

const emptyForm = { id: 0, name: '', slug: '', imageUrl: '', sortOrder: '' };

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState<any>(null);

  const load = () => {
    setLoading(true);
    adminApi.getCategories()
      .then((res) => setCategories(res.data))
      .catch(() => setError('خطا در دریافت دسته‌بندی‌ها'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const startEdit = (c: Category) => setForm({
    id: c.id, name: c.name, slug: c.slug, imageUrl: c.imageUrl || '', sortOrder: c.sortOrder ?? '',
  });

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const payload = {
      name: form.name,
      slug: form.slug,
      imageUrl: form.imageUrl,
      sortOrder: form.sortOrder ? Number(form.sortOrder) : null,
    };
    try {
      if (form.id) {
        await adminApi.updateCategory(form.id, payload);
      } else {
        await adminApi.createCategory(payload);
      }
      setForm(null);
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || 'ذخیره دسته‌بندی با خطا مواجه شد');
    }
  };

  const remove = async (id: number) => {
    if (!confirm('حذف این دسته‌بندی قطعی است؟')) return;
    try {
      await adminApi.deleteCategory(id);
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || 'حذف دسته‌بندی با خطا مواجه شد (ممکن است محصولی به آن متصل باشد)');
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <div className="admin-header-row">
        <h2 className="section-title">دسته‌بندی‌ها</h2>
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={() => setForm(emptyForm)}>+ دسته‌بندی جدید</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      {form && (
        <form className="admin-form" onSubmit={save}>
          <div className="admin-form-row">
            <div className="form-group">
              <label>نام</label>
              <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>اسلاگ (لاتین، یکتا)</label>
              <input value={form.slug} onChange={(e) => setForm({ ...form, slug: e.target.value })} required />
            </div>
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>آدرس تصویر</label>
              <input value={form.imageUrl} onChange={(e) => setForm({ ...form, imageUrl: e.target.value })} />
            </div>
            <div className="form-group">
              <label>ترتیب نمایش</label>
              <input type="number" value={form.sortOrder} onChange={(e) => setForm({ ...form, sortOrder: e.target.value })} />
            </div>
          </div>
          <div className="admin-form-actions">
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }}>ذخیره</button>
            <button type="button" className="btn" onClick={() => setForm(null)}>انصراف</button>
          </div>
        </form>
      )}

      <table className="admin-table">
        <thead><tr><th>نام</th><th>اسلاگ</th><th>ترتیب</th><th></th></tr></thead>
        <tbody>
          {categories.map((c) => (
            <tr key={c.id}>
              <td>{c.name}</td>
              <td>{c.slug}</td>
              <td>{c.sortOrder ?? '-'}</td>
              <td>
                <button className="link-btn" onClick={() => startEdit(c)}>ویرایش</button>
                <button className="link-btn danger" onClick={() => remove(c.id)}>حذف</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

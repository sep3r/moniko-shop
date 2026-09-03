import { useEffect, useState } from 'react';
import { adminApi } from '../../services/api';
import { Category } from '../../types';

const emptyForm = {
  id: 0,
  name: '',
  slug: '',
  imageUrl: '',
  sortOrder: '',
  parentId: '',
};

export default function AdminCategories() {
  const [categories, setCategories] = useState<Category[]>([]);
  const [tree, setTree] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState<any>(null);

  const load = () => {
    setLoading(true);
    Promise.all([adminApi.getCategories(), adminApi.getCategoryTree()])
      .then(([flatRes, treeRes]) => {
        setCategories(flatRes.data);
        setTree(treeRes.data);
      })
      .catch(() => setError('خطا در دریافت دسته‌بندی‌ها'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const startEdit = (c: Category) =>
    setForm({
      id: c.id,
      name: c.name,
      slug: c.slug,
      imageUrl: c.imageUrl || '',
      sortOrder: c.sortOrder ?? '',
      parentId: c.parentId ?? '',
    });

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    const payload = {
      name: form.name,
      slug: form.slug,
      imageUrl: form.imageUrl || null,
      sortOrder: form.sortOrder !== '' ? Number(form.sortOrder) : null,
      parentId: form.parentId !== '' ? Number(form.parentId) : null,
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
      setError(
        err.response?.data?.message ||
          err.response?.data?.error ||
          'ذخیره دسته‌بندی با خطا مواجه شد'
      );
    }
  };

  const remove = async (id: number) => {
    if (!confirm('حذف این دسته‌بندی قطعی است؟')) return;
    try {
      await adminApi.deleteCategory(id);
      load();
    } catch (err: any) {
      setError(
        err.response?.data?.message ||
          'حذف دسته‌بندی با خطا مواجه شد (ممکن است زیر‌دسته یا محصول داشته باشد)'
      );
    }
  };

  /** دسته‌های قابل انتخاب به عنوان والد (فقط ریشه‌ها، نه خودش) */
  const parentOptions = categories.filter(
    (c) => !c.parentId && (!form?.id || c.id !== form.id)
  );

  const parentName = (parentId?: number | null) => {
    if (!parentId) return '— (اصلی)';
    const p = categories.find((c) => c.id === parentId);
    return p ? p.name : `#${parentId}`;
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <div className="admin-header-row">
        <h2 className="section-title">دسته‌بندی‌ها (سلسله‌مراتبی)</h2>
        <button
          className="btn btn-primary"
          style={{ width: 'auto' }}
          onClick={() => setForm({ ...emptyForm })}
        >
          + دسته‌بندی جدید
        </button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      {form && (
        <form className="admin-form" onSubmit={save}>
          <div className="admin-form-row">
            <div className="form-group">
              <label>نام</label>
              <input
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                required
              />
            </div>
            <div className="form-group">
              <label>اسلاگ (لاتین، یکتا)</label>
              <input
                value={form.slug}
                onChange={(e) => setForm({ ...form, slug: e.target.value })}
                required
                dir="ltr"
                style={{ textAlign: 'left' }}
              />
            </div>
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>دسته والد</label>
              <select
                value={form.parentId}
                onChange={(e) =>
                  setForm({ ...form, parentId: e.target.value })
                }
              >
                <option value="">— بدون والد (دسته اصلی) —</option>
                {parentOptions.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
              <small style={{ color: '#888', fontSize: 12 }}>
                اگر والد انتخاب شود، این دسته در مگامنو زیر آن نمایش داده می‌شود.
              </small>
            </div>
            <div className="form-group">
              <label>ترتیب نمایش</label>
              <input
                type="number"
                value={form.sortOrder}
                onChange={(e) =>
                  setForm({ ...form, sortOrder: e.target.value })
                }
              />
            </div>
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>آدرس تصویر (اختیاری)</label>
              <input
                value={form.imageUrl}
                onChange={(e) =>
                  setForm({ ...form, imageUrl: e.target.value })
                }
                dir="ltr"
                style={{ textAlign: 'left' }}
              />
            </div>
          </div>
          <div className="admin-form-actions">
            <button
              type="submit"
              className="btn btn-primary"
              style={{ width: 'auto' }}
            >
              ذخیره
            </button>
            <button type="button" className="btn" onClick={() => setForm(null)}>
              انصراف
            </button>
          </div>
        </form>
      )}

      {/* نمایش درختی */}
      <h3 style={{ margin: '24px 0 12px', fontSize: 16 }}>ساختار درختی</h3>
      <div className="category-tree">
        {tree.length === 0 && (
          <p style={{ color: '#999' }}>هنوز دسته‌ای تعریف نشده.</p>
        )}
        {tree.map((root) => (
          <div key={root.id} className="tree-root">
            <div className="tree-row">
              <strong>{root.name}</strong>
              <span className="tree-slug">{root.slug}</span>
              <span className="tree-order">#{root.sortOrder ?? '-'}</span>
              <span>
                <button className="link-btn" onClick={() => startEdit(root)}>
                  ویرایش
                </button>
                <button
                  className="link-btn danger"
                  onClick={() => remove(root.id)}
                >
                  حذف
                </button>
              </span>
            </div>
            {root.children && root.children.length > 0 && (
              <ul className="tree-children">
                {root.children
                  .slice()
                  .sort(
                    (a, b) => (a.sortOrder ?? 999) - (b.sortOrder ?? 999)
                  )
                  .map((child) => (
                    <li key={child.id} className="tree-row">
                      <span>↳ {child.name}</span>
                      <span className="tree-slug">{child.slug}</span>
                      <span className="tree-order">
                        #{child.sortOrder ?? '-'}
                      </span>
                      <span>
                        <button
                          className="link-btn"
                          onClick={() => startEdit(child)}
                        >
                          ویرایش
                        </button>
                        <button
                          className="link-btn danger"
                          onClick={() => remove(child.id)}
                        >
                          حذف
                        </button>
                      </span>
                    </li>
                  ))}
              </ul>
            )}
          </div>
        ))}
      </div>

      {/* جدول تخت */}
      <h3 style={{ margin: '32px 0 12px', fontSize: 16 }}>لیست کامل</h3>
      <table className="admin-table">
        <thead>
          <tr>
            <th>نام</th>
            <th>اسلاگ</th>
            <th>والد</th>
            <th>ترتیب</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          {categories
            .slice()
            .sort((a, b) => {
              // ریشه‌ها اول، بعد بر اساس sortOrder
              const ap = a.parentId ?? 0;
              const bp = b.parentId ?? 0;
              if (ap !== bp) return ap - bp;
              return (a.sortOrder ?? 999) - (b.sortOrder ?? 999);
            })
            .map((c) => (
              <tr key={c.id}>
                <td>
                  {c.parentId ? (
                    <span style={{ paddingRight: 16 }}>↳ {c.name}</span>
                  ) : (
                    <strong>{c.name}</strong>
                  )}
                </td>
                <td dir="ltr" style={{ textAlign: 'left' }}>
                  {c.slug}
                </td>
                <td>{parentName(c.parentId)}</td>
                <td>{c.sortOrder ?? '-'}</td>
                <td>
                  <button className="link-btn" onClick={() => startEdit(c)}>
                    ویرایش
                  </button>
                  <button
                    className="link-btn danger"
                    onClick={() => remove(c.id)}
                  >
                    حذف
                  </button>
                </td>
              </tr>
            ))}
        </tbody>
      </table>
    </div>
  );
}

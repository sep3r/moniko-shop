import { useEffect, useRef, useState } from 'react';
import { adminApi } from '../../services/api';
import { Category, Product } from '../../types';
import { getProductImageUrl, FALLBACK_IMAGE } from '../../utils/imageUrl';
import { prepareProductImage } from '../../utils/imagePrepare';

const MIN_IMAGES = 1;
const MAX_IMAGES = 5;
const MAX_SPECS = 30;

const toFa = (n: number) => new Intl.NumberFormat('fa-IR').format(n);
const formatPrice = (price: number) => toFa(price) + ' تومان';

interface ImgItem {
  key: string;
  /** set for images that already exist on the server */
  id?: number;
  /** set for newly chosen files */
  file?: File;
  url: string;
}

interface SpecRow {
  key: string;
  value: string;
}

const emptyForm = {
  id: 0,
  name: '',
  subtitle: '',
  description: '',
  price: '',
  discountPrice: '',
  brand: '',
  categoryId: '' as number | string,
  stock: '',
  active: true,
};

export default function AdminProducts() {
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [form, setForm] = useState<typeof emptyForm | null>(null);
  const [imgs, setImgs] = useState<ImgItem[]>([]);
  const [specs, setSpecs] = useState<SpecRow[]>([]);
  const [saving, setSaving] = useState(false);
  const [processing, setProcessing] = useState(false);
  const [legacyImage, setLegacyImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const counter = useRef(0);

  const load = () => {
    setLoading(true);
    Promise.all([adminApi.getProducts(), adminApi.getCategories()])
      .then(([p, c]) => {
        setProducts(p.data);
        setCategories(c.data);
      })
      .catch(() => setError('خطا در دریافت اطلاعات'))
      .finally(() => setLoading(false));
  };

  useEffect(load, []);

  const revoke = (items: ImgItem[]) => {
    items.forEach((i) => { if (i.file) URL.revokeObjectURL(i.url); });
  };

  const closeForm = () => {
    revoke(imgs);
    setForm(null);
    setImgs([]);
    setSpecs([]);
    setLegacyImage(false);
  };

  const startCreate = () => {
    revoke(imgs);
    setError('');
    setForm({ ...emptyForm, categoryId: categories[0]?.id ?? '' });
    setImgs([]);
    setSpecs([]);
    setLegacyImage(false);
  };

  const startEdit = (p: Product) => {
    revoke(imgs);
    setError('');
    setForm({
      id: p.id,
      name: p.name,
      subtitle: p.subtitle || '',
      description: p.description || '',
      price: String(p.price),
      discountPrice: p.discountPrice ? String(p.discountPrice) : '',
      brand: p.brand || '',
      categoryId: p.category?.id ?? '',
      stock: String(p.stock),
      active: p.active,
    });
    setImgs((p.images ?? []).map((i) => ({ key: `e${i.id}`, id: i.id, url: getProductImageUrl(i.url) })));
    setSpecs((p.specs ?? []).map((s) => ({ key: s.key, value: s.value })));
    // old product that only has an external picture link: an upload is required to save
    setLegacyImage(!(p.images && p.images.length > 0) && !!p.imageUrl);
  };

  const onFiles = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const chosen = Array.from(e.target.files ?? []);
    e.target.value = ''; // allow choosing the same file again
    if (chosen.length === 0) return;
    setError('');

    const free = MAX_IMAGES - imgs.length;
    if (free <= 0) {
      setError(`حداکثر ${toFa(MAX_IMAGES)} تصویر برای هر محصول مجاز است`);
      return;
    }
    const accepted = chosen.slice(0, free);
    if (chosen.length > free) {
      setError(`فقط ${toFa(free)} تصویر دیگر قابل افزودن بود؛ بقیه نادیده گرفته شد (حداکثر ${toFa(MAX_IMAGES)} تصویر)`);
    }

    setProcessing(true);
    const added: ImgItem[] = [];
    for (const file of accepted) {
      try {
        const prepared = await prepareProductImage(file);
        counter.current += 1;
        added.push({ key: `n${counter.current}`, file: prepared, url: URL.createObjectURL(prepared) });
      } catch (err: any) {
        setError(`${file.name}: ${err.message || 'تصویر نامعتبر است'}`);
      }
    }
    setImgs((prev) => [...prev, ...added].slice(0, MAX_IMAGES));
    setProcessing(false);
  };

  const removeImg = (index: number) => {
    const item = imgs[index];
    if (item?.file) URL.revokeObjectURL(item.url);
    setImgs(imgs.filter((_, i) => i !== index));
  };

  const makeMain = (index: number) => {
    const copy = [...imgs];
    const [item] = copy.splice(index, 1);
    setImgs([item, ...copy]);
  };

  const moveImg = (index: number, delta: number) => {
    const target = index + delta;
    if (target < 0 || target >= imgs.length) return;
    const copy = [...imgs];
    [copy[index], copy[target]] = [copy[target], copy[index]];
    setImgs(copy);
  };

  const updateSpec = (index: number, patch: Partial<SpecRow>) => {
    setSpecs(specs.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  };

  const save = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!form || saving || processing) return;
    setError('');

    if (imgs.length < MIN_IMAGES) {
      setError('ثبت محصول بدون تصویر ممکن نیست؛ حداقل یک تصویر اضافه کنید');
      return;
    }
    if (imgs.length > MAX_IMAGES) {
      setError(`حداکثر ${toFa(MAX_IMAGES)} تصویر برای هر محصول مجاز است`);
      return;
    }
    const cleanSpecs = specs
      .map((s) => ({ key: s.key.trim(), value: s.value.trim() }))
      .filter((s) => s.key || s.value);
    if (cleanSpecs.some((s) => !s.key || !s.value)) {
      setError('در هر ردیف مشخصات، هم عنوان و هم مقدار باید پر شود (یا ردیف را حذف کنید)');
      return;
    }

    const fd = new FormData();
    fd.append('name', form.name);
    if (form.subtitle) fd.append('subtitle', form.subtitle);
    if (form.description) fd.append('description', form.description);
    fd.append('price', String(Number(form.price)));
    if (form.discountPrice) fd.append('discountPrice', String(Number(form.discountPrice)));
    if (form.brand) fd.append('brand', form.brand);
    fd.append('categoryId', String(Number(form.categoryId)));
    fd.append('stock', String(Number(form.stock)));
    fd.append('active', String(form.active));
    fd.append('specs', JSON.stringify(cleanSpecs));

    // final order of images: e<id> = keep existing, n<i> = i-th new file
    const newFiles: File[] = [];
    const order = imgs.map((im) => {
      if (im.id !== undefined) return `e${im.id}`;
      newFiles.push(im.file as File);
      return `n${newFiles.length - 1}`;
    });
    fd.append('imageOrder', order.join(','));
    newFiles.forEach((f) => fd.append('images', f));

    setSaving(true);
    try {
      if (form.id) {
        await adminApi.updateProduct(form.id, fd);
      } else {
        await adminApi.createProduct(fd);
      }
      closeForm();
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || err.response?.data?.error || 'ذخیره محصول با خطا مواجه شد');
    } finally {
      setSaving(false);
    }
  };

  const remove = async (id: number) => {
    if (!confirm('حذف این محصول قطعی است؟')) return;
    try {
      await adminApi.deleteProduct(id);
      load();
    } catch (err: any) {
      setError(err.response?.data?.message || 'حذف محصول با خطا مواجه شد');
    }
  };

  if (loading) return <div className="loading">در حال بارگذاری...</div>;

  return (
    <div>
      <div className="admin-header-row">
        <h2 className="section-title">محصولات</h2>
        <button className="btn btn-primary" style={{ width: 'auto' }} onClick={startCreate}>+ محصول جدید</button>
      </div>
      {error && <div className="error-msg">{error}</div>}

      {form && (
        <form className="admin-form" onSubmit={save}>
          <div className="form-group">
            <label>نام محصول</label>
            <input value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} maxLength={255} required />
          </div>
          <div className="form-group">
            <label>عنوان دوم / نام انگلیسی (اختیاری)</label>
            <input
              dir="ltr"
              value={form.subtitle}
              onChange={(e) => setForm({ ...form, subtitle: e.target.value })}
              maxLength={255}
              placeholder="e.g. Matte Foundation 30ml"
            />
          </div>
          <div className="form-group">
            <label>توضیحات (معرفی محصول)</label>
            <textarea
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              rows={4}
              maxLength={10000}
              style={{ width: '100%', padding: '12px 14px', border: '1px solid #A8BFAE', borderRadius: 8, fontFamily: 'inherit' }}
            />
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>قیمت (تومان)</label>
              <input type="number" min={1} value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} required />
            </div>
            <div className="form-group">
              <label>قیمت با تخفیف</label>
              <input type="number" min={0} value={form.discountPrice} onChange={(e) => setForm({ ...form, discountPrice: e.target.value })} />
            </div>
            <div className="form-group">
              <label>موجودی</label>
              <input type="number" min={0} value={form.stock} onChange={(e) => setForm({ ...form, stock: e.target.value })} required />
            </div>
          </div>
          <div className="admin-form-row">
            <div className="form-group">
              <label>برند</label>
              <input value={form.brand} onChange={(e) => setForm({ ...form, brand: e.target.value })} />
            </div>
            <div className="form-group">
              <label>دسته‌بندی</label>
              <select value={form.categoryId} onChange={(e) => setForm({ ...form, categoryId: e.target.value })} required>
                {categories.map((c) => <option key={c.id} value={c.id}>{c.name}</option>)}
              </select>
            </div>
          </div>

          {/* ---------- Images (1..5) ---------- */}
          <div className="form-group">
            <label>
              تصاویر محصول <span className="req">*</span>
              <span className="img-counter"> ({toFa(imgs.length)} از {toFa(MAX_IMAGES)})</span>
            </label>
            <div className="img-manager">
              {imgs.map((im, i) => (
                <div className={'img-tile' + (i === 0 ? ' main' : '')} key={im.key}>
                  <img src={im.url} alt="" />
                  {i === 0 && <span className="img-badge">تصویر اصلی</span>}
                  <div className="img-actions">
                    {i > 0 && <button type="button" onClick={() => makeMain(i)}>اصلی</button>}
                    {i > 0 && <button type="button" onClick={() => moveImg(i, -1)} aria-label="جابجایی به راست">›</button>}
                    {i < imgs.length - 1 && <button type="button" onClick={() => moveImg(i, 1)} aria-label="جابجایی به چپ">‹</button>}
                    <button type="button" className="danger" onClick={() => removeImg(i)}>حذف</button>
                  </div>
                </div>
              ))}
              {imgs.length < MAX_IMAGES && (
                <button
                  type="button"
                  className="img-add"
                  disabled={processing}
                  onClick={() => fileInputRef.current?.click()}
                >
                  {processing ? 'در حال آماده‌سازی...' : '+ افزودن تصویر'}
                </button>
              )}
              <input
                ref={fileInputRef}
                type="file"
                multiple
                hidden
                accept="image/jpeg,image/png,image/webp"
                onChange={onFiles}
              />
            </div>
            <p className="field-hint">
              حداقل {toFa(MIN_IMAGES)} و حداکثر {toFa(MAX_IMAGES)} تصویر (JPG، PNG یا WebP، هر کدام تا ۵ مگابایت). تصویر اول، تصویر اصلی محصول است.
            </p>
            {legacyImage && (
              <p className="field-hint warn">این محصول قبلاً فقط با لینک خارجی تصویر داشت؛ برای ذخیره، تصویر آن را آپلود کنید.</p>
            )}
          </div>

          {/* ---------- Specs ---------- */}
          <div className="form-group">
            <label>مشخصات محصول (جدول مشخصات در صفحه محصول)</label>
            <div className="spec-editor">
              {specs.map((s, i) => (
                <div className="spec-row" key={i}>
                  <input
                    placeholder="عنوان (مثلاً: حجم)"
                    value={s.key}
                    maxLength={100}
                    onChange={(e) => updateSpec(i, { key: e.target.value })}
                  />
                  <input
                    placeholder="مقدار (مثلاً: ۳۰ میلی‌لیتر)"
                    value={s.value}
                    maxLength={500}
                    onChange={(e) => updateSpec(i, { value: e.target.value })}
                  />
                  <button type="button" className="link-btn danger" onClick={() => setSpecs(specs.filter((_, idx) => idx !== i))}>حذف</button>
                </div>
              ))}
              {specs.length < MAX_SPECS && (
                <button type="button" className="btn" onClick={() => setSpecs([...specs, { key: '', value: '' }])}>
                  + افزودن ردیف مشخصات
                </button>
              )}
            </div>
          </div>

          <div className="form-group">
            <label><input type="checkbox" checked={form.active} onChange={(e) => setForm({ ...form, active: e.target.checked })} /> فعال (نمایش در فروشگاه)</label>
          </div>
          <div className="admin-form-actions">
            <button type="submit" className="btn btn-primary" style={{ width: 'auto' }} disabled={saving || processing}>
              {saving ? 'در حال ذخیره...' : 'ذخیره'}
            </button>
            <button type="button" className="btn" onClick={closeForm} disabled={saving}>انصراف</button>
          </div>
        </form>
      )}

      <table className="admin-table">
        <thead>
          <tr><th>تصویر</th><th>نام</th><th>دسته</th><th>قیمت</th><th>موجودی</th><th>وضعیت</th><th></th></tr>
        </thead>
        <tbody>
          {products.map((p) => (
            <tr key={p.id}>
              <td>
                <img
                  src={p.imageUrl ? getProductImageUrl(p.imageUrl) : FALLBACK_IMAGE}
                  alt=""
                  onError={(e) => { e.currentTarget.src = FALLBACK_IMAGE; }}
                  style={{ width: 48, height: 48, objectFit: 'cover', borderRadius: 6, background: '#D8F0E5' }}
                />
              </td>
              <td>{p.name}</td>
              <td>{p.category?.name}</td>
              <td>{formatPrice(p.discountPrice ?? p.price)}</td>
              <td>{p.stock}</td>
              <td>{p.active ? 'فعال' : 'غیرفعال'}</td>
              <td>
                <button className="link-btn" onClick={() => startEdit(p)}>ویرایش</button>
                <button className="link-btn danger" onClick={() => remove(p.id)}>حذف</button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

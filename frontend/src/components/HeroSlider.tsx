import { useEffect, useState } from 'react';
import { Banner } from '../types';
import { getProductImageUrl } from '../utils/imageUrl';
import SmartLink from './SmartLink';
import { Chevron } from './Carousel';

/** اسلایدر اصلی با پخش خودکار، نقطه‌های پایین و دکمه‌های کناری. */
export default function HeroSlider({ banners }: { banners: Banner[] }) {
  const [i, setI] = useState(0);
  const n = banners.length;

  useEffect(() => {
    if (n < 2) return;
    const t = window.setInterval(() => setI((v) => (v + 1) % n), 5000);
    return () => window.clearInterval(t);
  }, [n, i]);

  if (n === 0) return null;
  const go = (d: number) => setI((v) => (v + d + n) % n);

  return (
    <div className="kh-hero">
      <div className="kh-hero-viewport">
        <div className="kh-hero-track" style={{ transform: `translateX(${i * 100}%)` }}>
          {banners.map((b) => (
            <SmartLink key={b.id} to={b.linkUrl} className="kh-hero-slide" title={b.title}>
              <img src={getProductImageUrl(b.imageUrl)} alt={b.title || ''} loading={b.id === banners[0].id ? 'eager' : 'lazy'} fetchPriority={b.id === banners[0].id ? 'high' : 'auto'} decoding="async" />
            </SmartLink>
          ))}
        </div>
      </div>
      {n > 1 && (
        <>
          <button type="button" className="kh-arrow kh-arrow-prev" aria-label="قبلی" onClick={() => go(-1)}><Chevron dir="right" /></button>
          <button type="button" className="kh-arrow kh-arrow-next" aria-label="بعدی" onClick={() => go(1)}><Chevron dir="left" /></button>
          <div className="kh-dots">
            {banners.map((b, idx) => (
              <button key={b.id} type="button" aria-label={`اسلاید ${idx + 1}`}
                      className={idx === i ? 'active' : ''} onClick={() => setI(idx)} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}

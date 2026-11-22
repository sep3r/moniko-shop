// بررسی «قفل پالت»: در CSS/TSX/HTML/SVG فقط ۴ رنگ پالت مونا بیوتی + مشکی (برای فونت‌ها) مجاز است.
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join, extname } from 'node:path';

const ROOTS = ['src', 'public', 'index.html'];
const EXT = new Set(['.css', '.tsx', '.ts', '.html', '.svg']);
const ALLOWED = [
  /^#f58f83$/i, /^#d8f0e5$/i, /^#a8bfae$/i, /^#fffcf8$/i, /^#000(000)?$/i,
  /^rgba\(0,0,0,[\d.]+\)$/, /^rgba\(245,143,131,[\d.]+\)$/, /^rgba\(255,252,248,[\d.]+\)$/,
  /^rgba\(168,191,174,[\d.]+\)$/, /^rgba\(216,240,229,[\d.]+\)$/,
];
const COLOR = /#(?:[0-9a-fA-F]{8}|[0-9a-fA-F]{6}|[0-9a-fA-F]{4}|[0-9a-fA-F]{3})\b|rgba?\([^)]*\)|(?<![\w-])(?:white|black|red|green|blue|gray|grey|orange|yellow|purple|pink)(?![\w-])/g;

function walk(p, out = []) {
  const st = statSync(p);
  if (st.isDirectory()) for (const f of readdirSync(p)) walk(join(p, f), out);
  else if (EXT.has(extname(p)) || p.endsWith('index.html')) out.push(p);
  return out;
}

let bad = 0;
for (const root of ROOTS) {
  let files = [];
  try { files = walk(root); } catch { continue; }
  for (const f of files) {
    // کامنت‌ها و رشته‌های متنی فارسی مهم نیستند؛ فقط مقادیر رنگ
    const text = readFileSync(f, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '').replace(/<!--[\s\S]*?-->/g, '');
    for (const m of text.matchAll(COLOR)) {
      const c = m[0].replace(/\s+/g, '');
      if (/^(white|black|red|green|blue|gray|grey|orange|yellow|purple|pink)$/.test(c) && f.endsWith('.tsx')) continue; // کلمات عادی در متن JSX
      if (!ALLOWED.some((re) => re.test(c))) { console.error(`${f}: رنگ خارج از پالت: ${m[0]}`); bad++; }
    }
  }
}
if (bad) { console.error(`\n${bad} رنگ خارج از پالت پیدا شد.`); process.exit(1); }
console.log('palette OK: فقط ۴ رنگ پالت + مشکی');

// آدرس بک‌اند از VITE_API_URL گرفته می‌شود (مثلاً https://xxx.runflare.run/api)
// و پسوند /api برای ساخت آدرس تصاویر حذف می‌شود.
const API_BASE = ((import.meta.env.VITE_API_URL as string | undefined) || '')
    .replace(/\/api\/?$/, '')
    .replace(/\/$/, '');

export function getProductImageUrl(
    imageUrl?: string | null,
    productId?: number
): string {
  if (imageUrl) {
    if (
        imageUrl.startsWith('http://') ||
        imageUrl.startsWith('https://') ||
        imageUrl.startsWith('data:')
    ) {
      return imageUrl;
    }

    if (imageUrl.startsWith('/')) {
      return `${API_BASE}${imageUrl}`;
    }

    return `${API_BASE}/${imageUrl}`;
  }

  if (productId) {
    return `${API_BASE}/api/products/${productId}/image`;
  }

  return '';
}

export const FALLBACK_IMAGE =
    'data:image/svg+xml;charset=UTF-8,' +
    encodeURIComponent(
        '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400">' +
        '<rect width="400" height="400" fill="#f5f5f5"/>' +
        '<text x="200" y="205" text-anchor="middle" ' +
        'font-family="Arial" font-size="24" fill="#999">Moniko</text>' +
        '</svg>'
    );
const API_BASE = (import.meta.env.VITE_API_URL || '/api').replace(/\/$/, '');

export function getProductImageUrl(imageUrl?: string | null, productId?: number) {
  if (imageUrl) {
    if (imageUrl.startsWith('http://') || imageUrl.startsWith('https://') || imageUrl.startsWith('data:')) return imageUrl;
    if (imageUrl.startsWith('/api/')) {
      const origin = API_BASE.replace(/\/api$/, '');
      return origin + imageUrl;
    }
    return imageUrl;
  }
  if (productId) return `${API_BASE}/products/${productId}/image`;
  return '';
}

export const FALLBACK_IMAGE = 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="400" height="400" viewBox="0 0 400 400"><rect width="400" height="400" fill="#f5f5f5"/><text x="200" y="205" text-anchor="middle" font-family="Arial" font-size="24" fill="#999">Moniko</text></svg>'
);

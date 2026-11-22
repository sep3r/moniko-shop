// Prepares a product photo before upload: only jpg/png/webp, big photos are scaled down
// (max 1600px) and re-encoded so uploads stay fast and under the server limit (5MB).
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
const MAX_DIMENSION = 1600;
const SKIP_RESIZE_BELOW = 1.2 * 1024 * 1024;
const ALLOWED = ['image/jpeg', 'image/png', 'image/webp'];

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      URL.revokeObjectURL(url);
      resolve(img);
    };
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error('فایل انتخاب‌شده تصویر معتبری نیست'));
    };
    img.src = url;
  });
}

export async function prepareProductImage(file: File): Promise<File> {
  if (!ALLOWED.includes(file.type)) {
    throw new Error('فقط تصاویر JPG، PNG و WebP مجاز هستند');
  }
  const img = await loadImage(file);
  const longest = Math.max(img.naturalWidth, img.naturalHeight);

  let result: File = file;
  if (longest > MAX_DIMENSION || file.size > SKIP_RESIZE_BELOW) {
    const scale = Math.min(1, MAX_DIMENSION / longest);
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(img.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(img.naturalHeight * scale));
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.fillStyle = '#FFFCF8'; // JPEG has no transparency
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.drawImage(img, 0, 0, canvas.width, canvas.height);
      const blob: Blob | null = await new Promise((resolve) => canvas.toBlob(resolve, 'image/jpeg', 0.88));
      if (blob && blob.size < file.size) {
        const name = file.name.replace(/\.[^.]+$/, '') + '.jpg';
        result = new File([blob], name, { type: 'image/jpeg' });
      }
    }
  }

  if (result.size > MAX_IMAGE_BYTES) {
    throw new Error('حجم هر تصویر نباید بیشتر از ۵ مگابایت باشد');
  }
  return result;
}

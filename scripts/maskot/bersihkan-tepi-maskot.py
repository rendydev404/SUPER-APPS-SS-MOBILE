"""Menghapus pinggiran putih (fringe) di tepi siluet maskot.

PNG mentah dipotong dari latar putih dengan alpha biner 0/255, jadi piksel tepinya
masih bercampur putih dan tampak seperti garis terang di atas latar peach beranda.
Skrip ini menaksir alpha tiap piksel di pita tepi 4 px dari seberapa "memutih" ia
dibanding warna inti terdekat, lalu meng-unpremultiply warnanya. Hasilnya menjadi
sumber untuk pisah-lapisan-maskot.py. Jalankan dari root repo:
    python scripts/maskot/bersihkan-tepi-maskot.py
"""
import numpy as np
from PIL import Image, ImageFilter

BAND = 4
src = Image.open('scripts/maskot/img_mascot_chef_header_raw.png').convert('RGBA')
a = np.array(src).astype(float); A = a[..., 3]
M = A > 0

def erode(m, k):
    return np.array(Image.fromarray((m * 255).astype(np.uint8)).filter(ImageFilter.MinFilter(k))) > 0

core = erode(M, 2 * BAND + 1)
ring = M & ~core
# Warna inti terdekat: isi pita dengan rata-rata tetangga inti secara iteratif.
rgb = a[..., :3].copy(); known = core.copy()
for _ in range(BAND + 2):
    img = Image.fromarray(np.dstack([rgb, known * 255.0]).astype(np.uint8))
    blur = np.array(img.filter(ImageFilter.BoxBlur(1))).astype(float)
    w = blur[..., 3] / 255.0
    fill = (w > 0.02) & ~known
    rgb[fill] = blur[..., :3][fill] / np.maximum(w[fill], 1e-3)[..., None]
    known |= fill
c = a[..., :3]
den = np.maximum(255 - rgb, 8.0)
alpha_est = np.clip(((255 - c) / den).mean(axis=2), 0, 1)
al = np.where(ring, alpha_est, A / 255.0)
col = np.where(ring[..., None], np.clip((c - (1 - al[..., None]) * 255) / np.maximum(al[..., None], 0.05), 0, 255), c)
out = a.copy(); out[..., :3] = col; out[..., 3] = al * 255
out[..., 3][out[..., 3] < 8] = 0
Image.fromarray(out.astype(np.uint8)).save('scripts/maskot/img_mascot_chef_header.png')
print('selesai; piksel pita tepi', int(ring.sum()))

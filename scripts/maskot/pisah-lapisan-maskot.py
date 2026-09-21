"""Memotong img_mascot_chef_header.png menjadi tiga lapis untuk animasi lambaian
di InteractiveChefMascot.kt: badan (tanpa tangan), tangan (+ rok kulit di balik
manset), dan manset (digambar paling atas). Ukuran kanvas ketiganya sama dengan
aslinya supaya bisa ditumpuk tanpa offset. Jalankan dari root repo:
    python scripts/maskot/pisah-lapisan-maskot.py
Butuh Pillow + numpy. Titik putar tangan (255,292) & posisi mata ada di file Kotlin."""
import numpy as np
from PIL import Image, ImageFilter

im = Image.open('scripts/maskot/img_mascot_chef_header.png').convert('RGBA'); W, H = im.size
a = np.array(im).astype(int); R, G, B, A = a[..., 0], a[..., 1], a[..., 2], a[..., 3]
yy, xx = np.mgrid[0:H, 0:W]
opaque = A > 0
cream = (R > 185) & (G > 170) & (B > 140) & (R - B < 60)
red = (R > 120) & (G < 90) & (B < 90)
dark = (R < 95) & (G < 80) & (B < 75)
bbox = (xx >= 85) & (xx <= 345) & (yy >= 70) & (yy <= 372)
# Garis jahitan tepat di tepi luar manset; sisi tangan = cross > 0.
Ax, Ay, Bx, By = 312, 230, 194, 354
cross = (Bx - Ax) * (yy - Ay) - (By - Ay) * (xx - Ax)
handside = cross > 0
hand_side = opaque & bbox & handside
wrist_in = opaque & bbox & ~handside & ~cream & ~red & ~dark & ~((xx > 300) & (yy > 250))
hand_all = hand_side | wrist_in
cuffbox = (xx >= 185) & (xx <= 370) & (yy >= 205) & (yy <= 390)
cuff = opaque & cuffbox & ~handside & ~wrist_in
m = np.array(Image.fromarray((hand_side * 255).astype(np.uint8)).filter(ImageFilter.MaxFilter(35))) > 0
skirt = m & ~handside & opaque & cuffbox & ~wrist_in
near = hand_side & (np.abs(cross) / np.hypot(Bx - Ax, By - Ay) < 22) & ~dark
col = [int(a[..., i][near].mean()) for i in range(3)]
body = a.copy(); body[hand_side] = [0, 0, 0, 0]
hand = np.zeros_like(a); hand[skirt] = col + [255]; hand[hand_all] = a[hand_all]
cuffL = np.zeros_like(a); cuffL[cuff] = a[cuff]
out = 'feature/home/src/main/res/drawable-nodpi/'
for n, arr in [('body', body), ('hand', hand), ('cuff', cuffL)]:
    Image.fromarray(arr.astype(np.uint8)).save(out + f'img_mascot_chef_{n}.png')
print('selesai; warna rok kulit', col)

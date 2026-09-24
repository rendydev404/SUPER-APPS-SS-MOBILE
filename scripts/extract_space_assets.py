import cv2
import numpy as np
from PIL import Image
import os

def clean_cutout(img_crop, bg_thresh=245, low_cut=0.06, high_cut=0.22):
    rgb = cv2.cvtColor(img_crop, cv2.COLOR_BGR2RGB).astype(np.float32) / 255.0
    gray = cv2.cvtColor(img_crop, cv2.COLOR_BGR2GRAY)
    
    # Distance from white (0 = white, 1 = black)
    dist_white = np.linalg.norm(1.0 - rgb, axis=2) / np.sqrt(3.0)
    
    h, w = img_crop.shape[:2]
    flood_mask = np.zeros((h + 2, w + 2), np.uint8)
    near_white = (gray > bg_thresh).astype(np.uint8) * 255
    
    for pt in [(0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1), (w // 2, 0), (0, h // 2), (w - 1, h // 2), (w // 2, h - 1)]:
        if near_white[pt[1], pt[0]] == 255:
            cv2.floodFill(near_white, flood_mask, pt, 128)
            
    is_bg = (near_white == 128)
    
    alpha = np.ones((h, w), dtype=np.float32)
    alpha_bg = np.clip((dist_white - low_cut) / (high_cut - low_cut), 0.0, 1.0)
    alpha[is_bg] = alpha_bg[is_bg]
    
    # Unmultiply color with safety clamping
    a_safe = np.maximum(alpha, 0.05)
    unmult = np.clip((rgb - (1.0 - alpha[:, :, None])) / a_safe[:, :, None], 0.0, 1.0)
    
    res = np.zeros((h, w, 4), dtype=np.uint8)
    res[:, :, :3] = (unmult * 255.0).astype(np.uint8)
    res[:, :, 3] = (alpha * 255.0).astype(np.uint8)
    return res

def trim_alpha(rgba, pad=4):
    alpha = rgba[:, :, 3]
    pts = cv2.findNonZero((alpha > 15).astype(np.uint8))
    if pts is None:
        return rgba
    x, y, w, h = cv2.boundingRect(pts)
    x1 = max(0, x - pad)
    y1 = max(0, y - pad)
    x2 = min(rgba.shape[1], x + w + pad)
    y2 = min(rgba.shape[0], y + h + pad)
    return rgba[y1:y2, x1:x2]

def main():
    brain_dir = r"C:\Users\Creator MPB\.gemini\antigravity\brain\445f1c55-c928-4cc8-a17f-93398842fcf3"
    out_dir = r"d:\PROJECT-APPS-NATIVE\SUPER-APPS-SS-MOBILE\feature\home\src\main\res\drawable-nodpi"
    os.makedirs(out_dir, exist_ok=True)
    
    # 1. Main Shawarma Planet: crop only y < 860 to completely exclude the shadow
    img1 = cv2.imread(os.path.join(brain_dir, "planet_shawarma_interactive_1790221895392.jpg"))
    if img1 is not None:
        h, w = img1.shape[:2]
        crop1 = img1[0:860, 0:w]
        rgba1 = clean_cutout(crop1, bg_thresh=245, low_cut=0.05, high_cut=0.18)
        rgba1 = trim_alpha(rgba1)
        out1 = os.path.join(out_dir, "planet_shawarma_char.png")
        Image.fromarray(rgba1).save(out1, "PNG")
        print(f"Saved: {out1} ({rgba1.shape[1]}x{rgba1.shape[0]})")
        
    # 2. Cosmic Buddies (Garlic, Tomato, Fry, Pickle)
    img2 = cv2.imread(os.path.join(brain_dir, "cosmic_food_buddies_1790221960942.jpg"))
    if img2 is not None:
        # Garlic: [92, 46, 375, 444] -> bbox with margin
        g_crop = img2[30:500, 80:480]
        g_rgba = trim_alpha(clean_cutout(g_crop, bg_thresh=245, low_cut=0.05, high_cut=0.18))
        Image.fromarray(g_rgba).save(os.path.join(out_dir, "space_garlic_astro.png"), "PNG")
        
        # Tomato: [554, 69, 431, 386]
        t_crop = img2[50:470, 540:990]
        t_rgba = trim_alpha(clean_cutout(t_crop, bg_thresh=245, low_cut=0.05, high_cut=0.18))
        Image.fromarray(t_rgba).save(os.path.join(out_dir, "space_tomato_planet.png"), "PNG")
        
        # French Fry: [31, 542, 440, 457]
        # Use slightly higher low_cut to eliminate stardust hazy background and keep stars sharp
        f_crop = img2[520:1000, 20:470]
        f_rgba = trim_alpha(clean_cutout(f_crop, bg_thresh=240, low_cut=0.08, high_cut=0.25))
        Image.fromarray(f_rgba).save(os.path.join(out_dir, "space_fry_comet.png"), "PNG")
        
        # Pickle: [545, 573, 430, 397]
        p_crop = img2[550:980, 530:970]
        p_rgba = trim_alpha(clean_cutout(p_crop, bg_thresh=245, low_cut=0.05, high_cut=0.18))
        Image.fromarray(p_rgba).save(os.path.join(out_dir, "space_pickle_moon.png"), "PNG")
        print("Saved buddies (Garlic, Tomato, Fry, Pickle)")
        
    # 3. Space Creatures (Chili, Onion)
    img3 = cv2.imread(os.path.join(brain_dir, "space_food_creatures_1790221994967.jpg"))
    if img3 is not None:
        h, w = img3.shape[:2]
        # Chili rocket is on the left: X in [0, 490], Y in [140, 840]
        # We can mask out any pixel with X > 470 and Y > 300 where onion's ring might graze
        c_crop = img3[140:840, 20:470].copy()
        c_rgba = trim_alpha(clean_cutout(c_crop, bg_thresh=245, low_cut=0.06, high_cut=0.20))
        Image.fromarray(c_rgba).save(os.path.join(out_dir, "space_chili_rocket.png"), "PNG")
        
        # Onion Saturn is on the right: X in [480, 1000], Y in [280, 800]
        o_crop = img3[280:800, 480:1000].copy()
        o_rgba = trim_alpha(clean_cutout(o_crop, bg_thresh=245, low_cut=0.06, high_cut=0.20))
        Image.fromarray(o_rgba).save(os.path.join(out_dir, "space_onion_saturn.png"), "PNG")
        print("Saved creatures (Chili, Onion)")

if __name__ == "__main__":
    main()

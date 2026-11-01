"""Текстуры оверлея листвы: 32×32, 2×2 тайла базовой текстуры 16×16.

Крест листвы — 26×26 пикселей, и в модели берётся UV 0..13 из этой текстуры: пиксель
оверлея совпадает с пикселем блока (без растяжения). Края — рваные листья базовой
текстуры, а не ровные квадраты. Цвет чуть темнее базового, чтобы крест читался как
отдельный слой.

Запуск после gen_leaves.py и gen_season_textures.py:
    python scripts/gen_leaf_overlay_textures.py
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", "trailcraft", "textures", "block")
SEASONS = ("autumn", "winter", "warm_winter")
DARK = 0.82


def make_overlay(path):
    img = Image.open(path).convert("RGBA")
    out = Image.new("RGBA", (32, 32))
    for dx in (0, 16):
        for dy in (0, 16):
            out.alpha_composite(img, (dx, dy))
    px = out.load()
    for y in range(32):
        for x in range(32):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (round(r * DARK), round(g * DARK), round(b * DARK), a)
    return out


def write(folder, name):
    path = os.path.join(folder, name)
    if not os.path.exists(path):
        return 0
    out = make_overlay(path)
    out.save(os.path.join(folder, name[:-4] + "_overlay.png"))
    return 1


count = 0
for name in sorted(os.listdir(TEX)):
    if name.endswith("_leaves.png") and not name.endswith("_overlay.png"):
        count += write(TEX, name)
for season in SEASONS:
    folder = os.path.join(TEX, "season", season)
    if not os.path.isdir(folder):
        continue
    for name in sorted(os.listdir(folder)):
        if name.endswith("_leaves.png") and not name.endswith("_overlay.png"):
            count += write(folder, name)
print(f"ok: {count} overlays")

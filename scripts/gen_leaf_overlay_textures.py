"""Текстуры оверлея листвы: 32×32, базовая 16×16 тайлится в области 26×26.

Крест листвы — 26×26 пикселей, и в модели берётся UV 0..13 из этой текстуры: пиксель
оверлея совпадает с пикселем блока (без растяжения).

Чтобы края не были ровным квадратом, в области 26×26 остаётся только неровное «облако»
листьев: углы и края съедает маска на основе сглаженного шума. За счёт этого силуэт
креста рваный, как у живой кроны, а не прямоугольный.

Запуск после gen_leaves.py и gen_season_textures.py:
    python scripts/gen_leaf_overlay_textures.py
"""
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", "trailcraft", "textures", "block")
SEASONS = ("autumn", "winter", "warm_winter")
DARK = 0.82
SIZE = 26          # сколько пикселей текстуры реально используется (UV 0..13)


def make_overlay(path, seed):
    base = Image.open(path).convert("RGBA")
    rng = random.Random(seed)
    noise = Image.new("L", (8, 8))
    noise.putdata([rng.randrange(256) for _ in range(64)])
    noise = noise.resize((SIZE, SIZE), Image.BILINEAR)
    out = Image.new("RGBA", (32, 32))
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = base.getpixel((x % 16, y % 16))
            if not a:
                continue
            d = math.hypot((x - (SIZE - 1) / 2) / (SIZE / 2), (y - (SIZE - 1) / 2) / (SIZE / 2))
            threshold = 0.55 + 0.40 * (noise.getpixel((x, y)) / 255)
            if d > threshold or rng.random() < 0.04:
                continue
            out.putpixel((x, y), (round(r * DARK), round(g * DARK), round(b * DARK), a))
    return out


def write(folder, name):
    path = os.path.join(folder, name)
    if not os.path.exists(path):
        return 0
    seed = sum(ord(c) for c in name)
    make_overlay(path, seed).save(os.path.join(folder, name[:-4] + "_overlay.png"))
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

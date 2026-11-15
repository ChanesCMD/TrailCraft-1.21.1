"""Крупные прямоугольные облака вместо мелких ванильных клочков."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")

rng = random.Random(20261115)
img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Крупные ступенчатые массивы: несколько прямоугольников в одном пятне.
clusters = [
    (18, 22, 62, 24), (32, 40, 38, 18), (8, 46, 44, 14),
    (88, 18, 52, 34), (104, 38, 66, 18), (78, 46, 30, 12),
    (168, 24, 64, 20), (182, 42, 40, 26), (150, 52, 34, 12),
    (26, 92, 70, 26), (46, 112, 42, 16), (12, 118, 26, 14),
    (112, 88, 54, 22), (132, 104, 70, 28), (156, 126, 34, 14),
    (196, 96, 42, 18), (210, 112, 30, 22),
    (20, 166, 58, 28), (38, 188, 42, 22), (6, 184, 20, 12),
    (94, 158, 66, 22), (112, 174, 38, 28),
    (174, 162, 62, 26), (194, 182, 34, 20),
    (30, 218, 46, 18), (128, 214, 72, 22), (212, 206, 32, 24),
]

for x, y, w, h in clusters:
    draw.rectangle((x, y, x + w, y + h), fill=(255, 255, 255, 255))
    # ступеньки по краям
    if w > 34:
        draw.rectangle((x + 8, y - 6, x + w - 10, y), fill=(255, 255, 255, 255))
        draw.rectangle((x + 12, y + h, x + w - 16, y + h + 5), fill=(255, 255, 255, 255))

# Небольшие прорехи, чтобы форма не была идеальным прямоугольником.
for _ in range(18):
    x, y, w, h = rng.choice(clusters)
    hx = rng.randint(x + 4, max(x + 4, x + w - 8))
    hy = rng.randint(y + 3, max(y + 3, y + h - 5))
    draw.rectangle((hx, hy, hx + rng.randint(2, 6), hy + rng.randint(2, 5)), fill=(0, 0, 0, 0))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

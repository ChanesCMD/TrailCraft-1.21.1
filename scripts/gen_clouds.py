"""Плотные средние прямоугольные облака."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")
rng = random.Random(20261115)

img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Сетка 16×16 пикселей: облака среднего размера, но распределены плотно.
CELL = 16
for cy in range(0, 256, CELL):
    for cx in range(0, 256, CELL):
        if rng.random() > 0.74:
            continue
        w = rng.randint(5, 14)
        h = rng.randint(4, 9)
        x = cx + rng.randint(0, CELL - w)
        y = cy + rng.randint(0, CELL - h)
        draw.rectangle((x, y, x + w, y + h), fill=(255, 255, 255, 255))
        if w > 8:
            draw.rectangle((x + 2, y - 2, x + w - 2, y), fill=(255, 255, 255, 255))
        if h > 6:
            draw.rectangle((x - 2, y + 2, x, y + h - 1), fill=(255, 255, 255, 255))
        if rng.random() < 0.30:
            draw.rectangle((x + 2, y + 2, x + max(3, w // 3), y + max(3, h // 2)), fill=(0, 0, 0, 0))

# Редкие небольшие отдельные клочки между ячейками.
for _ in range(90):
    x, y = rng.randint(0, 248), rng.randint(0, 248)
    w, h = rng.randint(2, 6), rng.randint(2, 5)
    draw.rectangle((x, y, x + w, y + h), fill=(255, 255, 255, 255))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

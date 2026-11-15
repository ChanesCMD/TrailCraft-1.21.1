"""Плотные средние облачные массивы без дырок."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")
rng = random.Random(20261115)

img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
WHITE = (255, 255, 255, 255)
CELL = 16

for cy in range(0, 256, CELL):
    for cx in range(0, 256, CELL):
        if rng.random() > 0.86:
            continue
        w = rng.randint(9, 15)
        h = rng.randint(5, 10)
        x = cx + rng.randint(0, CELL - w)
        y = cy + rng.randint(0, CELL - h)
        draw.rectangle((x, y, x + w, y + h), fill=WHITE)
        # ступенька, но без дырок внутри
        if rng.random() < 0.5:
            draw.rectangle((x + 2, y - 2, x + w - 2, y), fill=WHITE)
        if rng.random() < 0.35:
            draw.rectangle((x + w, y + 2, x + w + 2, y + h - 2), fill=WHITE)

# Немного более длинных полос поверх сетки.
for _ in range(26):
    x, y = rng.randint(0, 240), rng.randint(0, 245)
    draw.rectangle((x, y, x + rng.randint(16, 34), y + rng.randint(3, 6)), fill=WHITE)

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

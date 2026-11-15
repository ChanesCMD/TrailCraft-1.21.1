"""Диагональные ступенчатые облачные полосы."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")
rng = random.Random(20261115)

img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
WHITE = (255, 255, 255, 255)


def band(x, y, dx, dy, width):
    draw.line([(x - dx * 2, y - dy * 2), (x + dx, y + dy)], fill=WHITE, width=width)
    # короткие ответвления, чтобы silhouette был рваным
    if rng.random() < 0.55:
        draw.line([(x, y), (x + rng.randint(18, 50), y + rng.randint(-18, 18))], fill=WHITE,
                  width=max(2, width // 2))


# Диагонали в обе стороны.
for i in range(12):
    y = -20 + i * 24 + rng.randint(-5, 5)
    band(rng.randint(0, 90), y, rng.randint(160, 260), rng.randint(110, 190), rng.randint(6, 13))
for i in range(8):
    x = -10 + i * 34 + rng.randint(-6, 6)
    band(x, rng.randint(0, 70), rng.randint(90, 170), rng.randint(130, 220), rng.randint(5, 11))

# Немного коротких поперечных клочков.
for _ in range(20):
    x, y = rng.randint(0, 240), rng.randint(0, 240)
    draw.line([(x, y), (x + rng.randint(8, 26), y + rng.randint(-8, 8))], fill=WHITE,
              width=rng.randint(2, 5))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

"""Длинные пересекающиеся полосы облаков без дырок."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")
rng = random.Random(20261115)

img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
W = (255, 255, 255, 255)


def strip(x, y, length, thickness, vertical=False):
    if vertical:
        draw.rectangle((x, y, x + thickness, y + length), fill=W)
        # ступенчатые края
        if length > 24:
            draw.rectangle((x - 2, y + 4, x, y + length - 5), fill=W)
            draw.rectangle((x + thickness, y + 8, x + thickness + 3, y + length - 3), fill=W)
    else:
        draw.rectangle((x, y, x + length, y + thickness), fill=W)
        if length > 30:
            draw.rectangle((x + 4, y - 2, x + length - 6, y), fill=W)
            draw.rectangle((x + 8, y + thickness, x + length - 4, y + thickness + 2), fill=W)


# Длинные горизонтальные полосы.
for i in range(18):
    y = 6 + i * 13 + rng.randint(-3, 3)
    x = rng.randint(-20, 90)
    length = rng.randint(40, 120)
    thickness = rng.randint(5, 11)
    strip(x, y, length, thickness)
    if rng.random() < 0.45:
        strip(x + rng.randint(15, 55), y + rng.randint(-7, 7), rng.randint(20, 55), rng.randint(4, 8))

# Пересекающие вертикальные полосы.
for i in range(10):
    x = 10 + i * 24 + rng.randint(-5, 5)
    y = rng.randint(-10, 90)
    length = rng.randint(35, 95)
    thickness = rng.randint(5, 10)
    strip(x, y, length, thickness, vertical=True)

# Пара коротких соединителей, чтобы полосы читались как один массив.
for _ in range(14):
    x, y = rng.randint(0, 240), rng.randint(0, 240)
    strip(x, y, rng.randint(12, 30), rng.randint(4, 7))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

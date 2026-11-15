"""Плотные слоистые пласты: больше полос в группе, меньше вертикальных зазоров."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")
rng = random.Random(20261115)

img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
WHITE = (255, 255, 255, 255)


def bar(x, y, length, height):
    draw.rectangle((x, y, x + length, y + height), fill=WHITE)
    if rng.random() < 0.5:
        draw.rectangle((x + rng.randint(3, 12), y + height, x + length - rng.randint(2, 14), y + height + 1),
                       fill=WHITE)


for gy in range(0, 256, 34):
    for gx in range(0, 256, 36):
        if rng.random() > 0.48:
            continue
        layers = rng.randint(3, 6)
        base_x = gx + rng.randint(0, 12)
        base_y = gy + rng.randint(0, 12)
        for layer in range(layers):
            length = rng.randint(28, 92)
            x = base_x + rng.randint(-8, 8) + layer * rng.randint(-3, 3)
            y = base_y + layer * rng.randint(4, 6)
            if x + length > 256:
                x = 256 - length
            if x < 0:
                x = 0
            bar(x, y, length, rng.randint(2, 4))

# Длинные одиночные пласты.
for _ in range(16):
    x, y = rng.randint(0, 170), rng.randint(0, 250)
    bar(x, y, rng.randint(45, 90), rng.randint(2, 4))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

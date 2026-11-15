"""Небольшие облачные массивы неправильной ступенчатой формы."""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")
rng = random.Random(20261115)

img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)
WHITE = (255, 255, 255, 255)


def puff(x, y, w, h):
    draw.rectangle((x, y, x + w, y + h), fill=WHITE)


def walk(x, y, steps):
    """Ступенчатая облачная клякса: направление и выступы меняются случайно."""
    for _ in range(steps):
        w = rng.randint(3, 9)
        h = rng.randint(2, 6)
        puff(x, y, w, h)
        # случайный выступ вбок
        if rng.random() < 0.35:
            if rng.random() < 0.5:
                puff(x + w, y + rng.randint(-2, h), rng.randint(2, 6), rng.randint(2, 4))
            else:
                puff(x - rng.randint(2, 6), y + rng.randint(-2, h), rng.randint(2, 5), rng.randint(2, 4))
        # шаг в одном из восьми направлений, но с разной длиной
        dir = rng.choice([(1, 0), (0, 1), (1, 1), (1, -1), (2, 1), (1, 2), (-1, 1), (2, -1)])
        x += dir[0] * rng.randint(2, 6)
        y += dir[1] * rng.randint(2, 6)
        x = max(-10, min(260, x))
        y = max(-10, min(260, y))


# Несколько десятков небольших рваных массивов.
for _ in range(92):
    walk(rng.randint(-10, 250), rng.randint(-10, 250), rng.randint(7, 13))

# Короткие отдельные клочки между ними.
for _ in range(55):
    puff(rng.randint(0, 250), rng.randint(0, 250), rng.randint(3, 7), rng.randint(2, 5))

os.makedirs(os.path.dirname(OUT), exist_ok=True)
img.save(OUT)
print("ok")

"""Текстуры частиц погоды (client/weather/): капли дождя, снежинки, брызги, круги на воде, пыль.

Все текстуры белые/серые с прозрачностью: цвет частице задаёт код (дождь — по цвету воды биома, пыль — по цвету
земли под ней). Пишет assets/trailcraft/textures/particle/*.png, assets/trailcraft/particles/*.json и превью
docs/textures/weather_particles.png. Нужен Pillow: python scripts/gen_weather_textures.py
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")
TEX = os.path.join(ASSETS, "textures", "particle")
PARTICLES = os.path.join(ASSETS, "particles")


def img(w, h):
    return Image.new("RGBA", (w, h), (0, 0, 0, 0))


def put(im, x, y, a, v=255):
    """Пиксель с непрозрачностью a (0..1), берётся наибольшая из уже стоящей и новой."""
    if 0 <= x < im.width and 0 <= y < im.height:
        old = im.getpixel((x, y))[3]
        a = max(old, int(round(255 * max(0.0, min(1.0, a)))))
        im.putpixel((x, y), (v, v, v, a))


def rain_drop(variant):
    """Полоса 3×32: сверху (хвост) прозрачная, снизу (голова капли) плотная и светлая."""
    im = img(3, 32)
    length = [32, 26, 20][variant]
    for y in range(32 - length, 32):
        t = (y - (32 - length)) / (length - 1)
        a = 0.15 + 0.85 * t ** 1.6
        put(im, 1, y, a, 255)
        put(im, 0, y, a * 0.35, 220)
        put(im, 2, y, a * 0.35, 220)
    return im


def snow_flake(variant):
    """Снежинки 8×8: комочек, крестик, шестилучевая звёздочка, пушинка."""
    im = img(8, 8)
    if variant == 0:
        for x, y, a in [(3, 3, 1), (4, 3, 1), (3, 4, 1), (4, 4, 1), (2, 3, .5), (5, 4, .5), (3, 2, .5), (4, 5, .5)]:
            put(im, x, y, a)
    elif variant == 1:
        for i in range(1, 7):
            put(im, i, 3, 0.9 if 2 <= i <= 5 else 0.5)
            put(im, 3, i, 0.9 if 2 <= i <= 5 else 0.5)
        put(im, 3, 3, 1)
    elif variant == 2:
        cx = cy = 3.5
        for k in range(6):
            ang = k * math.pi / 3
            for r in (1, 2, 3):
                put(im, int(round(cx + math.cos(ang) * r)), int(round(cy + math.sin(ang) * r)), 1.1 - r * 0.2)
        put(im, 3, 3, 1)
        put(im, 4, 4, 1)
    else:
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5)
                put(im, x, y, max(0.0, 1 - d / 3.2) ** 1.5)
    return im


def rain_splash(frame):
    """Брызги 8×8 по кадрам: капля ударяет, разлетается «коронкой» и тает."""
    im = img(8, 8)
    fade = [1.0, 0.9, 0.7, 0.4][frame]
    if frame == 0:
        for x, y in [(3, 6), (4, 6), (3, 5), (4, 5), (2, 7), (5, 7)]:
            put(im, x, y, fade)
    else:
        spread = frame + 1
        height = [0, 3, 4, 3][frame]
        for side in (-1, 1):
            for i in range(height):
                x = int(round(3.5 + side * (0.5 + spread * (i + 1) / height)))
                put(im, x, 7 - i, fade * (1 - i / (height + 1)))
        for x in range(3 - spread // 2, 5 + spread // 2):
            put(im, x, 7, fade * 0.6)
        if frame >= 2:
            put(im, 3 - spread, 7 - height, fade * 0.7)
            put(im, 4 + spread, 7 - height, fade * 0.7)
    return im


def rain_ripple(frame):
    """Круг на воде 16×16: расходится от центра и бледнеет (вид сверху)."""
    im = img(16, 16)
    radius = [1.5, 3.5, 5.5, 7.2][frame]
    width = [1.2, 1.0, 0.9, 0.8][frame]
    fade = [1.0, 0.85, 0.6, 0.35][frame]
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            a = max(0.0, 1 - abs(d - radius) / width) * fade
            if frame >= 2:
                a = max(a, max(0.0, 1 - abs(d - radius * 0.5) / 0.8) * fade * 0.4)
            put(im, x, y, a)
    return im


def dust_mote(variant):
    """Пыль 8×8: мягкое пятно, пара песчинок, клочок."""
    im = img(8, 8)
    rnd = random.Random(variant * 31 + 7)
    if variant == 0:
        for y in range(8):
            for x in range(8):
                d = math.hypot(x - 3.5, y - 3.5)
                put(im, x, y, max(0.0, 1 - d / 3.6) * 0.8, 235)
    elif variant == 1:
        for x, y in [(2, 3), (5, 4), (4, 1), (3, 6)]:
            put(im, x, y, 1, 255)
            put(im, x + 1, y, 0.4, 230)
    else:
        for _ in range(14):
            x, y = int(rnd.gauss(3.5, 1.4)), int(rnd.gauss(3.5, 0.9))
            put(im, x, y, rnd.uniform(0.4, 0.9), 240)
    return im


# имя частицы -> (функция, число кадров/вариантов)
SPRITES = {
    "rain_drop": (rain_drop, 3),
    "snow_flake": (snow_flake, 4),
    "rain_splash": (rain_splash, 4),
    "rain_ripple": (rain_ripple, 4),
    "dust_mote": (dust_mote, 3),
}

# как подкрашивать в превью (в игре цвет задаёт код)
PREVIEW_TINT = {
    "rain_drop": (150, 185, 235),
    "snow_flake": (255, 255, 255),
    "rain_splash": (170, 200, 240),
    "rain_ripple": (190, 215, 245),
    "dust_mote": (214, 182, 120),
}


def tint(im, rgb):
    out = img(im.width, im.height)
    for y in range(im.height):
        for x in range(im.width):
            v, _, _, a = im.getpixel((x, y))
            out.putpixel((x, y), tuple(int(c * v / 255) for c in rgb) + (a,))
    return out


def main():
    os.makedirs(TEX, exist_ok=True)
    os.makedirs(PARTICLES, exist_ok=True)
    preview_rows = []
    for name, (fn, n) in SPRITES.items():
        names = []
        row = []
        for i in range(n):
            im = fn(i)
            im.save(os.path.join(TEX, f"{name}_{i}.png"))
            names.append(f"trailcraft:{name}_{i}")
            row.append(im)
        with open(os.path.join(PARTICLES, f"{name}.json"), "w", encoding="utf-8") as f:
            json.dump({"textures": names}, f, indent=2)
            f.write("\n")
        preview_rows.append((name, row))

    cell = 40
    sheet = Image.new("RGBA", (8 + 4 * (cell * 2 + 8), 8 + len(preview_rows) * (cell * 2 + 8)), (34, 40, 52, 255))
    for r, (name, row) in enumerate(preview_rows):
        for i, im in enumerate(row):
            big = tint(im, PREVIEW_TINT[name])
            k = max(1, min((cell * 2) // im.width, (cell * 2) // im.height))
            big = big.resize((im.width * k, im.height * k), Image.NEAREST)
            x = 8 + i * (cell * 2 + 8) + (cell * 2 - big.width) // 2
            y = 8 + r * (cell * 2 + 8) + (cell * 2 - big.height) // 2
            sheet.alpha_composite(big, (x, y))
    sheet.save(os.path.join(ROOT, "docs", "textures", "weather_particles.png"))


if __name__ == "__main__":
    main()

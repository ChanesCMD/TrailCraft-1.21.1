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


def line(im, x0, y0, x1, y1, a):
    """Отрезок по точкам (шаг полпикселя)."""
    n = max(1, int(math.hypot(x1 - x0, y1 - y0) * 2))
    for i in range(n + 1):
        t = i / n
        put(im, int(round(x0 + (x1 - x0) * t)), int(round(y0 + (y1 - y0) * t)), a)


def snow_flake(variant):
    """Снежинки: 0–3 — 8×8 (комочек, крестик, шестилучевая звёздочка, пушинка); 4–9 — 9×9 с центром в пикселе 4
    (звезда с веточками, шестигранная пластинка, восьмилучевая звезда, иголка, крупа, ромбик)."""
    if variant >= 4:
        return snow_flake_big(variant)
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


def snow_flake_big(variant):
    im = img(9, 9)
    c = 4
    if variant == 4:
        # шестилучевая звезда с веточками на концах лучей
        for k in range(6):
            ang = k * math.pi / 3 + math.pi / 6
            ex, ey = c + math.cos(ang) * 4, c + math.sin(ang) * 4
            line(im, c, c, ex, ey, 0.9)
            mx, my = c + math.cos(ang) * 2.6, c + math.sin(ang) * 2.6
            for side in (-1, 1):
                b = ang + side * math.pi / 3
                line(im, mx, my, mx + math.cos(b) * 1.3, my + math.sin(b) * 1.3, 0.6)
        put(im, c, c, 1)
    elif variant == 5:
        # шестигранная пластинка: плотный край, полупрозрачная середина
        corners = [(c + math.cos(k * math.pi / 3) * 3.6, c + math.sin(k * math.pi / 3) * 3.6) for k in range(6)]
        for y in range(9):
            for x in range(9):
                d = math.hypot(x - c, y - c)
                ang = math.atan2(y - c, x - c) % (math.pi / 3) - math.pi / 6
                r = 3.6 * math.cos(math.pi / 6) / math.cos(ang)
                if d <= r - 0.6:
                    put(im, x, y, 0.35)
        for k in range(6):
            line(im, *corners[k], *corners[(k + 1) % 6], 0.95)
        put(im, c, c, 0.8)
    elif variant == 6:
        # восьмилучевая звезда: длинный крест и короткие диагонали
        line(im, c, 0, c, 8, 0.85)
        line(im, 0, c, 8, c, 0.85)
        line(im, c - 2, c - 2, c + 2, c + 2, 0.7)
        line(im, c - 2, c + 2, c + 2, c - 2, 0.7)
        put(im, c, c, 1)
    elif variant == 7:
        # иголка (столбик): частица крутится, поэтому иголка летит под разными углами
        line(im, c - 3, c + 2, c + 3, c - 2, 0.9)
        for x, y in ((c - 3, c + 2), (c + 3, c - 2)):
            put(im, x, y, 1)
    elif variant == 8:
        # крупа: плотный шарик с мягким краем
        for y in range(9):
            for x in range(9):
                d = math.hypot(x - c, y - c)
                put(im, x, y, 1.0 if d < 1.2 else max(0.0, 1 - (d - 1.2) / 1.6) * 0.7)
    else:
        # ромбик с крестом внутри
        for k in range(4):
            a0, a1 = k * math.pi / 2, (k + 1) * math.pi / 2
            line(im, c + math.cos(a0) * 4, c + math.sin(a0) * 4, c + math.cos(a1) * 4, c + math.sin(a1) * 4, 0.85)
        line(im, c, c - 2, c, c + 2, 0.6)
        line(im, c - 2, c, c + 2, c, 0.6)
        put(im, c, c, 1)
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
    """Пыль и песок 8×8 (оттенок серого — светлее/темнее цвета земли): 0 — мягкое пятно, 1 — пара песчинок,
    2 — клочок, 3 — крупная песчинка с тенью, 4 — россыпь точек, 5 — шлейф по ветру, 6 — комок с крапинами,
    7 — светлая и тёмная песчинки, 8 — бледное облачко, 9 — завиток вихря."""
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
    elif variant == 2:
        for _ in range(14):
            x, y = int(rnd.gauss(3.5, 1.4)), int(rnd.gauss(3.5, 0.9))
            put(im, x, y, rnd.uniform(0.4, 0.9), 240)
    elif variant == 3:
        for x, y, v in [(3, 3, 255), (4, 3, 245), (3, 4, 225), (4, 4, 200)]:
            put(im, x, y, 1, v)
        put(im, 5, 4, 0.5, 170)
        put(im, 4, 5, 0.5, 170)
    elif variant == 4:
        for _ in range(7):
            put(im, rnd.randrange(8), rnd.randrange(8), rnd.uniform(0.6, 1), rnd.choice((200, 230, 255)))
    elif variant == 5:
        for x in range(8):
            put(im, x, 4, (x + 1) / 8, 240)
            put(im, x, 3, (x + 1) / 8 * 0.45, 220)
            if x % 3 == 0:
                put(im, x, 5, (x + 1) / 8 * 0.35, 210)
    elif variant == 6:
        for y in range(8):
            for x in range(8):
                d = math.hypot((x - 3.5) * 1.2, y - 3.8) + rnd.uniform(-0.5, 0.5)
                if d < 2.8:
                    put(im, x, y, 0.85, 175 if rnd.random() < 0.3 else 235)
    elif variant == 7:
        put(im, 2, 2, 1, 255)
        put(im, 3, 2, 0.6, 240)
        put(im, 5, 5, 1, 165)
        put(im, 5, 6, 0.6, 150)
    elif variant == 8:
        for y in range(8):
            for x in range(8):
                d = math.hypot((x - 3.5) / 1.1, (y - 3.5) / 0.8)
                put(im, x, y, max(0.0, 1 - d / 4) * 0.45, 245)
    else:
        for i in range(14):
            t = i / 13
            ang = t * math.pi * 1.6
            r = 0.8 + t * 2.6
            put(im, int(round(3.5 + math.cos(ang) * r)), int(round(3.5 + math.sin(ang) * r)), 0.4 + 0.5 * (1 - t), 235)
    return im


# имя частицы -> (функция, число кадров/вариантов)
SPRITES = {
    "rain_drop": (rain_drop, 3),
    "snow_flake": (snow_flake, 10),
    "rain_splash": (rain_splash, 4),
    "rain_ripple": (rain_ripple, 4),
    "dust_mote": (dust_mote, 10),
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

    # превью: по 5 картинок в строке, длинные наборы (снежинки) переносятся на следующую строку
    cell, cols = 40, 5
    lines = [(name, row[i:i + cols]) for name, row in preview_rows for i in range(0, len(row), cols)]
    step = cell * 2 + 8
    sheet = Image.new("RGBA", (8 + cols * step, 8 + len(lines) * step), (34, 40, 52, 255))
    for r, (name, row) in enumerate(lines):
        for i, im in enumerate(row):
            big = tint(im, PREVIEW_TINT[name])
            k = max(1, min((cell * 2) // im.width, (cell * 2) // im.height))
            big = big.resize((im.width * k, im.height * k), Image.NEAREST)
            x = 8 + i * step + (cell * 2 - big.width) // 2
            y = 8 + r * step + (cell * 2 - big.height) // 2
            sheet.alpha_composite(big, (x, y))
    sheet.save(os.path.join(ROOT, "docs", "textures", "weather_particles.png"))


if __name__ == "__main__":
    main()

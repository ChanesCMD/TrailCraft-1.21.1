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


def _opaque(out):
    return {(x, y) for y in range(SIZE) for x in range(SIZE) if out.getpixel((x, y))[3]}


def _components(cells):
    left = set(cells)
    result = []
    while left:
        start = left.pop()
        comp = {start}
        stack = [start]
        while stack:
            x, y = stack.pop()
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                p = (x + dx, y + dy)
                if p in left:
                    left.remove(p)
                    comp.add(p)
                    stack.append(p)
        result.append(comp)
    return result


def _line(a, b):
    x0, y0 = a
    x1, y1 = b
    dx, dy = abs(x1 - x0), abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx - dy
    while True:
        yield x0, y0
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 > -dy:
            err -= dy
            x0 += sx
        if e2 < dx:
            err += dx
            y0 += sy


def _prune(out):
    """Срезает одиночные пиксели и кончики цепочек, которые в 3D выглядят летающими."""
    while True:
        cells = _opaque(out)
        drop = [p for p in cells if sum((p[0] + dx, p[1] + dy) in cells
                                        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) <= 1]
        if not drop:
            return out
        for x, y in drop:
            out.putpixel((x, y), (0, 0, 0, 0))


def _cleanup(out):
    """Убирает далеко оторванные мелкие пятна, а близкие к основной куче — пришивает к ней."""
    cells = _opaque(out)
    comps = _components(cells)
    if not comps:
        return out
    # Главная куча одна; всё остальное — либо пришиваем к ней коротким мостиком, либо убираем.
    main = max(comps, key=len)
    large_cells = set(main)
    for comp in comps:
        if comp is main:
            continue
        best = None
        for p in comp:
            for q in large_cells:
                d = max(abs(p[0] - q[0]), abs(p[1] - q[1]))
                if best is None or d < best[0]:
                    best = (d, p, q)
        if best is not None and best[0] <= 2:
            color = out.getpixel(best[1])
            for x, y in _line(best[1], best[2]):
                if out.getpixel((x, y))[3] == 0:
                    out.putpixel((x, y), (color[0], color[1], color[2], 255))
        else:
            for x, y in comp:
                out.putpixel((x, y), (0, 0, 0, 0))
    return out


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
            # Внутри — сплошная листва. Кромка в 0–2 пикселя остаётся рваной, а полоса
            # в 3–5 пикселей от края плотнее — так крест не выглядит обглоданным.
            edge = min(x, y, SIZE - 1 - x, SIZE - 1 - y)
            corner = min(x, SIZE - 1 - x) < 3 and min(y, SIZE - 1 - y) < 3
            if edge >= 5:
                keep = True
            elif edge >= 3:
                keep = rng.random() < (0.80 if corner else 0.88) + 0.10 * (noise.getpixel((x, y)) / 255)
            elif corner:
                keep = rng.random() < 0.12
            else:
                keep = rng.random() < 0.35 + 0.25 * (noise.getpixel((x, y)) / 255)
            if not keep or rng.random() < 0.03:
                continue
            out.putpixel((x, y), (round(r * DARK), round(g * DARK), round(b * DARK), a))
    return _cleanup(_prune(out))


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

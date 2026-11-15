"""Текстура облаков: длинные кучевые гряды со ступенчатой кромкой.

Заменяет ванильную `assets/minecraft/textures/environment/clouds.png` (256×256, в игре
1 тексель = 12 блоков, см. LevelRenderer.buildClouds). Форма собирается из строк: у каждой
строки свой сдвиг и свой шаг «ступеньки» (1–3 текселя), поэтому край не квадратный, а рваный.
Плотность подбирается под заданную, промежутки между грядами остаются.

Вариант выбран автором из подбора (длинные гряды из двух-трёх массивов впритык).
"""
import math
import os
import random

import numpy as np
from PIL import Image

SIZE = 256
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")

# Параметры выбранного варианта: длинные, не квадратные, с нормальными промежутками.
PARAMS = dict(
    name="29. Гряды",
    count=8,
    spacing=64,
    target=0.31,
    banks=(2, 3),
    w=(20, 34),
    h=(5, 8),
    step=3,
    profile="flat",
    holes=0,
    drift=2,
    crumbs=0,
    tails=0,
)
SEED = 1308


def rect(a, x, y, w, h):
    xs = np.arange(int(x), int(x) + int(w)) % SIZE
    ys = np.arange(int(y), int(y) + int(h)) % SIZE
    a[np.ix_(ys, xs)] = True


def clear(a, x, y, w, h):
    xs = np.arange(int(x), int(x) + int(w)) % SIZE
    ys = np.arange(int(y), int(y) + int(h)) % SIZE
    a[np.ix_(ys, xs)] = False


def snap(v, step):
    return int(round(v / step)) * step


def far_points(rng, count, min_dist):
    pts, tries = [], 0
    while len(pts) < count and tries < 6000:
        tries += 1
        p = (rng.uniform(0, SIZE), rng.uniform(0, SIZE))
        if all(math.hypot(min(abs(p[0] - q[0]), SIZE - abs(p[0] - q[0])),
                          min(abs(p[1] - q[1]), SIZE - abs(p[1] - q[1]))) >= min_dist for q in pts):
            pts.append(p)
    return pts


def bank(a, rng, cx, cy, w, h, step=2, taper=0.55, holes=0, drift_max=1, profile="flat"):
    """Один массив: силуэт из строк, края щёлкают по сетке своего шага (1–3)."""
    drift = 0
    hold = 0
    for row in range(h):
        t = (row + 0.5) / h
        if profile == "flat":                  # кучевое: широкий низ, сужается кверху
            prof = (1.0 - t) ** 0.45 + rng.uniform(-0.06, 0.06)
            if rng.random() < 0.28:
                prof += rng.uniform(0.08, 0.3)  # наплыв-лопасть
        else:
            prof = max(0.06, math.sin(math.pi * t) ** taper)
        half = w * 0.5 * prof
        st = max(1, step + rng.choice((-1, 0, 0, 1)))
        if hold > 0:
            hold -= 1
        else:
            drift += rng.randint(-drift_max, drift_max)
            if rng.random() < 0.22:            # площадка: 1–3 строки одной ширины
                hold = rng.randint(1, 3)
        x0 = snap(cx - half + drift + rng.uniform(-1, 1), st)
        x1 = snap(cx + half + drift + rng.uniform(-1, 1), st)
        if x1 - x0 >= max(2, step):
            rect(a, x0, cy + row, x1 - x0, 1)
    for _ in range(holes):
        clear(a, cx + rng.randint(-w // 3, w // 3), cy + rng.randint(1, max(1, h - 2)),
              rng.choice((1, 2, 2, 3)), 1)


def mass(a, rng, cx, cy, c):
    """Скопление из 2–3 слипшихся банков плюс соседняя гряда впритык."""
    n = rng.randint(*c["banks"])
    scale = rng.choice((0.65, 0.8, 1.0, 1.0, 1.15, 1.45))
    w0 = max(4, int(rng.randint(*c["w"]) * scale))
    h0 = max(2, int(rng.randint(*c["h"]) * (0.7 + 0.3 * scale)))
    for i in range(n):
        w = int(w0 * rng.uniform(0.6, 1.1))
        h = int(h0 * rng.uniform(0.6, 1.0))
        bank(a, rng, cx + rng.randint(-w0 // 2, w0 // 2), cy + rng.randint(-2, 2) + i * (h0 // 3),
             w, h, step=c["step"], taper=c.get("taper", 0.55), holes=c.get("holes", 0),
             drift_max=c.get("drift", 1), profile=c.get("profile", "flat"))
    if rng.random() < 0.35:                    # соседний массив впритык — получается гряда
        w = int(w0 * rng.uniform(0.5, 0.9))
        h = int(h0 * rng.uniform(0.5, 0.9))
        bank(a, rng, cx + rng.choice((-1, 1)) * rng.randint(w0 // 3, w0), cy + rng.randint(-3, 3),
             w, h, step=c["step"], taper=c.get("taper", 0.55), holes=0, drift_max=c.get("drift", 1),
             profile=c.get("profile", "flat"))


def build(c, seed, count, spacing):
    rng = random.Random(seed)
    a = np.zeros((SIZE, SIZE), dtype=bool)
    for cx, cy in far_points(rng, count, spacing):
        mass(a, rng, int(cx), int(cy), c)
    return a


def fit(c, seed, target):
    """Подгоняет число гряд и шаг между ними под нужную плотность неба."""
    n, sp = c["count"], float(c["spacing"])
    a = build(c, seed, n, sp)
    for _ in range(4):
        cov = a.mean()
        if cov <= 0:
            n += 1
            a = build(c, seed, n, sp)
            continue
        if abs(cov - target) < 0.012:
            break
        ratio = target / cov
        n = int(min(500, max(2, round(n * ratio))))
        sp = max(8.0, sp / math.sqrt(ratio))
        a = build(c, seed, n, sp)
    return a


def main():
    mask = fit(PARAMS, SEED, PARAMS["target"])
    rgba = np.dstack([np.full((SIZE, SIZE), 255, np.uint8)] * 3 +
                     [(mask * 255).astype(np.uint8)])
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    Image.fromarray(rgba, "RGBA").save(OUT)
    print("ok, плотность %.2f" % mask.mean())


if __name__ == "__main__":
    main()

"""Текстуры дёрна: верх (turf_top) и боковая трава (turf_side).

Берётся ванильный рисунок травы и слегка искажается (плавный сдвиг по блоку) с добавлением шума —
рисунок свой, но характер ванильный. Обе текстуры серые: в игре их красит цвет травы биома.

Запуск из корня проекта:  python scripts/gen_turf_textures.py   (нужен Pillow)
После него — python scripts/gen_rotations.py (повёрнутые варианты верха).
"""
import io
import os
import random
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_create_compat import find_jar  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures", "block")

SEED = 101
WARP = 0.7          # сила искажения рисунка, пикселей
CELL = 4            # сетка искажения: меньше — мельче завихрения
JITTER = 5          # шум на пиксель
SPECKS = 6          # редкие яркие/тёмные крапинки (только у верха)


def warp_field(rnd, amp, cell):
    grid = [[(rnd.uniform(-amp, amp), rnd.uniform(-amp, amp)) for _ in range(cell + 1)] for _ in range(cell + 1)]
    out = [[(0, 0)] * 16 for _ in range(16)]
    for y in range(16):
        for x in range(16):
            gx, gy = x * cell / 16, y * cell / 16
            x0, y0 = int(gx), int(gy)
            fx, fy = gx - x0, gy - y0
            dx = (grid[y0][x0][0] * (1 - fx) * (1 - fy) + grid[y0][x0 + 1][0] * fx * (1 - fy)
                  + grid[y0 + 1][x0][0] * (1 - fx) * fy + grid[y0 + 1][x0 + 1][0] * fx * fy)
            dy = (grid[y0][x0][1] * (1 - fx) * (1 - fy) + grid[y0][x0 + 1][1] * fx * (1 - fy)
                  + grid[y0 + 1][x0][1] * (1 - fx) * fy + grid[y0 + 1][x0 + 1][1] * fx * fy)
            out[y][x] = (dx, dy)
    return out


def vanilla(name):
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    return Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")


def main():
    # верх: серый рисунок травы, зерно 101
    base = vanilla("grass_block_top")
    src = [[base.getpixel((x, y))[0] for x in range(16)] for y in range(16)]
    field = warp_field(random.Random(SEED), WARP, CELL)
    rnd = random.Random(SEED)
    top = Image.new("RGBA", (16, 16))
    px = top.load()
    for y in range(16):
        for x in range(16):
            dx, dy = field[y][x]
            v = src[(y + round(dy)) % 16][(x + round(dx)) % 16]
            v = max(100, min(205, round(v + rnd.uniform(-JITTER, JITTER))))
            px[x, y] = (v, v, v, 255)
    for _ in range(SPECKS):
        x, y = rnd.randrange(16), rnd.randrange(16)
        v = 195 if rnd.random() < 0.5 else 112
        px[x, y] = (v, v, v, 255)
    top.save(os.path.join(TEX, "turf_top.png"))

    # боковая трава: ванильный оверлей, искажённый тем же способом (прозрачность сохраняем)
    overlay = vanilla("grass_block_side_overlay")
    field = warp_field(random.Random(SEED + 1), WARP, CELL)
    rnd = random.Random(SEED + 1)
    side = Image.new("RGBA", (16, 16))
    px = side.load()
    for y in range(16):
        for x in range(16):
            dx, dy = field[y][x]
            r, g, b, a = overlay.getpixel(((x + round(dx)) % 16, (y + round(dy)) % 16))
            if a == 0:
                px[x, y] = (0, 0, 0, 0)
                continue
            v = max(100, min(205, round(r + rnd.uniform(-JITTER, JITTER))))
            px[x, y] = (v, v, v, a)
    side.save(os.path.join(TEX, "turf_side.png"))
    print("ok")


if __name__ == "__main__":
    main()

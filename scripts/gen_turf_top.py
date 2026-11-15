"""Верх дёрна (turf_top): ванильный рисунок травы с искажением и шумом (вариант 09), красится цветом травы."""
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
SPECKS = 6          # редкие яркие/тёмные крапинки
MIX = 0.0          # доля процедурного шума в яркости


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


def main():
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    vanilla = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/grass_block_top.png"))).convert("RGBA")
    base = [[vanilla.getpixel((x, y))[0] for x in range(16)] for y in range(16)]
    field = warp_field(random.Random(SEED), WARP, CELL)
    rnd = random.Random(SEED)
    img = Image.new("RGBA", (16, 16))
    px = img.load()
    for y in range(16):
        for x in range(16):
            dx, dy = field[y][x]
            v = base[(y + round(dy)) % 16][(x + round(dx)) % 16]
            if MIX:
                v = v * (1 - MIX) + (112 + rnd.random() * 83) * MIX
            v = max(100, min(205, round(v + rnd.uniform(-JITTER, JITTER))))
            px[x, y] = (v, v, v, 255)
    for _ in range(SPECKS):
        x, y = rnd.randrange(16), rnd.randrange(16)
        v = 195 if rnd.random() < 0.5 else 112
        px[x, y] = (v, v, v, 255)
    img.save(os.path.join(TEX, "turf_top.png"))
    print("ok")


if __name__ == "__main__":
    main()




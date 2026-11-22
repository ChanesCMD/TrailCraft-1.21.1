"""Маленькая луна: 32x16, 4x2 фазы по 8x8, круглая, с прозрачным фоном (как на дневном фото)."""
import math
import os

from PIL import Image, ImageDraw

OUT = "src/main/resources/assets/trailcraft/textures/environment/moon_phases_06.png"
CELL = 16
BASE = (232, 237, 246)
CRATER = (203, 211, 226)
CRATER2 = (214, 221, 234)
# порядок как у ванили: полная, убывающая выпуклая, последняя четверть, убывающий серп,
# новолуние, растущий серп, первая четверть, растущая выпуклая
PHASES = [0.5, 0.625, 0.75, 0.875, 0.0, 0.125, 0.25, 0.375]
SIDES = [1, -1, -1, -1, 1, 1, 1, 1]


def main():
    sheet = Image.new("RGBA", (CELL * 4, CELL * 2), (0, 0, 0, 0))
    for i, (p, side) in enumerate(zip(PHASES, SIDES)):
        img = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
        d = ImageDraw.Draw(img)
        cx = cy = CELL / 2 - 0.5
        r = CELL / 2 - 0.6
        k = math.cos(math.tau * p)
        for y in range(CELL):
            for x in range(CELL):
                dx = (x - cx) / r
                dy = (y - cy) / r
                if dx * dx + dy * dy > 1.0:
                    continue
                if dx * side - k <= -0.25:
                    continue      # неосвещённая часть
                # кратеры: пара пятен на диск
                tone = BASE
                if (x, y) in ((5, 6), (6, 7), (10, 5), (9, 10), (10, 10), (6, 9)):
                    tone = CRATER
                elif (x, y) in ((4, 8), (11, 7), (7, 12), (12, 9)):
                    tone = CRATER2
                img.putpixel((x, y), tone + (255,))
        # мягкая кромка
        for y in range(CELL):
            for x in range(CELL):
                dx = (x - cx) / r
                dy = (y - cy) / r
                dd = dx * dx + dy * dy
                if 0.82 < dd <= 1.0:
                    r0, g0, b0, a0 = img.getpixel((x, y))
                    if a0:
                        img.putpixel((x, y), (r0, g0, b0, round(a0 * 0.75)))
        sheet.alpha_composite(img, ((i % 4) * CELL, (i // 4) * CELL))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    print("ok", sheet.size)


if __name__ == "__main__":
    main()

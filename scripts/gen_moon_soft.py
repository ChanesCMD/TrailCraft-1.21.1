"""Луна с мягкими краями: 32x16, 4x2 фазы по 8x8, у серпов два слоя полупрозрачных пикселей,
у новолуния — полупрозрачный светлый ободок."""
import math
import os

from PIL import Image

OUT = "src/main/resources/assets/trailcraft/textures/environment/moon_phases_06.png"
CELL = 8
LIT = (216, 224, 248)
CRATER = (176, 184, 200)
CRATER2 = (144, 152, 168)
# полнолуние, убывающая выпуклая, последняя четверть, убывающий серп,
# новолуние, растущий серп, первая четверть, растущая выпуклая
PHASES = [0.5, 0.625, 0.75, 0.875, 0.0, 0.125, 0.25, 0.375]
SIDES = [1, -1, -1, -1, 1, 1, 1, 1]
R = 3.5


def lit(dx, dy, k, side):
    return dx * side - k > -0.18 and dx * dx + dy * dy <= 1.0


def main():
    sheet = Image.new("RGBA", (CELL * 4, CELL * 2), (0, 0, 0, 0))
    for i, (p, side) in enumerate(zip(PHASES, SIDES)):
        img = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
        px = img.load()
        k = math.cos(math.tau * p)
        cx = cy = CELL / 2 - 0.5
        new_moon = p == 0.0
        for y in range(CELL):
            for x in range(CELL):
                dx = (x - cx) / R
                dy = (y - cy) / R
                inside = dx * dx + dy * dy <= 1.0
                if new_moon:
                    # новолуние: только едва заметный ободок
                    if inside and dx * dx + dy * dy > 0.55:
                        px[x, y] = (150, 160, 185, 70)
                    continue
                if not inside:
                    continue
                if lit(dx, dy, k, side):
                    tone = LIT
                    if (x, y) in ((2, 3), (3, 3), (5, 2), (4, 5)):
                        tone = CRATER
                    elif (x, y) in ((2, 4), (6, 3)):
                        tone = CRATER2
                    px[x, y] = tone + (255,)
                elif lit(dx, dy, k - 0.55, side):
                    px[x, y] = LIT + (150,)      # первый слой мягкого края
                elif lit(dx, dy, k - 1.1, side):
                    px[x, y] = LIT + (80,)       # второй, более прозрачный
        sheet.alpha_composite(img, ((i % 4) * CELL, (i // 4) * CELL))
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    sheet.save(OUT)
    print("ok", sheet.size)


if __name__ == "__main__":
    main()

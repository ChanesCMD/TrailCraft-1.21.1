"""Сырная луна (пасхалка): полная сырная луна в реальное полнолуние.

Берёт полную фазу из трафарета scripts/templates/moon_phases_06.png (обычная луна 32x16, 4x2 фазы
по 8x8; полная — левая верхняя клетка), перекрашивает её в сыр и кладёт в
assets/trailcraft/textures/environment/moon_cheese.png. Эту текстуру client/nightsky/SkyBodies рисует
только в реальное полнолуние (client/nightsky/RealMoon), а в остальные дни — обычную луну с фазами.

Запуск из корня проекта:  python scripts/gen_cheese_moon.py   (нужен Pillow)
"""
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
TEMPLATE = os.path.join(ROOT, "scripts", "templates", "moon_phases_06.png")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures", "environment",
                   "moon_cheese.png")
PREVIEW = os.path.join(ROOT, "docs", "textures", "cheese_moon.png")
CELL = 8

# Сыр: тело, две тени дырок и корка.
BODY = (249, 199, 84)
HOLE = (190, 136, 48)
HOLE2 = (158, 106, 32)
RIND = (142, 92, 30)
# Цвета обычной луны в трафарете: тело, кратеры и тёмная кромка.
OLD = {
    (216, 224, 248, 255): BODY,
    (176, 184, 200, 255): HOLE,
    (144, 152, 168, 255): HOLE2,
    (96, 104, 120, 255): RIND,
}
OLD_RIND = (96, 104, 120)
PREVIEW_SCALE = 32


def main():
    src = Image.open(TEMPLATE).convert("RGBA").crop((0, 0, CELL, CELL))
    px = src.load()
    out = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    op = out.load()
    for y in range(CELL):
        for x in range(CELL):
            r, g, b, a = px[x, y]
            if a == 0:
                # прозрачные пиксели по краю кромки тоже перекрашиваем, иначе при фильтрации лезет синяя кайма
                op[x, y] = (RIND + (0,)) if (r, g, b) == OLD_RIND else (r, g, b, 0)
            else:
                op[x, y] = OLD[(r, g, b, a)] + (255,)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    out.save(OUT)
    # превью: полная сырная луна на ночном небе
    big = out.resize((CELL * PREVIEW_SCALE, CELL * PREVIEW_SCALE), Image.NEAREST)
    preview = Image.new("RGBA", big.size, (12, 18, 32, 255))
    preview.alpha_composite(big)
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    preview.convert("RGB").save(PREVIEW)
    print("ok", out.size)


if __name__ == "__main__":
    main()

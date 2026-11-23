"""Луна-сыр (пасхалка): та же форма фаз, что у обычной луны, но жёлтая, с дырками и коркой.

Трафарет — scripts/templates/moon_phases_06.png (обычная луна 32x16, 4x2 фазы по 8x8);
результат — assets/trailcraft/textures/environment/moon_phases_06.png (её рисует client/nightsky/SkyBodies),
превью — docs/textures/cheese_moon.png. Форма фаз не меняется, поэтому ореолы в SkyBodies остаются на месте.

Запуск из корня проекта:  python scripts/gen_cheese_moon.py   (нужен Pillow)
Вернуть обычную луну:     python scripts/gen_cheese_moon.py --revert
"""
import os
import shutil
import sys

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
TEMPLATE = os.path.join(ROOT, "scripts", "templates", "moon_phases_06.png")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures", "environment",
                   "moon_phases_06.png")
PREVIEW = os.path.join(ROOT, "docs", "textures", "cheese_moon.png")

# Сыр: тело, две тени дырок, корка и едва заметный ободок новолуния.
BODY = (249, 199, 84)
HOLE = (190, 136, 48)
HOLE2 = (158, 106, 32)
RIND = (142, 92, 30)
FAINT = (249, 199, 84, 70)
# Цвета обычной луны в трафарете: тело, кратеры и тёмная кромка.
OLD = {
    (216, 224, 248, 255): BODY,
    (176, 184, 200, 255): HOLE,
    (144, 152, 168, 255): HOLE2,
    (96, 104, 120, 255): RIND,
}
OLD_RIND = (96, 104, 120)
PREVIEW_SCALE = 16


def main():
    if "--revert" in sys.argv:
        shutil.copyfile(TEMPLATE, OUT)
        print("вернул обычную луну")
        return
    src = Image.open(TEMPLATE).convert("RGBA")
    px = src.load()
    out = Image.new("RGBA", src.size, (0, 0, 0, 0))
    op = out.load()
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if a == 0:
                # прозрачные пиксели по краю кромки тоже перекрашиваем, иначе при фильтрации лезет синяя кайма
                op[x, y] = (RIND + (0,)) if (r, g, b) == OLD_RIND else (r, g, b, 0)
            elif a == 70:
                op[x, y] = FAINT
            else:
                op[x, y] = OLD[(r, g, b, a)] + (255,)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    out.save(OUT)
    # превью: та же таблица фаз на ночном небе
    big = out.resize((out.width * PREVIEW_SCALE, out.height * PREVIEW_SCALE), Image.NEAREST)
    preview = Image.new("RGBA", big.size, (12, 18, 32, 255))
    preview.alpha_composite(big)
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    preview.convert("RGB").save(PREVIEW)
    print("ok", out.size)


if __name__ == "__main__":
    main()

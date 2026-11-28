"""Арбузная луна: фазы 2x (4x по площади), полная — целый арбуз без увеличения,
поверх кот в исходном разрешении, посаженный выше. В репозиторий идёт выбранный вариант.

Запуск из корня проекта:  python scripts/gen_watermelon_moon.py   (нужен Pillow)
"""
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
SRC = os.path.join(ROOT, "scripts", "templates", "watermelon_moon.png")
CAT = os.path.join(ROOT, "scripts", "templates", "cat_moon.png")
ENV = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures", "environment")
OUT_SHEET = os.path.join(ENV, "moon_phases_watermelon.png")
OUT_FULL = os.path.join(ENV, "moon_watermelon_full.png")
PREVIEW = os.path.join(ROOT, "docs", "textures", "watermelon_moon.png")
CELL = 64          # фазы растянуты вдвое по стороне (вчетверо по площади)
FULL = 64          # холст полной луны: запас сверху, чтобы кот сидел выше
MELON = 32         # сам арбуз полной луны не увеличиваем
BG = (12, 18, 32)


def close(p, q, tol=8):
    return abs(p[0] - q[0]) <= tol and abs(p[1] - q[1]) <= tol and abs(p[2] - q[2]) <= tol


def cut_cells(img, size):
    """Убрать фон, обрезать каждый арбуз и вписать в клетку size x size."""
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if close((r, g, b), BG):
                px[x, y] = (r, g, b, 0)
    cw, ch = w // 4, h // 2
    cells = []
    for row in range(2):
        for col in range(4):
            cell = img.crop((col * cw, row * ch, (col + 1) * cw, (row + 1) * ch))
            bbox = cell.getbbox()
            if bbox is None:
                raise SystemExit("пустая клетка арбузной таблицы")
            melon = cell.crop(bbox)
            side = max(melon.width, melon.height)
            square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
            square.alpha_composite(melon, ((side - melon.width) // 2, (side - melon.height) // 2))
            cells.append(square.resize((size, size), Image.BOX))
    return cells


def cut_cat(cat):
    """Убрать старый арбуз из текстуры кота; глаза не трогаем."""
    px = cat.load()
    for y in range(cat.height):
        for x in range(cat.width):
            r, g, b, a = px[x, y]
            # корка старого арбуза: зелёная и жёлто-оливковая (у кота таких цветов нет)
            if a and g >= r - 2 and g > b + 20:
                px[x, y] = (r, g, b, 0)
    return cat


def main():
    src = Image.open(SRC).convert("RGBA")
    cells = cut_cells(src.copy(), CELL)          # фазы 64x64
    melon = cut_cells(src.copy(), MELON)[0]       # полная — 32x32
    cat = cut_cat(Image.open(CAT).convert("RGBA"))  # 32x32, без старого арбуза

    full = Image.new("RGBA", (FULL, FULL), (0, 0, 0, 0))
    full.alpha_composite(melon, ((FULL - MELON) // 2, (FULL - MELON) // 2))
    full.alpha_composite(cat, ((FULL - cat.width) // 2, 6))

    sheet = Image.new("RGBA", (CELL * 4, CELL * 2), (0, 0, 0, 0))
    for i, cell in enumerate(cells):
        sheet.alpha_composite(cell, ((i % 4) * CELL, (i // 4) * CELL))
    os.makedirs(os.path.dirname(OUT_SHEET), exist_ok=True)
    sheet.save(OUT_SHEET)
    full.save(OUT_FULL)

    # превью: полная луна крупно слева (холст 64 -> 512), фазы 1..7 справа по 256
    pad, gap, panel = 18, 16, 256
    full_panel = full.resize((512, 512), Image.NEAREST)
    width = pad * 2 + full_panel.width + 20 + 4 * panel + 3 * 12
    height = pad * 2 + 30 + 2 * (panel + 26) + 12
    img = Image.new("RGBA", (width, height), (12, 18, 32, 255))
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arialbd.ttf", 18)
    except OSError:
        font = ImageFont.load_default()
    d.text((pad, 10), "полная — кот (32x32, посажен выше)", font=font, fill=(235, 237, 244))
    img.alpha_composite(full_panel, (pad, 30 + pad))
    gx = pad + full_panel.width + 20
    for n in range(1, 8):
        i = n - 1
        col, row = i % 4, i // 4
        x = gx + col * (panel + 12)
        y = 30 + pad + row * (panel + 26) + 22
        cell = sheet.crop((n * CELL % (CELL * 4), (n * CELL // (CELL * 4)) * CELL,
                           n * CELL % (CELL * 4) + CELL, (n * CELL // (CELL * 4)) * CELL + CELL))
        img.alpha_composite(cell.resize((panel, panel), Image.NEAREST), (x, y))
        d.text((x + 2, y - 22), str(n), font=font, fill=(235, 237, 244))
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    img.convert("RGB").save(PREVIEW)
    full_panel.convert("RGB").save(os.path.join(ROOT, "build", "tmp", "watermelon_moon", "full_v9.png"))
    print("ok", sheet.size, full.size, img.size)


if __name__ == "__main__":
    main()

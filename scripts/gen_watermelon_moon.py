"""Арбузная луна: вырезает 8 круглых арбузов из превью-трафарета, собирает таблицу 128x64
и кладёт кота на полную фазу. Обычная и сырная луна не трогаются.

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
CELL = 32
BG = (12, 18, 32)


def close(p, q, tol=8):
    return abs(p[0] - q[0]) <= tol and abs(p[1] - q[1]) <= tol and abs(p[2] - q[2]) <= tol


def cut_cells(img):
    """Убрать фон, обрезать каждый арбуз и вписать в клетку 32x32."""
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
            cells.append(square.resize((CELL, CELL), Image.BOX))
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
    cells = cut_cells(Image.open(SRC).convert("RGBA"))
    cat = cut_cat(Image.open(CAT).convert("RGBA"))
    full = cells[0].copy()
    full.alpha_composite(cat)

    sheet = Image.new("RGBA", (CELL * 4, CELL * 2), (0, 0, 0, 0))
    for i, cell in enumerate(cells):
        sheet.alpha_composite(cell, ((i % 4) * CELL, (i // 4) * CELL))
    os.makedirs(os.path.dirname(OUT_SHEET), exist_ok=True)
    sheet.save(OUT_SHEET)
    full.save(OUT_FULL)

    panel, pad, gap = 256, 18, 14
    labels = ["полная — кот", "1", "2", "3", "4 — новолуние", "5", "6", "7"]
    img = Image.new("RGBA", (pad * 2 + 4 * panel + 3 * gap, pad * 2 + 2 * (panel + 28) + gap), (12, 18, 32, 255))
    d = ImageDraw.Draw(img)
    try:
        font = ImageFont.truetype("arialbd.ttf", 18)
    except OSError:
        font = ImageFont.load_default()
    for i in range(8):
        col, row = i % 4, i // 4
        x = pad + col * (panel + gap)
        y = pad + row * (panel + 28) + 24
        pic = full if i == 0 else cells[i]
        img.alpha_composite(pic.resize((panel, panel), Image.NEAREST), (x, y))
        d.text((x + 2, y - 24), labels[i], font=font, fill=(235, 237, 244))
    os.makedirs(os.path.dirname(PREVIEW), exist_ok=True)
    img.convert("RGB").save(PREVIEW)
    print("ok", sheet.size, full.size)


if __name__ == "__main__":
    main()

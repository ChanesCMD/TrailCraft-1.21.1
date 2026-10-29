"""Листва по сезонам: осенняя (жёлтая) и зимняя (белая) копии текстур листвы лиственных пород.

Пишет textures/block/season/autumn/<листва>.png и textures/block/season/winter/<листва>.png; в игре их подставляет вместо
летней текстуры client/SeasonColors.java (спрайт trailcraft:block/X → trailcraft:block/season/<сезон>/X, если такой
есть). Хвоя (CONIFERS) не меняется; красный клён осенью остаётся красным (осенней копии нет). Нужен Pillow.
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "trailcraft", "textures", "block")
# хвойные — та же листва круглый год (как в ClientEvents.wrapLeafModels)
CONIFERS = ("spruce", "pine", "fir", "cedar", "thuja")
# уже осенние: осенней копии нет
NO_AUTUMN = ("red_maple",)


def recolor(img, fn):
    out = img.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            r2, g2, b2 = colorsys.hsv_to_rgb(*fn(x, y, h, s, v))
            px[x, y] = (round(r2 * 255), round(g2 * 255), round(b2 * 255), a)
    return out


def autumn(x, y, h, s, v):
    # жёлтый с рыжими пятнами: оттенок зависит от пикселя, яркость прежняя, чуть светлее и насыщеннее
    hue = 0.13 - 0.03 * (((x * 7 + y * 13) % 5) == 0)
    return hue, min(1, 0.55 + s * 0.4), min(1, v * 1.15 + 0.08)


def winter(x, y, h, s, v):
    # иней: почти белая, с голубоватыми тенями, светотень текстуры остаётся
    return 0.58, 0.08 + 0.1 * (1 - v), 0.72 + 0.28 * v


def main():
    for name in sorted(os.listdir(ROOT)):
        if not name.endswith("_leaves.png") or name.startswith(CONIFERS):
            continue
        img = Image.open(os.path.join(ROOT, name)).convert("RGBA")
        for season, fn in (("autumn", autumn), ("winter", winter)):
            if season == "autumn" and name.startswith(NO_AUTUMN):
                continue
            folder = os.path.join(ROOT, "season", season)
            os.makedirs(folder, exist_ok=True)
            recolor(img, fn).save(os.path.join(folder, name))
            # анимированные текстуры — со своим .mcmeta
            meta = os.path.join(ROOT, name + ".mcmeta")
            if os.path.exists(meta):
                with open(meta, encoding="utf-8") as f, open(os.path.join(folder, name + ".mcmeta"), "w", encoding="utf-8") as o:
                    o.write(f.read())
        print(name)


if __name__ == "__main__":
    main()

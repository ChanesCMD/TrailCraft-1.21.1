"""Метательные копья (item/ThrowingSpearItem, init/ModTools): <материал>_throwing_spear.

Пишет: текстуру (одна, длинная — копьё в руке с оперением, как у стрелы, у торца древка), модели (в руке — как
у обычного копья, в инвентаре, в рамке и на земле — та же длинная текстура плоской иконкой), рецепты (копьё + 3 пера), переводы. Нужен Pillow.
Запуск: python scripts/gen_throwing_spear.py
"""
import json
import os
import re

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "trailcraft"
MATERIALS = ["flint", "bone", "amethyst", "copper", "bronze", "iron", "steel"]
FEATHER, FEATHER_EDGE = (246, 246, 242, 255), (214, 214, 212, 255)
SHADE, SHADE_EDGE = (186, 186, 192, 255), (150, 150, 158, 255)
OUTLINE = (62, 56, 54, 255)


def path(*parts):
    return os.path.join(ROOT, *parts)


def write(rel, data):
    p = path(*rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(rel):
    with open(path(*rel.split("/")), encoding="utf-8") as f:
        return json.load(f)


def fletch(src, tail_gap=5, length=12, width=6, back=0.5):
    """Оперение, как у стрелы в луке: у торца древка (идёт вверх-вправо, x + y ≈ c) два пера с тёмной обводкой —
    сверху-слева светлое, снизу-справа в тени. Перо у древка во всю длину, к острию сходит на нет, а хвостовой
    край скошен назад. d — сдвиг поперёк древка (x + y − c), s — вдоль древка от места пера (x − y, шаг 2 на пиксель).
    """
    im = src.copy()
    w, h = im.size
    opaque = lambda x, y: 0 <= x < w and 0 <= y < h and im.getpixel((x, y))[3] > 0
    pts = [(x, y) for y in range(h) for x in range(w) if opaque(x, y)]
    c = round(sum(x + y for x, y in pts) / len(pts))
    tail = min(x - y for x, y in pts)

    def inside(x, y):
        a = abs(x + y - c) - 1  # от края древка
        s = (x - y) - tail - 2 * tail_gap
        return 1 <= a <= width and -2 * back * a <= s <= 2 * length - a * 2 * length / width

    new = {}
    for y in range(h):
        for x in range(w):
            if opaque(x, y) or not inside(x, y):
                continue
            a = abs(x + y - c) - 1
            edge = any(not inside(x + dx, y + dy) and not opaque(x + dx, y + dy)
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            light, dim = (FEATHER, FEATHER_EDGE) if x + y < c else (SHADE, SHADE_EDGE)
            new[(x, y)] = OUTLINE if edge else light if a < width - 2 else dim
    for p, color in new.items():
        im.putpixel(p, color)
    return im


def main():
    tex = path("assets", NS, "textures", "item")
    for m in MATERIALS:
        src = Image.open(os.path.join(tex, f"{m}_spear_in_hand.png")).convert("RGBA")
        fletch(src).save(os.path.join(tex, f"{m}_throwing_spear_in_hand.png"))
        short = os.path.join(tex, f"{m}_throwing_spear.png")
        if os.path.exists(short):
            os.remove(short)
        hand = read(f"assets/{NS}/models/item/{m}_spear_in_hand.json")
        hand["textures"] = {"layer0": f"{NS}:item/{m}_throwing_spear_in_hand"}
        write(f"assets/{NS}/models/item/{m}_throwing_spear_in_hand.json", hand)
        icon = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{m}_throwing_spear_in_hand"}}
        write(f"assets/{NS}/models/item/{m}_throwing_spear.json", {
            "loader": "neoforge:separate_transforms", "gui_light": "front",
            "base": {"parent": f"{NS}:item/{m}_throwing_spear_in_hand"},
            "perspectives": {"gui": icon, "fixed": icon, "ground": icon}})
        write(f"data/{NS}/recipe/{m}_throwing_spear.json", {
            "type": "minecraft:crafting_shapeless", "category": "combat",
            "ingredients": [{"item": f"{NS}:{m}_spear"}] + [{"item": "minecraft:feather"}] * 3,
            "result": {"id": f"{NS}:{m}_throwing_spear", "count": 1}})
    for code in ("ru_ru", "en_us"):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for m in MATERIALS:
            name = data[f"item.{NS}.{m}_spear"]
            data[f"item.{NS}.{m}_throwing_spear"] = (re.sub(r"копьё$", "метательное копьё", name) if code == "ru_ru"
                                                     else re.sub(r"Spear$", "Throwing Spear", name))
        write(rel, data)
    print("ok")


if __name__ == "__main__":
    main()

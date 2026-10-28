"""
Дёрнорез (item/TurfCutterItem): одна длинная текстура 32×32, как у копья в руке, — длинное древко железного копья
(textures/item/iron_spear_in_hand.png) с лопаткой дёрнореза вместо наконечника; она же в инвентаре, в рамке и на земле.
Модель в руке — как у копья (iron_spear_in_hand.json).

Запуск из корня проекта:  python scripts/gen_turf_cutter.py   (нужен Pillow)
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
TEX = os.path.join(ROOT, "assets", NS, "textures", "item")

# Лопатка прежнего дёрнореза (строки 3–9 текстуры 16×16): A — контур, B — блик, C — металл, D — светлый металл,
# E/F — древко (светлое, среднее). Ставится сдвигом SHIFT вправо, чтобы древко продолжило древко копья.
HEAD = [
    ".......AA.A.....",
    ".......ABAA.....",
    ".......ABCCAA...",
    ".......ACCCA....",
    "......ABCCDBA...",
    "......EDCAAAA...",
    ".....EFAA.......",
]
HEAD_TOP, SHIFT = 3, 17
HEAD_COLORS = {"A": (46, 48, 52, 255), "B": (201, 205, 209, 255), "C": (111, 116, 122, 255), "D": (158, 163, 168, 255)}
# Строки 0..CUT длинной текстуры — наконечник копья с втулкой: их заменяет лопатка
CUT = 9
# Торец древка (нижний левый угол) — как лопатка: блик сверху слева, металл, контур снизу справа
BUTT = [
    "..BC",
    ".BDCA",
    ".CCAA",
    "..AA",
]
BUTT_TOP = 28


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def texture():
    img = Image.open(os.path.join(TEX, "iron_spear_in_hand.png")).convert("RGBA")
    # цвета древка (середина копья) от тёмного к светлому
    wood = sorted({img.getpixel((x, y)) for y in range(12, 24) for x in range(32) if img.getpixel((x, y))[3]}, key=lum)
    shaft = {"E": wood[-1], "F": wood[len(wood) // 2]}
    for y in range(CUT + 1):
        for x in range(32):
            img.putpixel((x, y), (0, 0, 0, 0))
    for dy, row in enumerate(BUTT):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, BUTT_TOP + dy), HEAD_COLORS[ch])
    for dy, row in enumerate(HEAD):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x + SHIFT, HEAD_TOP + dy), HEAD_COLORS.get(ch) or shaft[ch])
    return img


def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main():
    texture().save(os.path.join(TEX, "turf_cutter.png"))
    with open(os.path.join(ROOT, "assets", NS, "models", "item", "iron_spear_in_hand.json"), encoding="utf-8") as f:
        hand = json.load(f)
    hand["textures"] = {"layer0": f"{NS}:item/turf_cutter"}
    write(f"assets/{NS}/models/item/turf_cutter_in_hand.json", hand)
    flat = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/turf_cutter"}}
    write(f"assets/{NS}/models/item/turf_cutter.json", {
        "loader": "neoforge:separate_transforms", "gui_light": "front",
        "base": {"parent": f"{NS}:item/turf_cutter_in_hand"},
        "perspectives": {"gui": flat, "fixed": flat, "ground": flat}})
    # прежняя модель (лопатка и черенок отдельно) больше не нужна
    for rel in ("models/item/turf_cutter_long.json", "textures/item/turf_cutter_head.png",
                "textures/item/turf_cutter_handle.png"):
        p = os.path.join(ROOT, "assets", NS, *rel.split("/"))
        if os.path.exists(p):
            os.remove(p)
    print("ok")


if __name__ == "__main__":
    main()

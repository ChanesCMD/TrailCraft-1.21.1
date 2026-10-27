"""Метательные копья (item/ThrowingSpearItem, init/ModTools): <материал>_throwing_spear.

Пишет: текстуры (иконка и в руке — текстуры обычного копья с оперением из перьев у древка), модели (в руке — как
у обычного копья, в инвентаре и на земле — иконка), рецепты (копьё + 3 пера), переводы. Нужен Pillow.
Запуск: python scripts/gen_throwing_spear.py
"""
import json
import os
import re

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "trailcraft"
MATERIALS = ["flint", "bone", "amethyst", "copper", "bronze", "iron", "steel"]
FEATHER, FEATHER_SHADE = (236, 236, 232, 255), (186, 186, 190, 255)


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


def fletch(src):
    """Оперение: у торца древка (самый нижний-левый пиксель; древко идёт вверх-вправо) по перу с каждой стороны."""
    im = src.copy()
    w, h = im.size
    k = w // 16  # текстура в руке может быть крупнее 16×16
    opaque = [(x, y) for y in range(h) for x in range(w) if im.getpixel((x, y))[3]]
    bx, by = max(opaque, key=lambda p: p[1] - p[0])
    for step in range(1 * k, 5 * k):
        sx, sy = bx + step, by - step                         # точка на древке
        for side, color in ((1, FEATHER), (-1, FEATHER_SHADE)):
            for d in range(1, 2 * k + 1):
                x, y = sx + side * d, sy + side * d           # поперёк древка
                if 0 <= x < w and 0 <= y < h and not im.getpixel((x, y))[3]:
                    im.putpixel((x, y), color)
    return im


def main():
    tex = path("assets", NS, "textures", "item")
    for m in MATERIALS:
        for suffix in ("", "_in_hand"):
            src = Image.open(os.path.join(tex, f"{m}_spear{suffix}.png")).convert("RGBA")
            fletch(src).save(os.path.join(tex, f"{m}_throwing_spear{suffix}.png"))
        hand = read(f"assets/{NS}/models/item/{m}_spear_in_hand.json")
        hand["textures"] = {"layer0": f"{NS}:item/{m}_throwing_spear_in_hand"}
        write(f"assets/{NS}/models/item/{m}_throwing_spear_in_hand.json", hand)
        icon = {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{m}_throwing_spear"}}
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

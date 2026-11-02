"""Кремнёвые ножницы: текстура, модель и крафт."""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)
TEMPLATES = os.path.join(os.path.dirname(os.path.abspath(__file__)), "templates")


def write(path, data):
    p = os.path.join(ROOT, *path.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def main():
    base = Image.open(os.path.join(TEMPLATES, "vanilla_shears.png")).convert("RGBA")
    palette = [(38, 38, 42), (65, 65, 70), (95, 95, 100), (130, 130, 135), (170, 170, 175)]
    colors = sorted(palette, key=lum)
    pixels = list(base.get_flattened_data())
    levels = [lum(p) for p in pixels]
    lo, hi = min(levels), max(levels)
    out = []
    for p, level in zip(pixels, levels):
        if not p[3]:
            out.append((0, 0, 0, 0))
            continue
        rank = 0 if hi == lo else round((level - lo) / (hi - lo) * (len(colors) - 1))
        c = colors[max(0, min(len(colors) - 1, rank))]
        out.append((c[0], c[1], c[2], 255))
    img = Image.new("RGBA", base.size)
    img.putdata(out)
    img.save(os.path.join(ASSETS, "textures", "item", "flint_shears.png"))
    write(f"assets/{NS}/models/item/flint_shears.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/flint_shears"}})
    write(f"data/{NS}/recipe/flint_shears.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [{"item": f"{NS}:flint"}, {"item": f"{NS}:flint"}, {"item": f"{NS}:stick_processed"}],
        "result": {"id": f"{NS}:flint_shears", "count": 1},
    })
    for code, idx in (("ru_ru", 0), ("en_us", 1)):
        p = os.path.join(ASSETS, "lang", f"{code}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data[f"item.{NS}.flint_shears"] = ("Кремнёвые ножницы", "Flint Shears")[idx]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

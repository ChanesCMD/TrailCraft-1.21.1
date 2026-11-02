"""Цинковый лист, бурдюк и крафты атласа, календаря, пояса и фляги."""
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


def recolor(src, palette):
    base = Image.open(src).convert("RGBA")
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
    return img


def main():
    # цинк — холодный серо-голубой, как на фото автора
    palette = [(70, 78, 88), (100, 112, 125), (135, 150, 165), (175, 190, 205), (215, 225, 235)]
    sheet = recolor(os.path.join(TEMPLATES, "create_iron_sheet.png"), palette)
    sheet.save(os.path.join(ASSETS, "textures", "item", "zinc_sheet.png"))
    write(f"assets/{NS}/models/item/zinc_sheet.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/zinc_sheet"}})

    leather = [(72, 45, 26), (104, 67, 38), (142, 96, 55), (185, 139, 89), (220, 180, 125)]
    waterskin = recolor(os.path.join(ASSETS, "textures", "item", "flask.png"), leather)
    waterskin.save(os.path.join(ASSETS, "textures", "item", "waterskin.png"))
    write(f"assets/{NS}/models/item/waterskin.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/waterskin"}})

    leather_tag = {"tag": "c:leathers"}
    paper = {"item": "minecraft:paper"}
    charcoal = {"item": f"{NS}:charcoal"}
    thread = {"item": f"{NS}:jute_thread"}
    zinc_sheet = {"item": f"{NS}:zinc_sheet"}
    write(f"data/{NS}/recipe/atlas.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["LP", "PC"],
        "key": {"L": leather_tag, "P": paper, "C": charcoal},
        "result": {"id": f"{NS}:atlas", "count": 1},
    })
    write(f"data/{NS}/recipe/calendar.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["PP", "PC"],
        "key": {"P": paper, "C": charcoal},
        "result": {"id": f"{NS}:calendar", "count": 1},
    })
    write(f"data/{NS}/recipe/belt.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["TLT"],
        "key": {"T": thread, "L": leather_tag},
        "result": {"id": f"{NS}:belt", "count": 1},
    })
    write(f"data/{NS}/recipe/flask.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": [" T ", "Z Z", " Z "],
        "key": {"T": {"item": f"{NS}:rope"}, "Z": zinc_sheet},
        "result": {"id": f"{NS}:flask", "count": 1},
    })
    write(f"data/{NS}/recipe/medium_pocket.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["LL", "L "],
        "key": {"L": leather_tag},
        "result": {"id": f"{NS}:medium_pocket", "count": 1},
    })
    write(f"data/{NS}/recipe/leather_backpack.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "pattern": ["LLL", "L L", "LLL"],
        "key": {"L": leather_tag},
        "result": {"id": f"{NS}:leather_backpack", "count": 1},
    })
    write(f"data/{NS}/recipe/wallet.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [leather_tag, leather_tag, thread],
        "result": {"id": f"{NS}:wallet", "count": 1},
    })
    write(f"data/{NS}/recipe/waterskin.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [leather_tag, leather_tag, leather_tag, {"item": f"{NS}:rope"}],
        "result": {"id": f"{NS}:waterskin", "count": 1},
    })
    write(f"data/{NS}/recipe/zinc_sheet.json", {
        "type": "create:pressing",
        "ingredients": [{"tag": "c:ingots/zinc"}],
        "results": [{"id": f"{NS}:zinc_sheet"}],
    })
    write("data/c/tags/item/plates/zinc.json", {"replace": False, "values": [f"{NS}:zinc_sheet"]})

    for code, idx in (("ru_ru", 0), ("en_us", 1)):
        p = os.path.join(ASSETS, "lang", f"{code}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data[f"item.{NS}.zinc_sheet"] = ("Цинковый лист", "Zinc Sheet")[idx]
        data[f"item.{NS}.waterskin"] = ("Бурдюк", "Waterskin")[idx]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

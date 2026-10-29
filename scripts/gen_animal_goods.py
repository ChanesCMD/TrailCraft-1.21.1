"""
Шкура и кожа, куриное яйцо: текстуры, модификаторы лута, рецепты, переводы и превью docs/textures/animal_goods.png.

Запуск из корня проекта:  python scripts/gen_animal_goods.py   (нужен Pillow; ванильные текстуры — из jar клиента
в кэше Gradle)

Сырая шкура падает с животных вместо ванильной кожи (модификатор лута leather.json, как перо и кость), выскабливается
ножом (нож остаётся, теряет прочность — item/KnifeItem) и дубится в бочке с танином — своя кожа (c:leathers без
ванильной; танин и бочка — scripts/gen_fluids.py, block/entity/WaterBarrelBlockEntity). Курица несёт своё яйцо (TrailBird; ванильное в луте — тоже своё,
egg.json), c:eggs. Яблоко и яблоня — scripts/gen_fruit_trees.py.
Текстуры: шкуры и кожа — ванильная кожа в своих цветах, яйцо — рыжеватое.
"""
import colorsys
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar, recolor

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(HERE, "..", "docs", "textures")
RAW_HIDE = [(50, 34, 24), (78, 56, 38), (104, 78, 54), (128, 100, 72), (150, 124, 94), (176, 152, 122)]
SCRAPED_HIDE = [(96, 70, 56), (138, 104, 86), (176, 140, 118), (204, 172, 148), (226, 200, 176), (242, 224, 204)]

LANG = {
    "item.trailcraft.raw_hide": ("Сырая шкура", "Raw Hide"),
    "item.trailcraft.scraped_hide": ("Выскобленная шкура", "Scraped Hide"),
    "item.trailcraft.leather": ("Кожа", "Leather"),
    "item.trailcraft.egg": ("Яйцо", "Egg"),
}


def path(rel):
    return os.path.join(ROOT, *rel.split("/"))


def read(rel, default):
    try:
        with open(path(rel), encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return default


def write(rel, data):
    p = path(rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hsv_map(img, fn):
    img = img.convert("RGBA")
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                h, s, v = fn(*colorsys.rgb_to_hsv(r / 255, g / 255, b / 255))
                r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def textures(client):
    vanilla = lambda name: Image.open(BytesIO(client.read(f"assets/minecraft/textures/item/{name}.png")))
    items = {
        "raw_hide": recolor(vanilla("leather"), RAW_HIDE),
        "scraped_hide": recolor(vanilla("leather"), SCRAPED_HIDE),
        "leather": hsv_map(vanilla("leather"), lambda h, s, v: (h - 0.015, min(1, s * 1.1), v * 0.9)),
        "egg": hsv_map(vanilla("egg"), lambda h, s, v: (0.08, min(1, s + 0.22), v * 0.93)),
    }
    for name, img in items.items():
        img.save(os.path.join(TEX, "item", f"{name}.png"))
        write(f"assets/{NS}/models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    return list(items.values())


def loot():
    mods = read("data/neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False, "entries": []})
    for name, vanilla, ours in (("leather", "minecraft:leather", f"{NS}:raw_hide"), ("egg", "minecraft:egg", f"{NS}:egg")):
        write(f"data/{NS}/loot_modifiers/{name}.json",
              {"type": f"{NS}:replace_item", "conditions": [], "from": vanilla, "to": ours})
        if f"{NS}:{name}" not in mods["entries"]:
            mods["entries"].append(f"{NS}:{name}")
    write("data/neoforge/loot_modifiers/global_loot_modifiers.json", mods)


def recipes():
    write(f"data/{NS}/recipe/scraped_hide.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{NS}:raw_hide"}, {"tag": f"{NS}:knives"}],
        "result": {"id": f"{NS}:scraped_hide", "count": 1}})


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


def preview(images):
    scale, pad = 6, 6
    sheet = Image.new("RGBA", (pad + len(images) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "animal_goods.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    images = textures(client)
    loot()
    recipes()
    lang()
    preview(images)
    print("ok")

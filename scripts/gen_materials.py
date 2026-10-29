"""
Материалы из природы: кора, смола, джутовая нить — текстуры, модели, тэги, рецепты, переводы и превью
docs/textures/materials.png.

Запуск из корня проекта:  python scripts/gen_materials.py   (нужен Pillow; ванильные текстуры — из jar клиента в кэше
Gradle, как в gen_create_compat.py)

Кора — полоска коры дуба (textures/block/oak_log.png) с тёмным краем; падает при окорке (StrippableLogBlock). 2 коры
с водой в смесителе Create — бумага (minecraft:paper: из неё наждачка, схематика). Смола — ванильный сгусток слизи в
цветах янтаря; падает с хвойных (#trailcraft:resin_logs). Джутовая нить — ванильная нить в цветах джута
(textures/item/jute.png), в тэге c:strings: 1 джут — 2 нити.
"""
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
RESIN_LOGS = ["pine", "fir", "spruce", "cedar", "thuja"]
AMBER = [(70, 32, 6), (122, 60, 10), (170, 96, 18), (210, 136, 32), (236, 178, 64), (252, 220, 130)]

LANG = {
    "item.trailcraft.bark": ("Кора", "Bark"),
    "item.trailcraft.resin": ("Смола", "Resin"),
    "item.trailcraft.jute_thread": ("Джутовая нить", "Jute Thread"),
}

# Полоска коры: x — от, до по строкам (пусто — прозрачно)
BARK_ROWS = {3: (6, 12), 4: (4, 13), 5: (3, 13), 6: (3, 13), 7: (2, 13), 8: (2, 12), 9: (2, 12), 10: (2, 11),
             11: (3, 10), 12: (4, 8)}


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


def tag(kind, tag_id, values):
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write(rel, data)


def item_model(name):
    write(f"assets/{NS}/models/item/{name}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})


def bark():
    log = Image.open(os.path.join(TEX, "block", "oak_log.png")).convert("RGBA")
    img = Image.new("RGBA", (16, 16))
    for y, (x0, x1) in BARK_ROWS.items():
        for x in range(x0, x1):
            c = log.getpixel((x, y))
            edge = x in (x0, x1 - 1) or y in (min(BARK_ROWS), max(BARK_ROWS)) or (y + 1 in BARK_ROWS and not (
                BARK_ROWS[y + 1][0] <= x < BARK_ROWS[y + 1][1]))
            img.putpixel((x, y), tuple(int(v * 0.6) for v in c[:3]) + (255,) if edge else c[:3] + (255,))
    return img


def main():
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    vanilla = lambda name: Image.open(BytesIO(client.read(f"assets/minecraft/textures/item/{name}.png")))
    jute = Image.open(os.path.join(TEX, "item", "jute.png")).convert("RGBA")
    jute_colors = sorted({c[:3] for c in jute.get_flattened_data() if c[3]}, key=sum)
    textures = {
        "bark": bark(),
        "resin": recolor(vanilla("slime_ball"), AMBER),
        "jute_thread": recolor(vanilla("string"), jute_colors),
    }
    for name, img in textures.items():
        img.save(os.path.join(TEX, "item", f"{name}.png"))
        item_model(name)
    tag("block", f"{NS}:resin_logs", [f"{NS}:{w}_{k}" for w in RESIN_LOGS for k in ("log", "wood")])
    tag("item", "c:strings", [f"{NS}:jute_thread"])
    write(f"data/{NS}/recipe/jute_thread.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{NS}:jute"}], "result": {"id": f"{NS}:jute_thread", "count": 2}})
    write(f"data/{NS}/recipe/paper_from_bark.json", {
        "type": "create:mixing",
        "ingredients": [{"item": f"{NS}:bark"}, {"item": f"{NS}:bark"},
                        {"type": "neoforge:single", "amount": 250, "fluid": "minecraft:water"}],
        "results": [{"id": "minecraft:paper"}]})
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)
    scale, pad = 8, 8
    sheet = Image.new("RGBA", (pad + len(textures) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(textures.values()):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "materials.png"))


if __name__ == "__main__":
    main()
    print("ok")

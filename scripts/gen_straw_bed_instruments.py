"""
Кровать из соломы (block/StrawBedBlock.java) и рецепты приборов пояса (высотомер, секстант, хронометр) с мини-механизмом
точности: текстуры, модели, блокстейт, лут, рецепты, тэг мотыги, переводы и превью docs/textures/straw_bed_instruments.png.

Запуск из корня проекта:  python scripts/gen_straw_bed_instruments.py   (нужен Pillow)

Кровать — циновка из соломы 16×3×16: соломины вдоль X цветами предмета соломы (textures/item/straw.png), между пучками
тёмные щели. Рецепт — 4 соломы (2×2).
Приборы — из деталей Create: секстант — шестерня и 3 латунных слитка дугой; высотомер — 4 латунных листа вокруг
пузырька с водой (ингредиент neoforge:components — зелье «вода»); хронометр — мини-механизм точности над малой доской
любой породы (#trailcraft:small_boards). Мини-механизм — золотой лист, шестерня, вал и кусочек железа (без формы);
текстура — механизм точности Create (scripts/templates/precision_mechanism.png), уменьшенный до 12×12 в его же палитру.
"""
import json
import os
import random

from PIL import Image

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(HERE, "..", "docs", "textures")
MINI = 12  # размер мини-механизма на текстуре 16×16

WATER_BOTTLE = {"type": "neoforge:components", "items": "minecraft:potion",
                "components": {"minecraft:potion_contents": {"potion": "minecraft:water"}}}

LANG = {
    "block.trailcraft.straw_bed": ("Кровать из соломы", "Straw Bed"),
    "block.trailcraft.straw_bed.no_spawn": ("Здесь нельзя возродиться", "You can't respawn here"),
    "item.trailcraft.mini_precision_mechanism": ("Мини-механизм точности", "Mini Precision Mechanism"),
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


def tag(kind, tag_id, values):
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write(rel, data)


def straw_palette():
    """Цвета предмета соломы по яркости: тени, середина, светлые соломины."""
    img = Image.open(os.path.join(TEX, "item", "straw.png")).convert("RGBA")
    colors = sorted({c for c in img.get_flattened_data() if c[3]}, key=lambda c: sum(c[:3]))
    return colors[:2], colors[2:4], colors[4:]


def straw_bed_textures():
    rnd = random.Random(7)
    dark, mid, light = straw_palette()
    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        gap = y % 4 == 3  # щель между пучками
        x = rnd.randint(-3, 0)
        while x < 16:
            run = rnd.randint(3, 7)  # соломина
            if gap:
                c = rnd.choice(dark) if rnd.random() < 0.5 else mid[0]
            else:
                c = rnd.choice(light) if rnd.random() < 0.7 else mid[1]
            for i in range(max(0, x), min(16, x + run)):
                top.putpixel((i, y), c)
            x += run
    top.save(os.path.join(TEX, "block", "straw_bed_top.png"))
    # бок (видны нижние 3 строки, uv 13–16): торцы соломин — светлые точки на средних, нижняя строка — тень
    side = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            row = y % 3
            if row == 2:
                c = rnd.choice(dark)
            elif (x + row) % 2:
                c = rnd.choice(light)
            else:
                c = mid[1]
            side.putpixel((x, y), c)
    side.save(os.path.join(TEX, "block", "straw_bed_side.png"))
    return top


def straw_bed():
    face_side = {"uv": [0, 13, 16, 16], "texture": "#side"}
    write(f"assets/{NS}/models/block/straw_bed.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{NS}:block/straw_bed_top", "top": f"{NS}:block/straw_bed_top",
                     "side": f"{NS}:block/straw_bed_side"},
        "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "faces": {
            "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
            "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"},
            "north": {**face_side, "cullface": "north"}, "south": {**face_side, "cullface": "south"},
            "east": {**face_side, "cullface": "east"}, "west": {**face_side, "cullface": "west"}}}]})
    write(f"assets/{NS}/blockstates/straw_bed.json", {"variants": {"": {"model": f"{NS}:block/straw_bed"}}})
    write(f"assets/{NS}/models/item/straw_bed.json", {"parent": f"{NS}:block/straw_bed"})
    write(f"data/{NS}/loot_table/blocks/straw_bed.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:straw_bed"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/straw_bed"})
    write(f"data/{NS}/recipe/straw_bed.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"S": {"item": f"{NS}:straw"}}, "pattern": ["SS", "SS"],
        "result": {"id": f"{NS}:straw_bed", "count": 1}})
    tag("block", "minecraft:mineable/hoe", [f"{NS}:straw_bed"])


def mini_mechanism():
    src = Image.open(os.path.join(HERE, "templates", "precision_mechanism.png")).convert("RGBA")
    palette = [c for c in set(src.get_flattened_data()) if c[3] == 255]
    small = src.resize((MINI, MINI), Image.BOX)
    img = Image.new("RGBA", (16, 16))
    off = (16 - MINI) // 2
    for y in range(MINI):
        for x in range(MINI):
            c = small.getpixel((x, y))
            if c[3] >= 128:
                img.putpixel((x + off, y + off), min(palette, key=lambda p: sum((p[i] - c[i]) ** 2 for i in range(3))))
    img.save(os.path.join(TEX, "item", "mini_precision_mechanism.png"))
    write(f"assets/{NS}/models/item/mini_precision_mechanism.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/mini_precision_mechanism"}})
    return img


def recipes():
    write(f"data/{NS}/recipe/mini_precision_mechanism.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": "create:golden_sheet"}, {"item": "create:cogwheel"}, {"item": "create:shaft"},
                        {"item": "minecraft:iron_nugget"}],
        "result": {"id": f"{NS}:mini_precision_mechanism", "count": 1}})
    write(f"data/{NS}/recipe/sextant.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"B": {"item": "create:brass_ingot"}, "C": {"item": "create:cogwheel"}},
        "pattern": [" B ", "BCB"],
        "result": {"id": f"{NS}:sextant", "count": 1}})
    write(f"data/{NS}/recipe/brass_altimeter.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"S": {"item": "create:brass_sheet"}, "W": WATER_BOTTLE},
        "pattern": [" S ", "SWS", " S "],
        "result": {"id": f"{NS}:brass_altimeter", "count": 1}})
    write(f"data/{NS}/recipe/chronometer.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"M": {"item": f"{NS}:mini_precision_mechanism"}, "B": {"tag": f"{NS}:small_boards"}},
        "pattern": ["M", "B"],
        "result": {"id": f"{NS}:chronometer", "count": 1}})


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


def preview(images):
    scale, pad = 8, 8
    sheet = Image.new("RGBA", (pad + len(images) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "straw_bed_instruments.png"))


if __name__ == "__main__":
    top = straw_bed_textures()
    straw_bed()
    mini = mini_mechanism()
    recipes()
    lang()
    preview([top, Image.open(os.path.join(TEX, "block", "straw_bed_side.png")), mini])
    print("ok")

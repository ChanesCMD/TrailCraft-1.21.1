"""
Блок соломы, кровать из соломы (block/StrawBedBlock.java) и рецепты приборов пояса (высотомер, секстант, хронометр) с мини-механизмом
точности: текстуры, модели, блокстейт, лут, рецепты, тэг мотыги, переводы и превью docs/textures/straw_bed_instruments.png.

Запуск из корня проекта:  python scripts/gen_straw_bed_instruments.py   (нужен Pillow)

Блок соломы — 4 соломы (2×2), обратно — 4 соломы; ванильный тюк сена (HayBlock: ставится по осям, гасит урон от
падения): бок — соломины вдоль оси и две перевязки, торец — срезы соломин. Кровать — соломенный тюфяк 4 пикселя
высотой в два блока (изножье и изголовье с валиком) цветами предмета соломы (textures/item/straw.png). Рецепт — 2 блока
соломы рядом.
Приборы — из деталей Create: секстант — шестерня и 3 латунных слитка дугой; высотомер — 4 латунных листа вокруг
пузырька с водой (ингредиент neoforge:components — зелье «вода»); хронометр — мини-механизм точности над досками
любой породы (#minecraft:planks). Мини-механизм — золотой лист, шестерня, вал и кусочек железа (без формы);
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
    "block.trailcraft.straw_block": ("Блок соломы", "Straw Block"),
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


def strands(img, box, vertical, rnd, light, mid):
    """Соломины в прямоугольнике box (x0, y0, x1, y1): вдоль y (vertical) или вдоль x, отрезками разной длины."""
    x0, y0, x1, y1 = box
    start, stop = (y0, y1) if vertical else (x0, x1)
    for line in (range(x0, x1) if vertical else range(y0, y1)):
        pos = start - rnd.randint(0, 4)
        while pos < stop:
            run = rnd.randint(3, 7)
            c = rnd.choice(light) if rnd.random() < 0.75 else mid[1]
            for i in range(max(pos, start), min(pos + run, stop)):
                img.putpixel((line, i) if vertical else (i, line), c)
            pos += run


def straw_bed_textures():
    """
    Тюфяк смотрит изголовьем на север (v = 0 — северный край). Изножье и изголовье: соломины вдоль кровати, тёмная
    перевязка поперёк, края темнее; у изголовья — валик соломы поперёк. Бок — соломины вдоль, торец — срезы соломин
    (видны нижние 4 строки, uv 12–16).
    """
    rnd = random.Random(7)
    dark, mid, light = straw_palette()
    textures = {}
    for part in ("head", "foot"):
        img = Image.new("RGBA", (16, 16))
        strands(img, (0, 0, 16, 16), True, rnd, light, mid)
        if part == "head":
            strands(img, (0, 1, 16, 6), False, rnd, light, mid)  # валик-подушка
            for x in range(16):
                img.putpixel((x, 0), mid[0])
                img.putpixel((x, 6), dark[x % 2])
        else:
            for x in range(16):
                img.putpixel((x, 8), dark[x % 2])
                img.putpixel((x, 15), mid[0])
        for y in range(16):  # края тюфяка темнее
            for x in (0, 15):
                if img.getpixel((x, y)) in light:
                    img.putpixel((x, y), mid[1])
        img.save(os.path.join(TEX, "block", f"straw_bed_{part}.png"))
        textures[part] = img
    side = Image.new("RGBA", (16, 16), mid[1])
    strands(side, (0, 12, 16, 15), False, rnd, light, mid)
    for x in range(16):
        side.putpixel((x, 15), dark[x % 2])
    side.save(os.path.join(TEX, "block", "straw_bed_side.png"))
    end = Image.new("RGBA", (16, 16), mid[1])
    for x in range(16):
        for y in range(12, 15):
            end.putpixel((x, y), rnd.choice(light) if rnd.random() < 0.6 else rnd.choice(dark + mid))
        end.putpixel((x, 15), dark[x % 2])
    end.save(os.path.join(TEX, "block", "straw_bed_end.png"))
    old = os.path.join(TEX, "block", "straw_bed_top.png")
    if os.path.exists(old):
        os.remove(old)
    return textures, side, end


def straw_bed():
    """Изножье и изголовье (facing — от изножья к изголовью, модели смотрят на север), 16×4×16 каждое."""
    side = {"uv": [0, 12, 16, 16], "texture": "#side"}
    end = {"uv": [0, 12, 16, 16], "texture": "#end"}
    for part in ("head", "foot"):
        faces = {"up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                 "down": {"uv": [0, 0, 16, 16], "texture": "#top", "cullface": "down"},
                 "east": side, "west": side}
        # торец только снаружи: со стороны второй половины грани нет
        faces["north" if part == "head" else "south"] = end
        write(f"assets/{NS}/models/block/straw_bed_{part}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": f"{NS}:block/straw_bed_{part}", "top": f"{NS}:block/straw_bed_{part}",
                         "side": f"{NS}:block/straw_bed_side", "end": f"{NS}:block/straw_bed_end"},
            "elements": [{"from": [0, 0, 0], "to": [16, 4, 16], "faces": faces}]})
    old = path(f"assets/{NS}/models/block/straw_bed.json")
    if os.path.exists(old):
        os.remove(old)
    variants = {}
    for facing, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)):
        for part in ("head", "foot"):
            v = {"model": f"{NS}:block/straw_bed_{part}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},part={part}"] = v
    write(f"assets/{NS}/blockstates/straw_bed.json", {"variants": variants})
    # предмет — кровать целиком (изголовье и изножье), иначе в инвентаре видна половина без одного торца
    item_faces = lambda top, end_face: {
        "up": {"uv": [0, 0, 16, 16], "texture": top}, "down": {"uv": [0, 0, 16, 16], "texture": top},
        "east": side, "west": side, end_face: end}
    write(f"assets/{NS}/models/item/straw_bed.json", {
        "parent": "minecraft:block/block",
        "textures": {"particle": f"{NS}:block/straw_bed_head", "head": f"{NS}:block/straw_bed_head",
                     "foot": f"{NS}:block/straw_bed_foot", "side": f"{NS}:block/straw_bed_side",
                     "end": f"{NS}:block/straw_bed_end"},
        "elements": [{"from": [0, 0, -8], "to": [16, 4, 8], "faces": item_faces("#head", "north")},
                     {"from": [0, 0, 8], "to": [16, 4, 24], "faces": item_faces("#foot", "south")}],
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
            "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]}}})
    write(f"data/{NS}/loot_table/blocks/straw_bed.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:straw_bed"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"},
                                  {"condition": "minecraft:block_state_property", "block": f"{NS}:straw_bed",
                                   "properties": {"part": "head"}}]}],
        "random_sequence": f"{NS}:blocks/straw_bed"})
    write(f"data/{NS}/recipe/straw_bed.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"S": {"item": f"{NS}:straw_block"}}, "pattern": ["SS"],
        "result": {"id": f"{NS}:straw_bed", "count": 1}})
    tag("block", "minecraft:mineable/hoe", [f"{NS}:straw_bed", f"{NS}:straw_block"])


def straw_block():
    """Тюк соломы: бок — соломины вдоль оси и две тёмные перевязки, торец — срезы соломин."""
    rnd = random.Random(11)
    dark, mid, light = straw_palette()
    side = Image.new("RGBA", (16, 16))
    for x in range(16):
        y = rnd.randint(-4, 0)
        while y < 16:
            run = rnd.randint(3, 7)
            c = rnd.choice(light) if (x % 4 != 3 or rnd.random() < 0.4) and rnd.random() < 0.75 else mid[1]
            for i in range(max(0, y), min(16, y + run)):
                side.putpixel((x, i), c)
            y += run
    for y in (3, 12):  # перевязки
        for x in range(16):
            side.putpixel((x, y), dark[1] if x % 2 else dark[0])
    side.save(os.path.join(TEX, "block", "straw_block_side.png"))
    top = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            edge = x in (0, 15) or y in (0, 15)
            c = mid[1] if edge else (rnd.choice(light) if rnd.random() < 0.65 else rnd.choice(dark + mid))
            top.putpixel((x, y), c)
    top.save(os.path.join(TEX, "block", "straw_block_top.png"))
    textures = {"end": f"{NS}:block/straw_block_top", "side": f"{NS}:block/straw_block_side"}
    write(f"assets/{NS}/models/block/straw_block.json", {"parent": "minecraft:block/cube_column", "textures": textures})
    write(f"assets/{NS}/models/block/straw_block_horizontal.json",
          {"parent": "minecraft:block/cube_column_horizontal", "textures": textures})
    model, horizontal = f"{NS}:block/straw_block", f"{NS}:block/straw_block_horizontal"
    write(f"assets/{NS}/blockstates/straw_block.json", {"variants": {
        "axis=x": {"model": horizontal, "x": 90, "y": 90}, "axis=y": {"model": model},
        "axis=z": {"model": horizontal, "x": 90}}})
    write(f"assets/{NS}/models/item/straw_block.json", {"parent": model})
    write(f"data/{NS}/loot_table/blocks/straw_block.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:straw_block"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/straw_block"})
    write(f"data/{NS}/recipe/straw_block.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "key": {"S": {"item": f"{NS}:straw"}}, "pattern": ["SS", "SS"],
        "result": {"id": f"{NS}:straw_block", "count": 1}})
    write(f"data/{NS}/recipe/straw_from_block.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{NS}:straw_block"}],
        "result": {"id": f"{NS}:straw", "count": 4}})
    return side, top


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
        "key": {"M": {"item": f"{NS}:mini_precision_mechanism"}, "B": {"tag": "minecraft:planks"}},
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
    bed, bed_side, bed_end = straw_bed_textures()
    block_side, block_top = straw_block()
    straw_bed()
    mini = mini_mechanism()
    recipes()
    lang()
    preview([block_side, block_top, bed["head"], bed["foot"], bed_side, bed_end, mini])
    print("ok")

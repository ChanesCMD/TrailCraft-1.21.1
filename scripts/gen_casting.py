"""
Литьё (как в TerraFirmaCraft; smelting/Mold.java, MoldItem, Molten, init/ModSmelting.java): сырые формы из слепка,
обжиг на костре в формы, головки инструментов из форм (медь и бронза), инструменты из головки и черенка; вид кувшина
с жидким металлом; свои перо и кость (вместо ванильных: лут — loot/ReplaceItemModifier).

Пишет: модели, рецепты (крафт инструментов из головок заменяет прежний крафт из слитков у меди и бронзы), обжиг
на костре, переводы, текстуры (нужен Pillow): головки — текстура инструмента без черенка; формы — глиняная плитка
с углублением по силуэту головки (у формы слитка — прямоугольное), залитая — форма и поверх серый слой металла
(<форма>_mold_cast, в игре красится в цвет металла); превью —
docs/textures/casting.png.

Запуск из корня проекта:  python scripts/gen_casting.py   (после gen_smelting.py: берёт его кувшин и цвета глины)
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(ROOT, "src", "main", "resources")
NS = "trailcraft"
TEX = os.path.join(RES, "assets", NS, "textures", "item")
DOCS = os.path.join(ROOT, "docs", "textures")

# Формы (Mold): id, ru (форма чего), en; у инструментов — головка: ru-существительное, его род, en
MOLDS = [
    ("ingot", "слитка", "Ingot", None),
    ("pickaxe", "кирки", "Pickaxe", ("головка кирки", "f", "Pickaxe Head")),
    ("axe", "топора", "Axe", ("головка топора", "f", "Axe Head")),
    ("shovel", "лопаты", "Shovel", ("головка лопаты", "f", "Shovel Head")),
    ("hoe", "тяпки", "Garden Hoe", ("головка тяпки", "f", "Garden Hoe Head")),
    ("knife", "ножа", "Knife", ("клинок ножа", "m", "Knife Blade")),
    ("spear", "копья", "Spear", ("наконечник копья", "m", "Spear Head")),
    ("saw", "пилы", "Saw", ("полотно пилы", "n", "Saw Blade")),
    ("sickle", "серпа", "Sickle", ("лезвие серпа", "n", "Sickle Blade")),
    ("scythe", "косы", "Scythe", ("лезвие косы", "n", "Scythe Blade")),
    ("cutters", "кусачек", "Cutters", ("губки кусачек", "p", "Cutter Jaws")),
]
# Материалы, которые отливаются (ModSmelting.CAST_MATERIALS): прилагательное по родам, en
MATERIALS = {
    "copper": ({"f": "Медная", "m": "Медный", "n": "Медное", "p": "Медные"}, "Copper"),
    "bronze": ({"f": "Бронзовая", "m": "Бронзовый", "n": "Бронзовое", "p": "Бронзовые"}, "Bronze"),
}
# Инструмент из головки: копьё и коса — на длинной палке по диагонали, остальные — головка над обработанной палкой
LONG_HANDLE = {"spear", "scythe"}
# Металлы (Smelting.Metal): ru (название), ru (жидкий), en
METALS = {
    "copper": ("медь", "Жидкая медь", "Copper"),
    "tin": ("олово", "Жидкое олово", "Tin"),
    "zinc": ("цинк", "Жидкий цинк", "Zinc"),
    "bismuth": ("висмут", "Жидкий висмут", "Bismuth"),
    "lead": ("свинец", "Жидкий свинец", "Lead"),
    "gold": ("золото", "Жидкое золото", "Gold"),
    "tin_bronze": ("оловянная бронза", "Жидкая оловянная бронза", "Tin Bronze"),
    "bismuth_bronze": ("висмутовая бронза", "Жидкая висмутовая бронза", "Bismuth Bronze"),
    "brass": ("латунь", "Жидкая латунь", "Brass"),
}
LANG = {
    "tooltip.trailcraft.molten": ("%s, %s mB", "%s, %s mB"),
    "tooltip.trailcraft.molten.amount": ("%s mB", "%s mB"),
    "gui.trailcraft.jug.mold": ("Форма", "Mold"),
    "tooltip.trailcraft.mold.cast": ("Залито: %s", "Cast: %s"),
    "tooltip.trailcraft.mold.take": ("ПКМ — достать отливку", "Right-click to take the casting out"),
    "tooltip.trailcraft.mold.empty": ("Положить в слот формы в окне кувшина с жидким металлом",
                                      "Put into the mold slot of a jug with molten metal"),
    "item.trailcraft.feather": ("Перо", "Feather"),
    "item.trailcraft.bone": ("Кость", "Bone"),
}

# Цвета черенка (обработанная палка) — их нет у головки
HANDLE = {(69, 48, 26), (107, 76, 40), (143, 106, 58), (184, 148, 90), (212, 176, 122)}
# Формы: сырая глина (как сырой кувшин) и обожжённая (как кувшин) — gen_smelting.RAW_CLAY, FIRED_JUG
RAW_CLAY = (160, 150, 140)
FIRED = (184, 102, 64)
# Застывший металл в залитой форме — отдельный серый слой (<форма>_mold_cast), игра красит его в цвет металла
# (Metal.color, client/ClientCasting): тень у верхнего и левого края, основной, искорки, блик у нижнего и правого
CAST_GRAYS = [120, 210, 238, 255]
# Цвета металлов — как Metal.color в smelting/Smelting.java (здесь — только для превью)
METAL_COLORS = {
    "copper": (232, 132, 84), "tin": (210, 214, 218), "zinc": (176, 190, 190), "bismuth": (200, 170, 190),
    "lead": (130, 138, 160), "gold": (248, 204, 74), "tin_bronze": (214, 160, 80),
    "bismuth_bronze": (190, 150, 100), "brass": (226, 190, 82),
}
# Жидкий металл в горлышке кувшина
MOLTEN_GLOW = [(255, 214, 90), (255, 160, 40), (226, 96, 24)]


def write(rel, data):
    p = os.path.join(RES, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(rel):
    with open(os.path.join(RES, *rel.split("/")), encoding="utf-8") as f:
        return json.load(f)


def shade(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c[:3]) + (255,)


# ---------- текстуры ----------

def load(name):
    return Image.open(os.path.join(TEX, name + ".png")).convert("RGBA")


def largest_part(pixels):
    """Самый большой связный (по 8 соседям) кусок множества пикселей: у копья отпадают торец и лишние точки."""
    left, best = set(pixels), set()
    while left:
        start = left.pop()
        part, stack = {start}, [start]
        while stack:
            x, y = stack.pop()
            for dx in (-1, 0, 1):
                for dy in (-1, 0, 1):
                    n = (x + dx, y + dy)
                    if n in left:
                        left.remove(n)
                        part.add(n)
                        stack.append(n)
        if len(part) > len(best):
            best = part
    return best


def centered(img, pixels):
    """Пиксели pixels из img, сдвинутые в середину 16×16."""
    xs, ys = [x for x, _ in pixels], [y for _, y in pixels]
    dx = (16 - (max(xs) - min(xs) + 1)) // 2 - min(xs)
    dy = (16 - (max(ys) - min(ys) + 1)) // 2 - min(ys)
    out = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, y in pixels:
        out.putpixel((x + dx, y + dy), img.getpixel((x, y)))
    return out


def head(material, tool):
    """Головка: текстура инструмента без черенка, посередине."""
    img = load(f"{material}_{tool}")
    # без черенка и без серых пикселей (обмотка и торец у копья)
    pixels = [(x, y) for y in range(16) for x in range(16)
              if img.getpixel((x, y))[3] and img.getpixel((x, y))[:3] not in HANDLE
              and max(img.getpixel((x, y))[:3]) - min(img.getpixel((x, y))[:3]) > 8]
    return centered(img, largest_part(pixels))


def mask(img):
    return {(x, y) for y in range(16) for x in range(16) if img.getpixel((x, y))[3]}


def mold(shape, clay):
    """Форма: глиняная плитка со скруглёнными углами, свет сверху слева; в ней углубление по силуэту shape."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    plate = {(x, y) for y in range(1, 15) for x in range(16)
             if not ((x in (0, 15)) and (y in (1, 14)))}
    for x, y in plate:
        k = 1.0
        if (x, y - 1) not in plate or (x - 1, y) not in plate:
            k = 1.15
        elif (x, y + 1) not in plate or (x + 1, y) not in plate:
            k = 0.72
        img.putpixel((x, y), shade(clay, k * (0.97 if (x * 7 + y * 3) % 5 == 0 else 1.0)))
    for x, y in shape & plate:
        # углубление: тень сверху и слева, дно темнее плитки, нижний и правый край светлее
        if (x, y - 1) not in shape or (x - 1, y) not in shape:
            k = 0.45
        elif (x, y + 1) not in shape or (x + 1, y) not in shape:
            k = 0.85
        else:
            k = 0.62
        img.putpixel((x, y), shade(clay, k))
    return img


def cast(shape):
    """Слой залитого металла: серый по силуэту углубления (в игре красится в цвет металла)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x, y in shape:
        if not (0 <= x < 16 and 1 <= y < 15):
            continue
        if (x, y - 1) not in shape or (x - 1, y) not in shape:
            g = CAST_GRAYS[0]
        elif (x, y + 1) not in shape or (x + 1, y) not in shape:
            g = CAST_GRAYS[3]
        else:
            g = CAST_GRAYS[2 if (x + 2 * y) % 5 == 0 else 1]
        img.putpixel((x, y), (g, g, g, 255))
    return img


def tinted(base, layer, color):
    """Как игра рисует залитую форму: слой металла, умноженный на цвет металла, поверх формы."""
    img = base.copy()
    for y in range(16):
        for x in range(16):
            c = layer.getpixel((x, y))
            if c[3]:
                img.putpixel((x, y), tuple(c[i] * color[i] // 255 for i in range(3)) + (255,))
    return img


# Углубление формы слитка: слиток сверху, стенки скошены (верх уже низа)
INGOT_SHAPE = {(x, y) for y in range(5, 11) for x in range(3 + (y == 5), 13 - (y == 5))}


def mold_shapes():
    """Силуэт углубления каждой формы: слиток — INGOT_SHAPE, инструменты — по медной головке."""
    shapes = {"ingot": INGOT_SHAPE}
    for tool, *_ in MOLDS[1:]:
        shapes[tool] = mask(head("copper", tool))
    return shapes


def molten_jug():
    """Кувшин с жидким металлом: тёмное горлышко (строка 2) светится расплавом."""
    img = load("jug")
    mouth = [x for x in range(16) if img.getpixel((x, 2))[3] and sum(img.getpixel((x, 2))[:3]) < 260]
    for i, x in enumerate(mouth):
        edge = i in (0, len(mouth) - 1)
        img.putpixel((x, 2), MOLTEN_GLOW[2 if edge else (i % 2)] + (255,))
    return img


def textures():
    out = {}
    for material in MATERIALS:
        for tool, _ru, _en, h in MOLDS:
            if h:
                out[f"{material}_{tool}_head"] = head(material, tool)
    for tool, shape in mold_shapes().items():
        out[f"unfired_{tool}_mold"] = mold(shape, RAW_CLAY)
        out[f"{tool}_mold"] = mold(shape, FIRED)
        out[f"{tool}_mold_cast"] = cast(shape)
    out["jug_molten"] = molten_jug()
    for name, img in out.items():
        img.save(os.path.join(TEX, name + ".png"))
    preview(out)


def preview(out):
    """Превью ×6 на сером фоне: формы (сырая, обожжённая, залитая медью и бронзой), головки, форма слитка со всеми
    металлами, кувшин с жидким металлом и обычный."""
    def filled(tool, metal):
        return tinted(out[f"{tool}_mold"], out[f"{tool}_mold_cast"], METAL_COLORS[metal])

    rows = [[out[f"unfired_{t}_mold"], out[f"{t}_mold"], filled(t, "copper"), filled(t, "tin_bronze")]
            for t, *_ in MOLDS]
    rows += [[out[f"{m}_{t}_head"] for t, _r, _e, h in MOLDS if h] for m in MATERIALS]
    rows.append([filled("ingot", metal) for metal in METAL_COLORS])
    rows.append([out["jug_molten"], load("jug")])
    scale, pad = 6, 6
    cell = 16 * scale + pad
    # формы — по две четвёрки в ряд
    lines = [rows[i] + rows[i + 1] if i + 1 < len(MOLDS) else rows[i] for i in range(0, len(MOLDS), 2)]
    lines += rows[len(MOLDS):]
    sheet = Image.new("RGBA", (max(len(r) for r in lines) * cell + pad, len(lines) * cell + pad), (60, 60, 60, 255))
    for i, row in enumerate(lines):
        for j, img in enumerate(row):
            big = img.resize((16 * scale, 16 * scale), Image.NEAREST)
            sheet.paste(big, (pad + j * cell, pad + i * cell), big)
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "casting.png"))


# ---------- JSON ----------

def item_model(name, texture=None, parent="minecraft:item/generated", overrides=None):
    model = {"parent": parent, "textures": {"layer0": texture or f"{NS}:item/{name}"}}
    if overrides:
        model["overrides"] = overrides
    write(f"assets/{NS}/models/item/{name}.json", model)


def assets():
    for tool, *_ in MOLDS:
        item_model(f"unfired_{tool}_mold")
        write(f"assets/{NS}/models/item/{tool}_mold_filled.json", {"parent": "minecraft:item/generated", "textures": {
            "layer0": f"{NS}:item/{tool}_mold", "layer1": f"{NS}:item/{tool}_mold_cast"}})
        item_model(f"{tool}_mold", overrides=[
            {"predicate": {f"{NS}:filled": 1}, "model": f"{NS}:item/{tool}_mold_filled"}])
    for material in MATERIALS:
        for tool, _ru, _en, h in MOLDS:
            if h:
                item_model(f"{material}_{tool}_head")
    item_model("jug_molten")
    item_model("jug", overrides=[{"predicate": {f"{NS}:molten": 1}, "model": f"{NS}:item/jug_molten"}])
    # свои перо и кость пока выглядят как ванильные
    item_model("feather", "minecraft:item/feather")
    item_model("bone", "minecraft:item/bone", parent="minecraft:item/handheld")


def data():
    for tool, *_ in MOLDS:
        write(f"data/{NS}/recipe/campfire/{tool}_mold.json", {
            "type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 600, "experience": 0.3,
            "ingredient": {"item": f"{NS}:unfired_{tool}_mold"}, "result": {"id": f"{NS}:{tool}_mold", "count": 1}})
    # инструменты меди и бронзы — только из отлитых головок (прежние рецепты из слитков заменяются)
    for material in MATERIALS:
        for tool, _ru, _en, h in MOLDS:
            if not h:
                continue
            head_item = {"item": f"{NS}:{material}_{tool}_head"}
            if tool in LONG_HANDLE:
                pattern, key = [" H", "L "], {"H": head_item, "L": {"item": f"{NS}:long_stick"}}
            else:
                pattern, key = ["H", "S"], {"H": head_item, "S": {"item": f"{NS}:stick_processed"}}
            write(f"data/{NS}/recipe/{material}_{tool}.json", {
                "type": "minecraft:crafting_shaped", "category": "equipment", "key": key, "pattern": pattern,
                "result": {"id": f"{NS}:{material}_{tool}", "count": 1}})
    # свои перо и кость — в общих тэгах, ванильные стрелы и костная мука делаются и из них
    write("data/c/tags/item/feathers.json", {"replace": False, "values": [f"{NS}:feather"]})
    write("data/c/tags/item/bones.json", {"replace": False, "values": [f"{NS}:bone"]})
    write("data/minecraft/recipe/arrow.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"#": {"item": "minecraft:stick"}, "X": {"item": "minecraft:flint"}, "Y": {"tag": "c:feathers"}},
        "pattern": ["X", "#", "Y"], "result": {"id": "minecraft:arrow", "count": 4}})
    write("data/minecraft/recipe/bone_meal.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "group": "bonemeal",
        "ingredients": [{"tag": "c:bones"}], "result": {"id": "minecraft:bone_meal", "count": 3}})
    # ванильные перо и кость из любого лута — свои (loot/ReplaceItemModifier)
    for name, item in (("feather", "feather"), ("bone", "bone")):
        write(f"data/{NS}/loot_modifiers/{name}.json", {
            "type": f"{NS}:replace_item", "conditions": [], "from": f"minecraft:{item}", "to": f"{NS}:{item}"})
    write("data/neoforge/loot_modifiers/global_loot_modifiers.json", {
        "replace": False, "entries": [f"{NS}:feather", f"{NS}:bone"]})


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for tool, ru, en, h in MOLDS:
            data[f"item.{NS}.unfired_{tool}_mold"] = (f"Сырая форма {ru}", f"Unfired {en} Mold")[k]
            data[f"item.{NS}.{tool}_mold"] = (f"Форма {ru}", f"{en} Mold")[k]
            if h:
                noun, gender, en_head = h
                for material, (adj, en_material) in MATERIALS.items():
                    data[f"item.{NS}.{material}_{tool}_head"] = (f"{adj[gender]} {noun}", f"{en_material} {en_head}")[k]
        for metal, (ru, ru_molten, en) in METALS.items():
            data[f"metal.{NS}.{metal}"] = (ru, en.lower())[k]
            data[f"metal.{NS}.{metal}.molten"] = (ru_molten, f"Molten {en}")[k]
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


if __name__ == "__main__":
    textures()
    assets()
    data()
    lang()
    print("ok")

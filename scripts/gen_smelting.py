"""Глина, кирпичи и плавка (пакет smelting, init/ModSmelting.java): JSON и текстуры.

Пишет: кирпичи четырёх глин (предмет brick_<глина> — обжиг комка глины на костре; блок bricks_<глина> — 4 кирпича),
солому, укреплённый (саманный) кирпич, малую сыродутную печь, слепок из глины, сырой и обожжённый кувшин —
модели, блокстейты, лут, тэги, рецепты, переводы, текстуры (нужен Pillow). Убирает плавку металлов на костре
(металлы теперь плавятся только в кувшине в малой сыродутной печи — smelting/Smelting.java).
Запуск: python scripts/gen_smelting.py
"""
import json
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "trailcraft"

# глина: цвет обожжённого кирпича, ru (кирпич, блок), en
CLAYS = {
    "white": ((226, 208, 170), "Белый кирпич", "Белые кирпичи", "White"),
    "blue": ((146, 164, 178), "Голубой кирпич", "Голубые кирпичи", "Blue"),
    "gray": ((112, 106, 100), "Серый кирпич", "Серые кирпичи", "Gray"),
    "red": ((176, 86, 58), "Красный кирпич", "Красные кирпичи", "Red"),
}
ADOBE = (150, 118, 82)       # укреплённый кирпич: глина с соломой
STRAW = (222, 190, 96)
RAW_CLAY = (160, 150, 140)   # сырой кувшин
MOLD_CLAY = (138, 128, 118)  # слепок (самые светлые места шаблона красителя — в 1,3 раза светлее)
FIRED_JUG = (184, 102, 64)

# Металлы на костре больше не плавятся: эти рецепты удаляются (костёр по-прежнему жжёт древесный уголь)
CAMPFIRE_METALS = [
    "aluminum_ingot_from_raw_bauxite", "aluminum_ingot_from_raw_nepheline", "bismuth_bronze_ingot_from_bismuth_bronze_mix",
    "bismuth_ingot_from_raw_native_bismuth", "brass_ingot_from_brass_mix", "copper_ingot_from_copper_mix",
    "copper_ingot_from_raw_malachite", "copper_ingot_from_raw_native_copper", "copper_ingot_from_raw_tetrahedrite",
    "gold_ingot_from_raw_native_gold", "iron_ingot_from_raw_hematite", "iron_ingot_from_raw_limonite",
    "iron_ingot_from_raw_magnetite", "lead_ingot_from_raw_galena", "lithium_ingot_from_raw_lepidolite",
    "nickel_ingot_from_raw_pentlandite", "tin_bronze_ingot_from_tin_bronze_mix", "tin_ingot_from_raw_cassiterite",
    "tin_ingot_from_tin_mix", "zinc_ingot_from_raw_sphalerite", "zinc_ingot_from_zinc_mix",
]

STRAW_TOOLS = [f"{NS}:{m}_knife" for m in ("flint", "bone", "amethyst", "copper", "bronze", "iron", "steel")] + \
              [f"{NS}:{m}_{t}" for t in ("sickle", "scythe") for m in ("copper", "bronze", "iron", "steel")]
STRAW_GRASS = [f"{NS}:grass_small", f"{NS}:grass_medium", f"{NS}:grass_tall", "minecraft:short_grass",
               "minecraft:tall_grass"]

LANG = {
    "item.trailcraft.straw": ("Солома", "Straw"),
    "item.trailcraft.reinforced_brick": ("Укреплённый кирпич", "Reinforced Brick"),
    "item.trailcraft.clay_mold": ("Слепок из глины", "Clay Mold"),
    "item.trailcraft.unfired_jug": ("Сырой кувшин", "Unfired Jug"),
    "item.trailcraft.jug": ("Кувшин", "Jug"),
    "block.trailcraft.small_bloomery": ("Малая сыродутная печь", "Small Bloomery"),
    "tooltip.trailcraft.clay_mold": ("ПКМ — выбрать, что вылепить", "Right-click to choose what to shape"),
    "tooltip.trailcraft.jug.result": ("Выплавится: %s × %s", "Will smelt: %s × %s"),
    "gui.trailcraft.clay_mold": ("Слепок из глины", "Clay Mold"),
    "gui.trailcraft.clay_mold.hint": ("Выберите форму", "Choose a shape"),
    "item.trailcraft.cloth_blank": ("Тканевая заготовка", "Cloth Blank"),
    "jei.trailcraft.small_bloomery": ("Малая сыродутная печь", "Small Bloomery"),
    "jei.trailcraft.handwork": ("Ручная работа", "Handwork"),
    "jei.trailcraft.bloomery.one": ("В кувшине, в печи с углём: 1 предмет — 1 слиток", "In a jug, in a bloomery with coal: 1 item = 1 ingot"),
    "jei.trailcraft.bloomery.alloy": ("Сплав прямо в кувшине: доли металлов как у сплава", "Alloy right in the jug: metal shares as in the alloy"),
    "jei.trailcraft.bloomery.mix": ("Шихта сплава плавится сразу в сплав", "Alloy mix melts straight into the alloy"),
    "jei.trailcraft.branch": ("ПКМ веткой по породе %s раз", "Right-click a rock with a branch %s times"),
    "jei.trailcraft.cloth": ("Заготовка и палка в руках, зажать ПКМ; палка вернётся с шансом %s %%", "Blank and stick in hands, hold right-click; the stick returns with a %s %% chance"),
    "jei.trailcraft.clay_mold": ("ПКМ слепком — выбрать форму", "Right-click the mold to choose a shape"),
    "jei.trailcraft.straw": ("Срезать траву ножом, серпом или косой", "Cut grass with a knife, sickle or scythe"),
    "jei.trailcraft.leaves_branch": ("С листвы падает ветка: 5 %, ножом 15 %", "Leaves drop a branch: 5 %, with a knife 15 %"),
    "jei.trailcraft.chopping": ("Бревно на пне — удар топором: %s тонких бревна", "Log on a chopping stump, hit with an axe: %s thin logs"),
    "jei.trailcraft.campfire": ("Бросить на землю (брёвен — %s–%s), зажать ПКМ огнивом",
                                "Drop on the ground (%s–%s logs), hold right-click with a firestarter"),
    "item.trailcraft.cloth": ("Тканевое полотно", "Cloth"),
    "tooltip.trailcraft.cloth_blank": ("В другой руке обработанная палка — зажать ПКМ", "Hold right-click with a processed stick in the other hand"),
}


def path(*parts):
    return os.path.join(ROOT, *parts)


def write(rel, data):
    p = path(*rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(rel, default=None):
    p = path(*rel.split("/"))
    if not os.path.exists(p):
        return default
    with open(p, encoding="utf-8") as f:
        return json.load(f)


def tag(kind, tag_id, values):
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write(rel, data)


# ---------- текстуры ----------

def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def png(rel, im):
    p = path("assets", NS, "textures", *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p + ".png")


def brick_wall(color, rnd, mortar=(196, 190, 178), specks=None):
    """Кладка 16×16: ряды по 4 пикселя, кирпич 8×3, швы в 1 пиксель со сдвигом через ряд."""
    im = Image.new("RGBA", (16, 16), mortar + (255,))
    for row in range(4):
        y0 = row * 4
        off = 0 if row % 2 == 0 else 4
        for b in range(-1, 3):
            x0 = b * 8 + off
            k = rnd.uniform(0.86, 1.08)
            for y in range(y0, y0 + 3):
                for x in range(x0, x0 + 7):
                    if 0 <= x < 16:
                        n = k * rnd.uniform(0.94, 1.05)
                        if y == y0:
                            n *= 1.08
                        if y == y0 + 2:
                            n *= 0.88
                        im.putpixel((x, y), shade(color, n) + (255,))
                        if specks and rnd.random() < 0.18:
                            im.putpixel((x, y), shade(specks, rnd.uniform(0.85, 1.1)) + (255,))
    return im


# Кирпич в руке — по форме ванильного кирпича (minecraft:item/brick): буквы — оттенки, цвет берётся от глины
BRICK_SHAPE = [
    "................",
    "................",
    "..........aa....",
    "......aaaadms...",
    "...aaadmmmmmms..",
    "aaadmmmmmmmmmms.",
    "hmmmmmmmmmmmhhds",
    "dhmmmmmmmhhhdsso",
    "ddhmmhhhhdssssso",
    "dddhhdsssssssseo",
    "sddmsssssssseoo.",
    "asdmssssseooo...",
    ".asmseoooo......",
    "..aaoo..........",
    "................",
    "................",
]
# яркость оттенков ванильного кирпича относительно основного (m = #B5593F)
BRICK_SHADES = {"h": 1.09, "m": 1.0, "d": 0.77, "s": 0.69, "e": 0.53, "a": 0.40, "o": 0.24}

# Слепок из глины — по форме ванильного зелёного красителя (minecraft:item/green_dye)
MOLD_SHAPE = [
    "................",
    "..........o.....",
    ".........omoo...",
    "......okoldomk..",
    "....omolmolmk...",
    "...ollmoolmdk...",
    "..ollmomllmomk..",
    ".odlmomllmomk...",
    ".omoollldomlk...",
    ".oodmlmomlldk...",
    "odmmmmomllmk....",
    ".kkkkkmmdkk.....",
    ".....kkkk.......",
    "................",
    "................",
    "................",
]
# яркость оттенков красителя относительно основного (m = #496A18)
MOLD_SHADES = {"l": 1.32, "m": 1.0, "d": 0.75, "o": 0.62, "k": 0.48}

# Цвета соломы — как у метлы (textures/item/broom.png)
STRAW_LIGHT, STRAW_MID, STRAW_DARK, STRAW_EDGE = (0xE8, 0xDC, 0x96), (0xD2, 0xC6, 0x76), (0x96, 0x8A, 0x3E), (0x60, 0x54, 0x20)
STRAW_TIE, STRAW_TIE_DARK = (0x5C, 0x46, 0x28), (0x3C, 0x2C, 0x18)


def from_shape(shape, shades, color, specks=None, seed=0):
    """Рисует по шаблону из букв: каждая буква — оттенок основного цвета; specks — вкрапления (солома в самане)."""
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(shape):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            c = shade(color, shades[ch])
            if specks and ch in "hmds" and rnd.random() < 0.22:
                c = shade(specks, shades[ch] * rnd.uniform(0.9, 1.05))
            im.putpixel((x, y), c + (255,))
    return im


def brick_item(color, specks=None, seed=0):
    return from_shape(BRICK_SHAPE, BRICK_SHADES, color, specks, seed)


def straw_item():
    """Сноп соломы цветов метлы: по диагонали, перетянут посередине, распушён к концам."""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(16):
        for x in range(16):
            u, v = x - y, x + y - 15  # u — вдоль снопа, v — поперёк
            a = abs(u)
            if a > 13:
                continue
            width = 2.5 if a <= 2 else (3.5 if a <= 7 else 4.5)
            if abs(v) > width:
                continue
            if a <= 1:
                c = STRAW_TIE if a == 0 else STRAW_TIE_DARK
            else:
                c = [STRAW_LIGHT, STRAW_MID, STRAW_DARK, STRAW_MID][v % 4]
                if abs(v) >= width - 0.6:
                    c = STRAW_EDGE if a < 10 else STRAW_DARK
                if a >= 12:
                    c = STRAW_MID if v % 2 == 0 else STRAW_DARK
            im.putpixel((x, y), c + (255,))
    return im


def lump(color, seed):
    return from_shape(MOLD_SHAPE, MOLD_SHADES, color, seed=seed)


def jug(color, seed):
    """Кувшин: горлышко, ручка справа, пузатое тело."""
    rnd = random.Random(seed)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    widths = {2: 3, 3: 2, 4: 2, 5: 3, 6: 4, 7: 5, 8: 5, 9: 5, 10: 5, 11: 5, 12: 4, 13: 3, 14: 3}
    for y, half in widths.items():
        for x in range(8 - half, 8 + half):
            light = 1.18 if x < 8 - half + 2 else (0.78 if x >= 8 + half - 1 else 1.0)
            im.putpixel((x, y), shade(color, light * rnd.uniform(0.94, 1.05)) + (255,))
    for x in range(6, 10):
        im.putpixel((x, 2), shade(color, 0.6) + (255,))  # тёмное горло
    for y, x in ((4, 10), (4, 11), (5, 12), (6, 12), (7, 12), (8, 11)):  # ручка
        im.putpixel((x, y), shade(color, 0.85) + (255,))
    return im


def bloomery_faces():
    rnd = random.Random(11)
    side = brick_wall(ADOBE, rnd, mortar=(118, 98, 72), specks=STRAW)
    top = side.copy()
    for y in range(4, 12):  # дымоход
        for x in range(4, 12):
            top.putpixel((x, y), (30, 24, 20, 255) if 5 <= x <= 10 and 5 <= y <= 10 else (70, 56, 44, 255))
    front = side.copy()
    front_on = side.copy()
    for y in range(7, 15):  # устье
        for x in range(4, 12):
            edge = x in (4, 11) or y == 7
            front.putpixel((x, y), (60, 48, 38, 255) if edge else (22, 18, 16, 255))
            if edge:
                front_on.putpixel((x, y), (60, 48, 38, 255))
            else:
                glow = (255, 190, 60) if y >= 12 else (232, 110, 28)
                front_on.putpixel((x, y), shade(glow, rnd.uniform(0.85, 1.05)) + (255,))
    return side, top, front, front_on


JUTE = (191, 163, 102)       # тканевая заготовка и полотно — цвет джута
STICK = (120, 86, 48)


def cloth_blank():
    """Тканевая заготовка: обработанная палка по диагонали, посередине намотан толстый слой джута."""
    rnd = random.Random(21)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for t in range(2, 15):  # палка
        im.putpixel((t, 15 - t), shade(STICK, 1.1 if t % 3 else 0.85) + (255,))
    for y in range(16):
        for x in range(16):
            u, v = x - (15 - y), x + (15 - y) - 15  # v — вдоль палки, u — поперёк
            if abs(v) <= 4 and abs(u) <= 3 - (abs(v) == 4):
                k = (1.15 if u < 0 else 0.8 if u > 1 else 1.0) * (0.88 if (v + u) % 3 == 0 else 1.0)
                im.putpixel((x, y), shade(JUTE, k * rnd.uniform(0.95, 1.04)) + (255,))
    return im


def cloth():
    """Тканевое полотно: сложенный кусок рогожи с переплетением нитей, нижний край загнут."""
    rnd = random.Random(22)
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y in range(3, 14):
        for x in range(2, 14):
            k = 1.08 if (x + y) % 2 == 0 else 0.9          # переплетение
            if y == 3 or x == 2:
                k *= 1.12
            if y == 13 or x == 13:
                k *= 0.72
            if y >= 11:
                k *= 0.85                                    # загнутый край
            im.putpixel((x, y), shade(JUTE, k * rnd.uniform(0.96, 1.03)) + (255,))
    for x in range(2, 14):
        im.putpixel((x, 11), shade(JUTE, 0.62) + (255,))
    for x, y in ((14, 5), (14, 8), (1, 7)):                  # торчащие нитки
        im.putpixel((x, y), shade(JUTE, 0.9) + (255,))
    return im


def textures():
    for clay, (color, *_rest) in CLAYS.items():
        png(f"block/bricks_{clay}", brick_wall(color, random.Random(sum(map(ord, clay)))))
        png(f"item/brick_{clay}", brick_item(color, seed=len(clay)))
    png("item/reinforced_brick", brick_item(ADOBE, specks=STRAW, seed=3))
    png("item/straw", straw_item())
    png("item/clay_mold", lump(MOLD_CLAY, 1))
    png("item/unfired_jug", jug(RAW_CLAY, 2))
    png("item/jug", jug(FIRED_JUG, 2))
    png("item/cloth_blank", cloth_blank())
    png("item/cloth", cloth())
    side, top, front, front_on = bloomery_faces()
    png("block/small_bloomery_side", side)
    png("block/small_bloomery_top", top)
    png("block/small_bloomery_front", front)
    png("block/small_bloomery_front_on", front_on)


# ---------- JSON ----------

def item_model(name, parent="minecraft:item/generated"):
    write(f"assets/{NS}/models/item/{name}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{name}"}})


def simple_loot(name):
    write(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{name}"})


def assets():
    for clay in CLAYS:
        item_model(f"brick_{clay}")
        write(f"assets/{NS}/models/block/bricks_{clay}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/bricks_{clay}"}})
        write(f"assets/{NS}/blockstates/bricks_{clay}.json", {"variants": {"": {"model": f"{NS}:block/bricks_{clay}"}}})
        write(f"assets/{NS}/models/item/bricks_{clay}.json", {"parent": f"{NS}:block/bricks_{clay}"})
        simple_loot(f"bricks_{clay}")
    for name in ("straw", "reinforced_brick", "clay_mold", "unfired_jug", "jug", "cloth_blank", "cloth"):
        item_model(name)
    for lit in ("", "_on"):
        write(f"assets/{NS}/models/block/small_bloomery{lit}.json", {
            "parent": "minecraft:block/orientable", "textures": {
                "top": f"{NS}:block/small_bloomery_top", "front": f"{NS}:block/small_bloomery_front{lit}",
                "side": f"{NS}:block/small_bloomery_side"}})
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    variants = {}
    for facing, y in rot.items():
        for lit in (False, True):
            v = {"model": f"{NS}:block/small_bloomery{'_on' if lit else ''}"}
            if y:
                v["y"] = y
            variants[f"facing={facing},lit={str(lit).lower()}"] = v
    write(f"assets/{NS}/blockstates/small_bloomery.json", {"variants": variants})
    write(f"assets/{NS}/models/item/small_bloomery.json", {"parent": f"{NS}:block/small_bloomery"})
    simple_loot("small_bloomery")


# Рюкзак из крафта — сразу с одним карманом своего тира в первом слоте под карманы (BackpackItem.POCKETS = 61)
BACKPACK_POCKETS = {"duffel_backpack": "small_pocket", "leather_backpack": "medium_pocket",
                    "tactical_backpack": "large_pocket"}
POCKETS_SLOT = 61


def backpack_result(backpack):
    return {"id": f"{NS}:{backpack}", "count": 1, "components": {f"{NS}:contents": [
        {"slot": POCKETS_SLOT, "item": {"id": f"{NS}:{BACKPACK_POCKETS[backpack]}", "count": 1}}]}}


def data():
    clay_balls = [f"{NS}:clay_ball_{c}" for c in CLAYS]
    tag("item", f"{NS}:clay_balls", clay_balls)
    tag("item", f"{NS}:straw_tools", STRAW_TOOLS)
    # топливо малой сыродутной печи (BloomeryBlockEntity): уголь и торф (торф горит вдвое меньше)
    tag("item", f"{NS}:bloomery_fuels", ["#minecraft:coals", f"{NS}:peat"])
    tag("block", f"{NS}:straw_grass", STRAW_GRASS)
    tag("block", "minecraft:mineable/pickaxe", [f"{NS}:bricks_{c}" for c in CLAYS] + [f"{NS}:small_bloomery"])
    for clay in CLAYS:
        write(f"data/{NS}/recipe/campfire/brick_{clay}.json", {
            "type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 400, "experience": 0.1,
            "ingredient": {"item": f"{NS}:clay_ball_{clay}"}, "result": {"id": f"{NS}:brick_{clay}", "count": 1}})
        write(f"data/{NS}/recipe/bricks_{clay}.json", {
            "type": "minecraft:crafting_shaped", "category": "building", "key": {"#": {"item": f"{NS}:brick_{clay}"}},
            "pattern": ["##", "##"], "result": {"id": f"{NS}:bricks_{clay}", "count": 1}})
    # Всё до первой меди — в сетке 2×2 (верстак нужен доскам, а доскам — пила из меди)
    write(f"data/{NS}/recipe/reinforced_brick.json", {
        "type": "minecraft:crafting_shapeless", "category": "building",
        "ingredients": [{"tag": f"{NS}:clay_balls"}, {"item": f"{NS}:straw"}],
        "result": {"id": f"{NS}:reinforced_brick", "count": 1}})
    write(f"data/{NS}/recipe/small_bloomery.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "key": {"#": {"item": f"{NS}:reinforced_brick"}},
        "pattern": ["##", "##"], "result": {"id": f"{NS}:small_bloomery", "count": 1}})
    write(f"data/{NS}/recipe/clay_mold.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": f"{NS}:clay_balls"}] * 3, "result": {"id": f"{NS}:clay_mold", "count": 1}})
    write(f"data/{NS}/recipe/campfire/jug.json", {
        "type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 600, "experience": 0.3,
        "ingredient": {"item": f"{NS}:unfired_jug"}, "result": {"id": f"{NS}:jug", "count": 1}})
    # Ткань: заготовка — 3 джута и обработанная палка; полотно ткут в руках (item/ClothBlankItem);
    # армейский вещмешок — верёвка и 3 полотна
    write(f"data/{NS}/recipe/cloth_blank.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{NS}:jute"}] * 3 + [{"item": f"{NS}:stick_processed"}],
        "result": {"id": f"{NS}:cloth_blank", "count": 1}})
    write(f"data/{NS}/recipe/duffel_backpack.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [{"item": f"{NS}:rope"}] + [{"item": f"{NS}:cloth"}] * 3,
        "result": backpack_result("duffel_backpack")})
    # Кремнёвый нож — срезать траву на солому: кремень над обработанной палкой
    write(f"data/{NS}/recipe/flint_knife.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"F": {"item": f"{NS}:flint"}, "P": {"item": f"{NS}:stick_processed"}}, "pattern": ["F", "P"],
        "result": {"id": f"{NS}:flint_knife", "count": 1}})
    for name in CAMPFIRE_METALS:
        p = path("data", NS, "recipe", "campfire", f"{name}.json")
        if os.path.exists(p):
            os.remove(p)


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for clay, (_c, ru_item, ru_block, en) in CLAYS.items():
            data[f"item.{NS}.brick_{clay}"] = (ru_item, f"{en} Brick")[k]
            data[f"block.{NS}.bricks_{clay}"] = (ru_block, f"{en} Bricks")[k]
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


if __name__ == "__main__":
    textures()
    assets()
    data()
    lang()
    print("ok")

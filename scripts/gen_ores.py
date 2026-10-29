"""
Генерирует JSON для руд, слитков, огнива и костра: модели, блокстейты, лут, тэги, рецепты, генерацию в мире, переводы.
Таблица руд та же, что в src/main/java/.../ore/Ore.java — меняя руду, поменяйте её в обоих местах.

Запуск из корня проекта:  python scripts/gen_ores.py
Скрипт перезаписывает только свои файлы; в общие тэги и переводы добавляет свои строки, не трогая чужие.
Текстуры (оверлеи руд textures/block/ore/<руда>/<порода>_<n>.png и иконки предметов) он не рисует.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
VARIANTS = 3  # текстур на пару «руда — порода»

# id, элемент, кирка (any/stone/iron), кремнёвая кирка берёт?, что падает, сколько, породы,
# генерация: попыток на чанк, размер жилы (у всех, кроме угля, — вдвое меньше прежнего), высота от и до;
# названия ru / en (блок, то, что падает)
ORES = [
    ("bituminous_coal", "coal", "any", True, "bituminous_coal", 2, 4, ["sandstone", "limestone"], 10, 14, 0, 200,
     "Залежь каменного угля", "Bituminous Coal Deposit", "Каменный уголь", "Bituminous Coal"),
    ("malachite", "copper", "stone", True, "raw_malachite", 2, 4, ["limestone", "dolomite"], 8, 5, -10, 160,
     "Малахит", "Malachite", "Рудный малахит", "Raw Malachite"),
    ("native_copper", "copper", "stone", True, "raw_native_copper", 1, 1, ["basalt", "gabbro"], 6, 4, -64, 64,
     "Самородная медь", "Native Copper", "Рудная самородная медь", "Raw Native Copper"),
    ("tetrahedrite", "copper", "stone", True, "raw_tetrahedrite", 2, 4, ["andesite", "granite"], 6, 4, -64, 120,
     "Тетраэдрит", "Tetrahedrite", "Рудный тетраэдрит", "Raw Tetrahedrite"),
    ("hematite", "iron", "stone", False, "raw_hematite", 1, 1, ["sandstone", "shale"], 8, 5, 20, 200,
     "Гематит", "Hematite", "Рудный гематит", "Raw Hematite"),
    ("magnetite", "iron", "stone", False, "raw_magnetite", 1, 1, ["diorite", "marble"], 6, 5, -64, 120,
     "Магнетит", "Magnetite", "Рудный магнетит", "Raw Magnetite"),
    ("limonite", "iron", "any", False, "raw_limonite", 2, 4, ["limestone", "chalk"], 8, 5, 40, 200,
     "Лимонит", "Limonite", "Рудный лимонит", "Raw Limonite"),
    ("sphalerite", "zinc", "stone", False, "raw_sphalerite", 2, 4, ["dolomite", "limestone"], 6, 4, -10, 120,
     "Сфалерит", "Sphalerite", "Рудный сфалерит", "Raw Sphalerite"),
    ("cassiterite", "tin", "stone", False, "raw_cassiterite", 2, 4, ["gray_granite", "granite"], 4, 4, -64, 80,
     "Касситерит", "Cassiterite", "Рудный касситерит", "Raw Cassiterite"),
    ("native_bismuth", "bismuth", "stone", False, "raw_native_bismuth", 1, 1, ["granite", "marble"], 4, 3, -64, 100,
     "Самородный висмут", "Native Bismuth", "Рудный самородный висмут", "Raw Native Bismuth"),
    ("galena", "lead", "stone", False, "raw_galena", 2, 4, ["limestone", "marble"], 5, 4, -10, 140,
     "Галенит", "Galena", "Рудный галенит", "Raw Galena"),
    ("native_gold", "gold", "iron", False, "raw_native_gold", 1, 1, ["gray_granite", "andesite"], 3, 3, -64, 40,
     "Самородное золото", "Native Gold", "Рудное самородное золото", "Raw Native Gold"),
    ("pentlandite", "nickel", "iron", False, "raw_pentlandite", 1, 1, ["gabbro", "basalt"], 4, 3, -64, 20,
     "Пентландит", "Pentlandite", "Рудный пентландит", "Raw Pentlandite"),
    ("lepidolite", "lithium", "stone", False, "raw_lepidolite", 1, 1, ["granite", "gray_granite"], 3, 3, -64, 60,
     "Лепидолит", "Lepidolite", "Рудный лепидолит", "Raw Lepidolite"),
    ("bauxite", "aluminum", "any", False, "raw_bauxite", 2, 4, ["limestone", "basalt"], 5, 5, 30, 200,
     "Боксит", "Bauxite", "Рудный боксит", "Raw Bauxite"),
    ("nepheline", "aluminum", "stone", False, "raw_nepheline", 2, 4, ["diorite", "gray_granite"], 4, 4, -64, 80,
     "Нефелин", "Nepheline", "Рудный нефелин", "Raw Nepheline"),
    ("apatite", "apatite", "stone", True, "apatite", 2, 4, ["gabbro", "marble"], 5, 4, -40, 120,
     "Апатитовая руда", "Apatite Ore", "Апатит", "Apatite"),
    # киноварь — редстоун (кусок + каменный молоток — 2 редстоуна, жёрнов — 3); кварц — ванильный предмет
    ("cinnabar", "redstone", "stone", False, "raw_cinnabar", 1, 2, ["shale", "basalt"], 5, 4, -64, 60,
     "Киноварь", "Cinnabar", "Рудная киноварь", "Raw Cinnabar"),
    ("quartz", "quartz", "stone", False, "minecraft:quartz", 1, 2, ["granite", "gray_granite"], 6, 4, -40, 120,
     "Кварцевая жила", "Quartz Vein", None, None),
]

# Во что плавится то, что падает с руды (справочник; плавка — в кувшине, smelting/Smelting.java)
SMELT = {
    "raw_malachite": "minecraft:copper_ingot", "raw_native_copper": "minecraft:copper_ingot",
    "raw_tetrahedrite": "minecraft:copper_ingot",
    "raw_hematite": "minecraft:iron_ingot", "raw_magnetite": "minecraft:iron_ingot", "raw_limonite": "minecraft:iron_ingot",
    "raw_sphalerite": "create:zinc_ingot", "raw_cassiterite": f"{NS}:tin_ingot", "raw_native_bismuth": f"{NS}:bismuth_ingot",
    "raw_galena": f"{NS}:lead_ingot", "raw_native_gold": "minecraft:gold_ingot", "raw_pentlandite": f"{NS}:nickel_ingot",
    "raw_lepidolite": f"{NS}:lithium_ingot", "raw_bauxite": f"{NS}:aluminum_ingot", "raw_nepheline": f"{NS}:aluminum_ingot",
    "tin_bronze_mix": f"{NS}:tin_bronze_ingot", "bismuth_bronze_mix": f"{NS}:bismuth_bronze_ingot",
    "brass_mix": "create:brass_ingot",
    "copper_mix": "minecraft:copper_ingot", "tin_mix": f"{NS}:tin_ingot", "zinc_mix": "create:zinc_ingot",
}

# Самородки на поверхности над большой жилой (worldgen/NuggetDepositFeature): руда, во что дробятся молотком,
# названия ru / en. Блок и предмет — <руда>_nugget; одна фича каждой руды на 4 сектора 9×9 чанков — чанк в каждой клетке
# 18×18 чанков выбирает сама фича (NuggetDepositFeature), поэтому места распределены равномерно.
NUGGETS = [
    ("native_copper", "copper_mix", "Самородная медь", "Native Copper Nugget"),
    ("malachite", "copper_mix", "Малахит", "Malachite Nugget"),
    ("tetrahedrite", "copper_mix", "Тетраэдрит", "Tetrahedrite Nugget"),
    ("cassiterite", "tin_mix", "Касситерит", "Cassiterite Nugget"),
    ("sphalerite", "zinc_mix", "Сфалерит", "Sphalerite Nugget"),
]

# Свои слитки: id, тэг c:ingots/..., ru, en
INGOTS = [
    ("tin_ingot", "tin", "Оловянный слиток", "Tin Ingot"),
    ("lead_ingot", "lead", "Свинцовый слиток", "Lead Ingot"),
    ("bismuth_ingot", "bismuth", "Висмутовый слиток", "Bismuth Ingot"),
    ("nickel_ingot", "nickel", "Никелевый слиток", "Nickel Ingot"),
    ("lithium_ingot", "lithium", "Литиевый слиток", "Lithium Ingot"),
    ("aluminum_ingot", "aluminum", "Алюминиевый слиток", "Aluminum Ingot"),
    ("tin_bronze_ingot", "bronze", "Слиток оловянной бронзы", "Tin Bronze Ingot"),
    ("bismuth_bronze_ingot", "bronze", "Слиток висмутовой бронзы", "Bismuth Bronze Ingot"),
]
OTHER_ITEMS = [
    ("tin_bronze_mix", "Шихта оловянной бронзы", "Tin Bronze Mix"),
    ("bismuth_bronze_mix", "Шихта висмутовой бронзы", "Bismuth Bronze Mix"),
    ("brass_mix", "Шихта латуни", "Brass Mix"),
    ("copper_mix", "Шихта чистой меди", "Pure Copper Mix"),
    ("tin_mix", "Шихта чистого олова", "Pure Tin Mix"),
    ("zinc_mix", "Шихта чистого цинка", "Pure Zinc Mix"),
    ("charcoal", "Древесный уголь", "Charcoal"),
    ("firestarter", "Огниво", "Firestarter"),
    ("stone_hammer", "Каменный молоток", "Stone Hammer"),
]
# Твёрдые камешки — из них молоток
HARD_PEBBLES = [f"{NS}:{r}_pebble" for r in ["andesite", "basalt", "diorite", "gabbro", "granite", "gray_granite"]]
GRAVELS = [f"{NS}:{r}_gravel" for r in ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite",
                                         "gray_granite", "limestone", "marble", "sandstone", "shale"]]
WOODS = ["oak", "birch", "acacia", "sakura", "eucalyptus", "chestnut", "thuja", "pine", "fir", "spruce", "white_poplar",
         "ebony", "maple", "cedar", "willow", "lilac", "palm", "ironwood", "aspen", "linden", "pussy_willow", "walnut",
         "alder"]

LANG = {
    "itemGroup.trailcraft_ores": ("TrailCraft: Руды", "TrailCraft: Ores"),
    "tooltip.trailcraft.ore.rock": ("Порода: %s", "Rock: %s"),
    "tooltip.trailcraft.firestarter": ("Бросьте на землю тонкие брёвна (%s–%s) и ветки (%s), наведитесь на них и зажмите ПКМ",
                                       "Drop thin logs (%s–%s) and branches (%s) on the ground, aim at them and hold RMB"),
    "message.trailcraft.firestarter.need_fuel": ("Для костра на земле нужны тонкие брёвна (%s–%s) и ветки (%s)",
                                                 "A campfire needs thin logs (%s–%s) and branches (%s) on the ground"),
    "message.trailcraft.firestarter.no_place": ("Здесь костёр не разжечь", "Can't light a campfire here"),
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


def tag(kind, tag_id, add=(), remove=()):
    """Добавляет (и убирает) значения в тэге, сохраняя остальные."""
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    values = [v for v in data["values"] if v not in remove]
    for v in add:
        if v not in values:
            values.append(v)
    data["values"] = values
    write(rel, data)


# Инструменты держатся в руке как ванильные
HANDHELD = {"stone_hammer"}


def item_model(item_id):
    parent = "minecraft:item/handheld" if item_id in HANDHELD else "minecraft:item/generated"
    write(f"assets/{NS}/models/item/{item_id}.json", {"parent": parent, "textures": {"layer0": f"{NS}:item/{item_id}"}})


def ore_models():
    write(f"assets/{NS}/models/block/ore_layered.json", {
        "parent": "minecraft:block/block",
        "render_type": "minecraft:cutout_mipped",
        "textures": {"particle": "#rock"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 16, 16],
             "faces": {d: {"texture": "#" + layer, "cullface": d} for d in ["down", "up", "north", "south", "west", "east"]}}
            for layer in ("rock", "ore")
        ],
    })
    for o in ORES:
        ore, rocks = o[0], o[7]
        variants = {}
        for rock in rocks:
            models = []
            for n in range(1, VARIANTS + 1):
                model = f"{NS}:block/ore/{ore}/{rock}_{n}"
                write(f"assets/{NS}/models/block/ore/{ore}/{rock}_{n}.json", {
                    "parent": f"{NS}:block/ore_layered",
                    "textures": {"rock": f"{NS}:block/{rock}", "ore": f"{NS}:block/ore/{ore}/{rock}_{n}"},
                })
                models.append({"model": model})
            variants[f"rock={rock}"] = models
        write(f"assets/{NS}/blockstates/{ore}_ore.json", {"variants": variants})
        # Предмет: порода по свойству trailcraft:rock (номер породы × 0,1, см. ClientOres)
        write(f"assets/{NS}/models/item/{ore}_ore.json", {
            "parent": f"{NS}:block/ore/{ore}/{rocks[0]}_1",
            "overrides": [{"predicate": {f"{NS}:rock": round(i * 0.1, 1)}, "model": f"{NS}:block/ore/{ore}/{rock}_1"}
                          for i, rock in enumerate(rocks) if i > 0],
        })
        if ":" not in o[4]:  # ванильный предмет — своя модель не нужна
            item_model(o[4])


SILK_TOUCH = {"condition": "minecraft:match_tool", "predicate": {"predicates": {"minecraft:enchantments": [
    {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}


def loot():
    for o in ORES:
        ore, drop, lo, hi = o[0], o[4], o[5], o[6]
        block = f"{NS}:{ore}_ore"
        functions = []
        if (lo, hi) != (1, 1):
            functions.append({"function": "minecraft:set_count", "add": False,
                              "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        functions += [{"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
                      {"function": "minecraft:explosion_decay"}]
        write(f"data/{NS}/loot_table/blocks/{ore}_ore.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
                # Шёлковое касание: сам блок, порода сохраняется в предмете
                {"type": "minecraft:item", "name": block, "conditions": [SILK_TOUCH],
                 "functions": [{"function": "minecraft:copy_state", "block": block, "properties": ["rock"]}]},
                {"type": "minecraft:item", "name": drop if ":" in drop else f"{NS}:{drop}", "functions": functions},
            ]}]}],
            "random_sequence": f"{NS}:blocks/{ore}_ore",
        })
    # Костёр: с шёлковым касанием — сам костёр, иначе свой древесный уголь вместо ванильного (горящий — ничего:
    # уголь в нём догорает)
    write("data/minecraft/loot_table/blocks/campfire.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:alternatives", "children": [
            {"type": "minecraft:item", "name": "minecraft:campfire", "conditions": [SILK_TOUCH]},
            {"type": "minecraft:item", "name": f"{NS}:charcoal", "conditions": [
                {"condition": "minecraft:survives_explosion"},
                {"condition": "minecraft:block_state_property", "block": "minecraft:campfire",
                 "properties": {"lit": "false"}}]},
        ]}]}],
        "random_sequence": "minecraft:blocks/campfire",
    })


def tags():
    blocks = [f"{NS}:{o[0]}_ore" for o in ORES]
    tag("block", "minecraft:mineable/pickaxe", add=blocks + GRAVELS)
    tag("block", "minecraft:mineable/shovel", remove=GRAVELS)
    tag("block", "minecraft:needs_stone_tool", add=[f"{NS}:{o[0]}_ore" for o in ORES if o[2] == "stone"])
    tag("block", "minecraft:needs_iron_tool", add=[f"{NS}:{o[0]}_ore" for o in ORES if o[2] == "iron"])
    tag("block", f"{NS}:mineable/flint_pickaxe", add=GRAVELS + [f"{NS}:{o[0]}_ore" for o in ORES if o[3]])
    for kind in ("block", "item"):
        tag(kind, "c:ores", add=blocks)
        tag(kind, f"{NS}:ores", add=blocks)
        for o in ORES:
            tag(kind, f"c:ores/{o[1]}", add=[f"{NS}:{o[0]}_ore"])
    raws = [o for o in ORES if o[4].startswith("raw_")]
    tag("item", "c:raw_materials", add=[f"{NS}:{o[4]}" for o in raws])
    for o in raws:
        tag("item", f"c:raw_materials/{o[1]}", add=[f"{NS}:{o[4]}"])
    tag("item", "c:ingots", add=[f"{NS}:{i[0]}" for i in INGOTS])
    for i in INGOTS:
        tag("item", f"c:ingots/{i[1]}", add=[f"{NS}:{i[0]}"])
    # Цинк — слиток Create; запись необязательная, на случай если Create сам его в тэг не положил
    data = read("data/c/tags/item/ingots/zinc.json", {"replace": False, "values": []})
    if not any(isinstance(v, dict) and v.get("id") == "create:zinc_ingot" for v in data["values"]):
        data["values"].append({"id": "create:zinc_ingot", "required": False})
    write("data/c/tags/item/ingots/zinc.json", data)
    tag("item", "minecraft:coals", add=[f"{NS}:bituminous_coal", f"{NS}:charcoal"])
    tag("item", f"{NS}:thin_logs", add=[f"{NS}:thin_{w}_log" for w in WOODS])
    tag("item", f"{NS}:hard_pebbles", add=HARD_PEBBLES)


def recipes():
    # Металлы на костре не плавятся (только в кувшине в малой сыродутной печи — smelting/Smelting.java,
    # scripts/gen_smelting.py убирает старые рецепты костра); SMELT — справочник, во что что плавится
    write(f"data/{NS}/recipe/campfire/charcoal_from_thin_logs.json", {
        "type": "minecraft:campfire_cooking", "category": "misc", "cookingtime": 600, "experience": 0.15,
        "ingredient": {"tag": f"{NS}:thin_logs"}, "result": {"id": f"{NS}:charcoal", "count": 1},
    })
    # Шихта бронз: куски руды, раздробленные молотком (молоток остаётся, теряет прочность — HammerItem)
    hammer = {"item": f"{NS}:stone_hammer"}
    write(f"data/{NS}/recipe/tin_bronze_mix.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "c:raw_materials/copper"}] * 3 + [{"tag": "c:raw_materials/tin"}, hammer],
        "result": {"id": f"{NS}:tin_bronze_mix", "count": 4},
    })
    write(f"data/{NS}/recipe/bismuth_bronze_mix.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "c:raw_materials/copper"}] * 2
                       + [{"tag": "c:raw_materials/bismuth"}, {"tag": "c:raw_materials/zinc"}, hammer],
        "result": {"id": f"{NS}:bismuth_bronze_mix", "count": 4},
    })
    write(f"data/{NS}/recipe/brass_mix.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": "c:raw_materials/copper"}] * 3 + [{"tag": "c:raw_materials/zinc"}, hammer],
        "result": {"id": f"{NS}:brass_mix", "count": 4},
    })
    # Киноварь, раздробленная молотком, — 2 редстоуна (жёрнов и дробилка Create дают больше — gen_create_compat.py)
    write(f"data/{NS}/recipe/redstone_from_cinnabar.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"item": f"{NS}:raw_cinnabar"}, hammer],
        "result": {"id": "minecraft:redstone", "count": 2},
    })
    # Самородки: 3 штуки и молоток — 1 чистая шихта; чистая шихта смешивается в сплавы без молотка
    for ore, mix, _, _ in NUGGETS:
        write(f"data/{NS}/recipe/{mix}_from_{ore}_nugget.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"item": f"{NS}:{ore}_nugget"}] * 3 + [hammer],
            "result": {"id": f"{NS}:{mix}", "count": 1},
        })
    for alloy, second in (("tin_bronze_mix", "tin_mix"), ("brass_mix", "zinc_mix")):
        write(f"data/{NS}/recipe/{alloy}_from_pure_mix.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"item": f"{NS}:copper_mix"}] * 3 + [{"item": f"{NS}:{second}"}],
            "result": {"id": f"{NS}:{alloy}", "count": 4},
        })
    write(f"data/{NS}/recipe/stone_hammer.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"P": {"tag": f"{NS}:hard_pebbles"}, "S": {"item": f"{NS}:stick_processed"}}, "pattern": ["P", "S"],
        "result": {"id": f"{NS}:stone_hammer", "count": 1},
    })
    # Две обработанные палки по диагонали — длинная палка (recipe/long_stick.json), поэтому в огниве вторая — ветка
    write(f"data/{NS}/recipe/firestarter.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment",
        "key": {"S": {"item": f"{NS}:stick_processed"}, "B": {"item": f"{NS}:stick_branch"}}, "pattern": ["S ", " B"],
        "result": {"id": f"{NS}:firestarter", "count": 1},
    })
    # бронзовая кирка — из отлитой головки (рецепт пишет gen_casting.py)
    # Печи нет: металлы плавят в кувшине в малой сыродутной печи
    furnace = path("data", NS, "recipe", "bootstrap_furnace.json")
    if os.path.exists(furnace):
        os.remove(furnace)
    write("data/neoforge/data_maps/item/furnace_fuels.json", {"values": {
        f"{NS}:bituminous_coal": {"burn_time": 1600}, f"{NS}:charcoal": {"burn_time": 1600}}})


def worldgen():
    features = []
    for o in ORES:
        ore, rocks, count, size, lo, hi = o[0], o[7], o[8], o[9], o[10], o[11]
        write(f"data/{NS}/worldgen/configured_feature/ore_{ore}.json", {
            "type": "minecraft:ore",
            "config": {"size": size, "discard_chance_on_air_exposure": 0.0, "targets": [
                {"target": {"predicate_type": "minecraft:block_match", "block": f"{NS}:{rock}"},
                 "state": {"Name": f"{NS}:{ore}_ore", "Properties": {"rock": rock}}} for rock in rocks]},
        })
        write(f"data/{NS}/worldgen/placed_feature/ore_{ore}.json", {
            "feature": f"{NS}:ore_{ore}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                              "min_inclusive": {"absolute": lo},
                                                              "max_inclusive": {"absolute": hi}}},
                {"type": "minecraft:biome"},
            ],
        })
        features.append(f"{NS}:ore_{ore}")
    write(f"data/{NS}/neoforge/biome_modifier/ores.json", {
        "type": "neoforge:add_features", "biomes": f"#{NS}:trailworldgen_biomes",
        "features": features, "step": "underground_ores",
    })
    deposits = []
    for ore, _, _, _ in NUGGETS:
        write(f"data/{NS}/worldgen/configured_feature/nugget_deposit_{ore}.json", {
            "type": f"{NS}:nugget_deposit", "config": {"ore": ore}})
        write(f"data/{NS}/worldgen/placed_feature/nugget_deposit_{ore}.json", {
            "feature": f"{NS}:nugget_deposit_{ore}",
            "placement": [
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"},
                {"type": "minecraft:biome"},
            ],
        })
        deposits.append(f"{NS}:nugget_deposit_{ore}")
    write(f"data/{NS}/neoforge/biome_modifier/nugget_deposits.json", {
        "type": "neoforge:add_features", "biomes": f"#{NS}:trailworldgen_biomes",
        "features": deposits, "step": "underground_ores",
    })


def nuggets():
    """
    Самородок на земле: формы block/nugget_shape_<n> (пишет scripts/gen_nugget_models.py), у каждой руды — своя
    текстура; блокстейт случайно выбирает форму и поворот.
    """
    folder = path("assets", NS, "models", "block")
    shapes = sorted(int(f[len("nugget_shape_"):-5]) for f in os.listdir(folder)
                    if f.startswith("nugget_shape_") and f.endswith(".json"))
    for ore, _, _, _ in NUGGETS:
        block = f"{ore}_nugget"
        for v in "abcd":  # старые модели по образцу кремня
            old = path("assets", NS, "models", "block", f"{block}_{v}.json")
            if os.path.exists(old):
                os.remove(old)
        old = path("assets", NS, "models", "block", f"{block}.json")
        if os.path.exists(old):
            os.remove(old)
        for f in os.listdir(folder):
            if f.startswith(f"{block}_") and f.endswith(".json"):
                os.remove(os.path.join(folder, f))
        variants = []
        for n in shapes:
            write(f"assets/{NS}/models/block/{block}_{n}.json", {
                "parent": f"{NS}:block/nugget_shape_{n}",
                "textures": {"particle": f"{NS}:block/{block}", "all": f"{NS}:block/{block}"}})
            variants += [{"model": f"{NS}:block/{block}_{n}", **({"y": y} if y else {})} for y in (0, 90, 180, 270)]
        write(f"assets/{NS}/blockstates/{block}.json", {"variants": {"": variants}})
        item_model(block)
        write(f"data/{NS}/loot_table/blocks/{block}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{block}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{block}",
        })


def lang():
    entries = dict(LANG)
    for o in ORES:
        entries[f"block.{NS}.{o[0]}_ore"] = (o[12], o[13])
        if o[14]:
            entries[f"item.{NS}.{o[4]}"] = (o[14], o[15])
    for i in INGOTS:
        entries[f"item.{NS}.{i[0]}"] = (i[2], i[3])
    for i in OTHER_ITEMS:
        entries[f"item.{NS}.{i[0]}"] = (i[1], i[2])
    for ore, _, ru, en in NUGGETS:
        entries[f"block.{NS}.{ore}_nugget"] = (ru, en)
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for key, names in entries.items():
            data[key] = names[k]
        write(rel, data)


def items():
    for i in INGOTS:
        item_model(i[0])
    for i in OTHER_ITEMS:
        item_model(i[0])


if __name__ == "__main__":
    ore_models()
    items()
    nuggets()
    loot()
    tags()
    recipes()
    worldgen()
    lang()
    print("ok")

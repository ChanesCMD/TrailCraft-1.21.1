"""
Генерирует JSON для земледелия: тяпки, грядки, ягодные кусты и ягоды, дикие морковь и картофель, джут и верёвку —
модели, блокстейты, лут, тэги, рецепты, генерацию в мире и переводы.
Блоки и предметы те же, что в ModBlocks / ModItems / ModTools — меняя список, меняйте в обоих местах.

Запуск из корня проекта:  python scripts/gen_farm.py
Скрипт перезаписывает только свои файлы; в общие тэги и переводы добавляет свои строки, не трогая чужие.
Текстуры рисует scripts/gen_farm_textures.py.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"

# Тяпки: материал, чем крафтится (предмет или #тэг; None — рецепта нет), узор, названия ru / en
HOES = [
    ("flint", f"{NS}:flint", ["MM", " S"], "Кремнёвая тяпка", "Flint Garden Hoe"),
    ("bone", "minecraft:bone", ["MM", " S"], "Костяная тяпка", "Bone Garden Hoe"),
    ("copper", "minecraft:copper_ingot", ["MM", " S", " S"], "Медная тяпка", "Copper Garden Hoe"),
    ("bronze", "#c:ingots/bronze", ["MM", " S", " S"], "Бронзовая тяпка", "Bronze Garden Hoe"),
    ("iron", "minecraft:iron_ingot", ["MM", " S", " S"], "Железная тяпка", "Iron Garden Hoe"),
    # TODO: уточнить — стального слитка пока нет, как и у остальных стальных инструментов
    ("steel", None, None, "Стальная тяпка", "Steel Garden Hoe"),
]

# Грядки: почва, названия ru / en
FARMLANDS = [
    ("zheltozem", "Грядка из жёлтозёма", "Zheltozem Farmland"),
    ("burozem", "Грядка из бурозёма", "Burozem Farmland"),
    ("chernozem", "Грядка из чернозёма", "Chernozem Farmland"),
]

# Ягодные кусты: ягода, куст ru / en, ягода ru / en
BERRIES = [
    ("currant", "Куст смородины", "Currant Bush", "Смородина", "Currants"),
    ("cloudberry", "Куст морошки", "Cloudberry Bush", "Морошка", "Cloudberries"),
    ("gooseberry", "Куст крыжовника", "Gooseberry Bush", "Крыжовник", "Gooseberries"),
    ("blackberry", "Куст ежевики", "Blackberry Bush", "Ежевика", "Blackberries"),
]

# Где растут дикие растения: биом -> что в нём растёт (леса — scripts/gen_forests.py). TODO: уточнить биомы.
# Смородина и крыжовник — умеренные равнины и леса (смородина — и в сырых), морошка — самая прохладная полоса,
# ежевика — тёплые и умеренные леса и опушки, картофель — умеренные равнины, морковь — равнины и светлые рощи,
# джут — жаркие и влажные места (жёлтозём).
WILD_BY_BIOME = {
    "burozem_plains": ["currant_bush", "cloudberry_bush", "gooseberry_bush", "blackberry_bush", "wild_carrot", "wild_potato"],
    "burozem_taiga": ["currant_bush", "cloudberry_bush"],
    "pine_forest": ["currant_bush", "cloudberry_bush"],
    "burozem_forest": ["currant_bush", "gooseberry_bush", "blackberry_bush"],
    "maple_forest": ["currant_bush", "gooseberry_bush", "blackberry_bush"],
    "alder_forest": ["currant_bush", "blackberry_bush"],
    "thuja_forest": ["currant_bush"],
    "chernozem_plains": ["currant_bush", "gooseberry_bush", "wild_carrot", "wild_potato"],
    "chernozem_forest_steppe": ["currant_bush", "gooseberry_bush", "blackberry_bush", "wild_carrot"],
    "walnut_grove": ["gooseberry_bush", "blackberry_bush", "wild_carrot"],
    "zheltozem_plains": ["blackberry_bush", "wild_carrot", "wild_jute"],
    "sakura_grove": ["blackberry_bush", "wild_carrot"],
    "lilac_grove": ["blackberry_bush", "wild_carrot"],
    "ironwood_forest": ["blackberry_bush"],
    "acacia_grove": ["wild_carrot", "wild_jute"],
    "eucalyptus_forest": ["wild_jute"],
    "palm_grove": ["wild_jute"],
    "ebony_forest": ["wild_jute"],
}
# Пятно: растение, попыток (столько растений максимум), разброс, раз в сколько чанков
PATCHES = {
    "currant_bush": (3, 2, 8),
    "cloudberry_bush": (3, 2, 6),
    "gooseberry_bush": (3, 2, 8),
    "blackberry_bush": (3, 2, 8),
    "wild_carrot": (4, 2, 6),
    "wild_potato": (4, 2, 8),
    "wild_jute": (5, 2, 5),
}

LANG = {
    "block.trailcraft.wild_carrot": ("Дикая морковь", "Wild Carrot"),
    "block.trailcraft.wild_potato": ("Дикий картофель", "Wild Potato"),
    "block.trailcraft.wild_jute": ("Дикий джут", "Wild Jute"),
    "block.trailcraft.jute_crop": ("Посевы джута", "Jute Crop"),
    "item.trailcraft.jute": ("Джут", "Jute"),
    "item.trailcraft.jute_seeds": ("Семена джута", "Jute Seeds"),
    "item.trailcraft.rope": ("Верёвка", "Rope"),
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


def ingredient(ref):
    return {"tag": ref[1:]} if ref.startswith("#") else {"item": ref}


# ============================== МОДЕЛИ И БЛОКСТЕЙТЫ ==============================

def block_model(name, parent, textures, cutout=False):
    data = {"parent": parent}
    if cutout:
        data["render_type"] = "minecraft:cutout"
    data["textures"] = textures
    write(f"assets/{NS}/models/block/{name}.json", data)


def cross(name, texture=None):
    block_model(name, "minecraft:block/cross", {"cross": f"{NS}:block/{texture or name}"}, cutout=True)


def crop(name, texture=None):
    block_model(name, "minecraft:block/crop", {"crop": f"{NS}:block/{texture or name}"}, cutout=True)


def item_model(item_id, texture=None, parent="minecraft:item/generated"):
    write(f"assets/{NS}/models/item/{item_id}.json",
          {"parent": parent, "textures": {"layer0": texture or f"{NS}:item/{item_id}"}})


def blockstate(name, variants):
    write(f"assets/{NS}/blockstates/{name}.json",
          {"variants": {k: {"model": f"{NS}:block/{v}"} for k, v in variants.items()}})


def models():
    for m, *_ in HOES:
        item_model(f"{m}_hoe", parent="minecraft:item/handheld")

    for soil, *_ in FARMLANDS:
        for suffix in ("", "_moist"):
            block_model(f"{soil}_farmland{suffix}", "minecraft:block/template_farmland",
                        {"dirt": f"{NS}:block/{soil}", "top": f"{NS}:block/{soil}_farmland{suffix}"})
        blockstate(f"{soil}_farmland", {f"moisture={i}": f"{soil}_farmland" + ("_moist" if i == 7 else "")
                                        for i in range(8)})
        write(f"assets/{NS}/models/item/{soil}_farmland.json", {"parent": f"{NS}:block/{soil}_farmland"})

    for berry, *_ in BERRIES:
        for i in range(4):
            cross(f"{berry}_bush_stage{i}")
        blockstate(f"{berry}_bush", {f"age={i}": f"{berry}_bush_stage{i}" for i in range(4)})
        item_model(f"{berry}_bush", f"{NS}:block/{berry}_bush_stage3")
        item_model(berry)

    for plant in ("wild_carrot", "wild_potato"):
        cross(plant)
        blockstate(plant, {"": plant})
        item_model(plant, f"{NS}:block/{plant}")

    cross("wild_jute_bottom")
    cross("wild_jute_top")
    blockstate("wild_jute", {"half=lower": "wild_jute_bottom", "half=upper": "wild_jute_top"})
    item_model("wild_jute", f"{NS}:block/wild_jute_top")

    # Посевы джута: 0–2 — в один блок, с 3-й — в два (низ как у дикого джута), на 4-й — цветы сверху
    for i in range(3):
        crop(f"jute_crop_stage{i}")
    crop("jute_crop_bottom", "wild_jute_bottom")
    crop("jute_crop_stage3_top")
    crop("jute_crop_stage4_top", "wild_jute_top")
    variants = {}
    for age in range(5):
        lower = f"jute_crop_stage{age}" if age < 3 else "jute_crop_bottom"
        # верхней половины до 3-й стадии не бывает, но у блокстейта должна быть модель для каждого состояния
        upper = "jute_crop_stage4_top" if age == 4 else "jute_crop_stage3_top"
        variants[f"age={age},half=lower"] = lower
        variants[f"age={age},half=upper"] = upper
    blockstate("jute_crop", variants)

    for item in ("jute", "jute_seeds", "rope"):
        item_model(item)


# ============================== ЛУТ ==============================

def count(lo, hi):
    return {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}


def pool(item, functions=(), conditions=()):
    entry = {"type": "minecraft:item", "name": item}
    if functions:
        entry["functions"] = list(functions)
    return {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry],
            "conditions": [{"condition": "minecraft:survives_explosion"}, *conditions]}


def state_is(block, **props):
    return {"condition": "minecraft:block_state_property", "block": f"{NS}:{block}", "properties": props}


def loot_table(block, pools):
    write(f"data/{NS}/loot_table/blocks/{block}.json",
          {"type": "minecraft:block", "pools": pools, "random_sequence": f"{NS}:blocks/{block}"})


def loot():
    for soil, *_ in FARMLANDS:
        loot_table(f"{soil}_farmland", [pool(f"{NS}:{soil}")])
    # кусты — ветки
    for berry, *_ in BERRIES:
        loot_table(f"{berry}_bush", [pool(f"{NS}:stick_branch", [count(1, 2)])])
    loot_table("wild_carrot", [pool("minecraft:carrot", [count(1, 2)])])
    loot_table("wild_potato", [pool("minecraft:potato", [count(1, 2)])])
    # двойные растения — только с нижней половины (верхнюю ломают — нижняя ломается следом и даёт лут)
    lower = state_is("wild_jute", half="lower")
    loot_table("wild_jute", [pool(f"{NS}:jute", [count(1, 2)], [lower]), pool(f"{NS}:jute_seeds", [], [lower])])
    lower = state_is("jute_crop", half="lower")
    ripe = state_is("jute_crop", half="lower", age="4")
    loot_table("jute_crop", [
        pool(f"{NS}:jute", [count(2, 3)], [ripe]),
        pool(f"{NS}:jute_seeds", [], [lower]),
        pool(f"{NS}:jute_seeds", [], [ripe, {"condition": "minecraft:random_chance", "chance": 0.5}]),
    ])


# ============================== ТЭГИ ==============================

def tags():
    hoes = [f"{NS}:{m}_hoe" for m, *_ in HOES]
    tag("item", "minecraft:hoes", hoes)
    for t in ("minecraft:enchantable/durability", "minecraft:enchantable/mining", "minecraft:enchantable/mining_loot"):
        tag("item", t, hoes)
    tag("block", "minecraft:mineable/shovel", [f"{NS}:{soil}_farmland" for soil, *_ in FARMLANDS])
    tag("block", "minecraft:maintains_farmland", [f"{NS}:jute_crop"])
    tag("block", "minecraft:crops", [f"{NS}:jute_crop"])
    tag("block", "minecraft:replaceable_by_trees",
        [f"{NS}:{b}_bush" for b, *_ in BERRIES] + [f"{NS}:wild_carrot", f"{NS}:wild_potato", f"{NS}:wild_jute"])
    tag("item", "c:seeds", [f"{NS}:jute_seeds"])
    tag("item", "c:foods/berry", [f"{NS}:{b}" for b, *_ in BERRIES])
    tag("item", "c:ropes", [f"{NS}:rope"])


# ============================== РЕЦЕПТЫ ==============================

def shaped(name, pattern, key, result, n=1):
    write(f"data/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "equipment" if name.endswith("_hoe") else "misc",
        "key": key, "pattern": pattern, "result": {"id": result, "count": n},
    })


def recipes():
    # тяпки: материал сверху, обработанная палка — черенок
    for m, material, pattern, *_ in HOES:
        if material:
            shaped(f"{m}_hoe", pattern, {"M": ingredient(material), "S": {"item": f"{NS}:stick_processed"}}, f"{NS}:{m}_hoe")
    shaped("rope", ["J", "J", "J"], {"J": {"item": f"{NS}:jute"}}, f"{NS}:rope")


# ============================== ГЕНЕРАЦИЯ ==============================

def patch_state(plant):
    """Кусты появляются уже подросшими, часто с ягодами."""
    if plant.endswith("_bush"):
        return {"type": "minecraft:weighted_state_provider", "entries": [
            {"weight": w, "data": {"Name": f"{NS}:{plant}", "Properties": {"age": str(age)}}}
            for age, w in ((1, 1), (2, 1), (3, 2))]}
    return {"type": "minecraft:simple_state_provider", "state": {"Name": f"{NS}:{plant}"}}


def worldgen():
    for plant, (tries, spread, rarity) in PATCHES.items():
        name = f"{plant}_patch"
        write(f"data/{NS}/worldgen/configured_feature/{name}.json", {
            "type": "minecraft:random_patch",
            "config": {
                "tries": tries, "xz_spread": spread, "y_spread": 1,
                "feature": {
                    "feature": {"type": "minecraft:simple_block", "config": {"to_place": patch_state(plant)}},
                    # только на воздух или на место нашей травы (не в воду)
                    "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                        "type": "minecraft:matching_blocks",
                        "blocks": ["minecraft:air", f"{NS}:grass_small", f"{NS}:grass_medium", f"{NS}:grass_tall"]}}],
                },
            },
        })
        write(f"data/{NS}/worldgen/placed_feature/{name}.json", {
            "feature": f"{NS}:{name}",
            "placement": [
                {"type": "minecraft:rarity_filter", "chance": rarity},
                {"type": "minecraft:in_square"},
                # высота земли до деревьев (WORLD_SURFACE_WG в лесу уже по кронам)
                {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE"},
                {"type": "minecraft:biome"},
            ],
        })
    for biome, plants in WILD_BY_BIOME.items():
        write(f"data/{NS}/neoforge/biome_modifier/wild_plants_{biome}.json", {
            "type": "neoforge:add_features",
            "biomes": [f"{NS}:{biome}"],
            # во всех биомах в одном порядке (как в PATCHES), иначе игра падает с «Feature order cycle»
            "features": [f"{NS}:{p}_patch" for p in PATCHES if p in plants],
            "step": "vegetal_decoration",
        })


# ============================== ПЕРЕВОДЫ ==============================

def lang():
    entries = dict(LANG)
    for m, _, _, ru, en in HOES:
        entries[f"item.{NS}.{m}_hoe"] = (ru, en)
    for soil, ru, en in FARMLANDS:
        entries[f"block.{NS}.{soil}_farmland"] = (ru, en)
    for berry, bush_ru, bush_en, ru, en in BERRIES:
        entries[f"block.{NS}.{berry}_bush"] = (bush_ru, bush_en)
        entries[f"item.{NS}.{berry}"] = (ru, en)
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for key, names in entries.items():
            data[key] = names[k]
        write(rel, data)


if __name__ == "__main__":
    models()
    loot()
    tags()
    recipes()
    worldgen()
    lang()
    print("ok")

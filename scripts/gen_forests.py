"""
Генерирует леса мира TrailCraft (пресет trailworldgen): лесные биомы, смеси деревьев, деревья у рек,
раскладку биомов (world_preset), почву под лесами (правила поверхности в noise_settings), тэг биомов и переводы,
а также JSON вариантов листвы на чужой древесине (красный клён — ModWood.addLeafVariant).

Запуск из корня проекта:  python scripts/gen_forests.py
Лесной биом строится из равнины той же почвы (biome/<почва>_plains.json): те же мобы, пещеры, озёра лавы,
меняются температура, осадки, цвет травы, деревья и то, что лежит на земле. Почва под лесом — та же, что на равнине:
скрипт вписывает лес в правило поверхности равнины. Раскладка биомов целиком задаётся таблицей ниже (layout).
Скрипт перезаписывает только свои файлы; в тэги и переводы добавляет свои строки, не трогая чужие.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"

# Пятна красного клёна в кленовом лесу: шум биома выше порога (примерно треть площади) — пятно
RED_MAPLE_NOISE = 0.25
# Рощи лесостепи: шум биома выше порога (примерно 45% площади) — роща
GROVE_NOISE = 0.1

# Леса: почва, названия ru / en, температура и осадки биома, цвет травы, слои деревьев, лишние мобы к мобам равнины.
# Слой деревьев: (имя, {порода: вес}, густота). Густота: ("count", n) — n попыток на чанк;
# ("noise", ниже, выше, порог) — столько попыток там, где шум биома ниже / выше порога (пятна размером в сотню блоков);
# ("rarity", n) — одно дерево раз в n чанков. Деревья не касаются друг друга, поэтому реальная густота ниже.
FORESTS = {
    # --- бурозём ---
    "burozem_taiga": dict(
        soil="burozem", ru="Тайга", en="Taiga", temperature=0.3, downfall=0.8, grass=0x6E9C6A,
        trees=[("trees", {"spruce": 4, "fir": 3, "cedar": 1, "birch": 1, "aspen": 1}, ("count", 9))],
        mobs=[("minecraft:wolf", 8, 4, 4), ("minecraft:fox", 8, 2, 4), ("minecraft:rabbit", 4, 2, 3)]),
    "pine_forest": dict(
        soil="burozem", ru="Сосновый бор", en="Pine Forest", temperature=0.35, downfall=0.5, grass=0x7A9E5E,
        trees=[("trees", {"pine": 1}, ("count", 7))],
        mobs=[("minecraft:wolf", 5, 4, 4), ("minecraft:fox", 4, 2, 4), ("minecraft:rabbit", 4, 2, 3)]),
    "burozem_forest": dict(
        soil="burozem", ru="Широколиственный лес", en="Broadleaf Forest", temperature=0.6, downfall=0.8, grass=0x73A462,
        trees=[("trees", {"oak": 4, "linden": 3, "chestnut": 2, "walnut": 1, "birch": 1}, ("count", 8))],
        mobs=[("minecraft:wolf", 5, 4, 4)]),
    # клён 70%, красный клён 30%: красный — в основном пятнами, изредка поодиночке
    "maple_forest": dict(
        soil="burozem", ru="Кленовый лес", en="Maple Forest", temperature=0.55, downfall=0.8, grass=0x76A85C,
        trees=[("maples", {"maple": 1}, ("noise", 8, 2, RED_MAPLE_NOISE)),
               ("red_maples", {"red_maple": 1}, ("noise", 0, 7, RED_MAPLE_NOISE)),
               ("lone_red_maples", {"red_maple": 1}, ("rarity", 4))],
        mobs=[("minecraft:wolf", 5, 4, 4)]),
    "alder_forest": dict(
        soil="burozem", ru="Ольшаник", en="Alder Forest", temperature=0.55, downfall=0.9, grass=0x5F9A55,
        trees=[("trees", {"alder": 1}, ("count", 8))],
        mobs=[]),
    "thuja_forest": dict(
        soil="burozem", ru="Туевый лес", en="Thuja Forest", temperature=0.5, downfall=0.9, grass=0x6A9C60,
        trees=[("trees", {"thuja": 1}, ("count", 8))],
        mobs=[("minecraft:wolf", 3, 4, 4)]),
    # --- чернозём ---
    "chernozem_forest_steppe": dict(
        soil="chernozem", ru="Лесостепь", en="Forest Steppe", temperature=0.7, downfall=0.6, grass=0x79C05A,
        trees=[("trees", {"aspen": 3, "oak": 2, "linden": 1, "maple": 1}, ("noise", 0, 7, GROVE_NOISE))],
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    "walnut_grove": dict(
        soil="chernozem", ru="Ореховая роща", en="Walnut Grove", temperature=0.75, downfall=0.6, grass=0x86BC5A,
        trees=[("trees", {"walnut": 1}, ("count", 4))],
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    # --- жёлтозём ---
    "sakura_grove": dict(
        soil="zheltozem", ru="Сакуровая роща", en="Sakura Grove", temperature=0.8, downfall=0.8, grass=0x9EBF58,
        trees=[("trees", {"sakura": 1}, ("count", 5))],
        mobs=[]),
    "lilac_grove": dict(
        soil="zheltozem", ru="Сиреневая роща", en="Lilac Grove", temperature=0.8, downfall=0.7, grass=0xA5BE57,
        trees=[("trees", {"lilac": 1}, ("count", 5))],
        mobs=[]),
    "ironwood_forest": dict(
        soil="zheltozem", ru="Лес железного дерева", en="Ironwood Forest", temperature=0.85, downfall=0.9, grass=0x8FB04E,
        trees=[("trees", {"ironwood": 1}, ("count", 8))],
        mobs=[]),
    "acacia_grove": dict(
        soil="zheltozem", ru="Акациевая роща", en="Acacia Grove", temperature=1.0, downfall=0.6, grass=0xB8B653,
        trees=[("trees", {"acacia": 1}, ("count", 3))],
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    "eucalyptus_forest": dict(
        soil="zheltozem", ru="Эвкалиптовый лес", en="Eucalyptus Forest", temperature=1.0, downfall=0.85, grass=0x99B455,
        trees=[("trees", {"eucalyptus": 1}, ("count", 7))],
        mobs=[]),
    "palm_grove": dict(
        soil="zheltozem", ru="Пальмовая роща", en="Palm Grove", temperature=1.1, downfall=0.8, grass=0xA9BC52,
        trees=[("trees", {"palm": 1}, ("count", 4))],
        mobs=[]),
    "ebony_forest": dict(
        soil="zheltozem", ru="Эбеновый лес", en="Ebony Forest", temperature=1.1, downfall=0.95, grass=0x7FAE45,
        trees=[("trees", {"ebony": 1}, ("count", 9))],
        mobs=[]),
}
# Больше не генерируются: скрипт убирает их из тэга и переводов
REMOVED_BIOMES = ["zheltozem_forest"]

# Пойменные деревья на берегах рек (редко): порода: вес, попыток на чанк
RIVER_TREES = {"willow": 3, "alder": 2, "white_poplar": 2, "pussy_willow": 2}
RIVER_TREE_COUNT = 2

# На земле в лесу: трава, камешки, упавшие ветки (попыток на чанк). Высота берётся по земле до деревьев
# (WORLD_SURFACE / OCEAN_FLOOR), а не по кронам, как *_WG у равнин.
FOREST_GROUND = {
    "forest_grass_patch": ("grass_patch", 3, "WORLD_SURFACE"),
    "forest_loose_pebbles": ("loose_pebble", 4, "OCEAN_FLOOR"),
    "forest_fallen_branches": ("fallen_branch", 2, "WORLD_SURFACE"),
}

# Варианты листвы на чужой древесине (ModWood.addLeafVariant): вид -> порода древесины, листва ru / en, саженец ru / en
LEAF_VARIANTS = {
    "red_maple": ("maple", "Листва красного клёна", "Red Maple Leaves", "Саженец красного клёна", "Red Maple Sapling"),
}

# === РАСКЛАДКА БИОМОВ (multi_noise) ===
# Температура делит сушу на полосы почв: бурозём (холодно) | чернозём | жёлтозём (тепло), внутри полос — подполосы.
T_CHERNOZEM, T_ZHELTOZEM = -0.24, 0.02
T_TAIGA = -0.55          # бурозём холоднее — бореальные леса (тайга, бор), теплее — широколиственные
T_SUBTROPIC, T_TROPIC = 0.25, 0.5  # жёлтозём: тёплые рощи | субтропики | тропики
T_RED_DESERT = 0.37      # жёлтозём: сухо — пустыня, ещё теплее — красная пустыня
# Влажность: от сухого к влажному — равнина, потом леса
H_PINE, H_TAIGA = -0.2, 0.05                    # бурозём холодный: равнина | бор | тайга
H_BROADLEAF, H_MAPLE, H_WET = -0.1, 0.2, 0.45   # бурозём умеренный: равнина | широколиственный | кленовый | ольшаник, туя
H_FOREST_STEPPE, H_WALNUT = 0.15, 0.4           # чернозём: равнина | лесостепь | ореховая роща
H_ZHELTOZEM_FOREST = 0.2                        # жёлтозём: пустыни | равнина | рощи и леса (влажнее H_WET — леса)
H_DESERT, H_SHORE = -0.02, -0.14
# Континентальность: море | прибрежная полоса | суша; странность около 0 — реки, по знаку странности — пары биомов
C_SEA, C_COAST = -0.19, -0.15
W_RIVER = 0.05


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


# ============================== ДЕРЕВЬЯ ==============================

def tree_mix(name, weights):
    """Смесь деревьев: random_selector, шансы подобраны так, чтобы породы выпадали пропорционально весам."""
    woods = list(weights.items())
    left = sum(weights.values())
    features = []
    for wood, w in woods[:-1]:
        features.append({"feature": f"{NS}:{wood}_wild_checked", "chance": round(w / left, 4)})
        left -= w
    write(f"data/{NS}/worldgen/configured_feature/{name}.json", {
        "type": "minecraft:random_selector",
        "config": {"features": features, "default": f"{NS}:{woods[-1][0]}_wild_checked"},
    })


def tree_placement(name, density):
    kind = density[0]
    if kind == "noise":
        _, below, above, level = density
        first = [{"type": "minecraft:noise_threshold_count", "noise_level": level, "below_noise": below, "above_noise": above}]
    elif kind == "rarity":
        first = [{"type": "minecraft:rarity_filter", "chance": density[1]}]
    else:
        first = [{"type": "minecraft:count", "count": density[1]}]
    write(f"data/{NS}/worldgen/placed_feature/{name}.json", {
        "feature": f"{NS}:{name}",
        "placement": [
            *first,
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
            {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": "minecraft:biome"},
        ],
    })


def tree_layers(biome):
    return [f"{biome}_{name}" for name, _, _ in FORESTS[biome]["trees"]]


def trees():
    for biome, f in FORESTS.items():
        for (name, weights, density), feature in zip(f["trees"], tree_layers(biome)):
            tree_mix(feature, weights)
            tree_placement(feature, density)
    tree_mix("river_trees", RIVER_TREES)
    tree_placement("river_trees", ("count", RIVER_TREE_COUNT))
    write(f"data/{NS}/neoforge/biome_modifier/river_trees.json", {
        "type": "neoforge:add_features",
        "biomes": [f"{NS}:{soil}_river" for soil in ("burozem", "chernozem", "zheltozem")],
        "features": [f"{NS}:river_trees"],
        "step": "vegetal_decoration",
    })
    for name, (feature, count, heightmap) in FOREST_GROUND.items():
        write(f"data/{NS}/worldgen/placed_feature/{name}.json", {
            "feature": f"{NS}:{feature}",
            "placement": [
                {"type": "minecraft:count", "count": count},
                {"type": "minecraft:in_square"},
                {"type": "minecraft:heightmap", "heightmap": heightmap},
                {"type": "minecraft:biome"},
            ],
        })


def leaf_variants():
    """Листва, саженец и деревья варианта — копии файлов породы древесины с заменой имён."""
    for vid, (base, leaves_ru, leaves_en, sapling_ru, sapling_en) in LEAF_VARIANTS.items():
        def clone(rel_base, rel_new, wood=False):
            text = json.dumps(read(rel_base), ensure_ascii=False)
            for a, b in ((f'"{NS}:{base}_', f'"{NS}:{vid}_'), (f"{NS}:block/{base}_", f"{NS}:block/{vid}_"),
                         (f"blocks/{base}_", f"blocks/{vid}_")):
                text = text.replace(a, b)
            data = json.loads(text)
            if wood:
                data["config"]["wood"] = vid
            write(rel_new, data)

        a = f"assets/{NS}"
        d = f"data/{NS}"
        for part in ("leaves", "sapling"):
            clone(f"{a}/blockstates/{base}_{part}.json", f"{a}/blockstates/{vid}_{part}.json")
            clone(f"{a}/models/block/{base}_{part}.json", f"{a}/models/block/{vid}_{part}.json")
            clone(f"{a}/models/item/{base}_{part}.json", f"{a}/models/item/{vid}_{part}.json")
            clone(f"{d}/loot_table/blocks/{base}_{part}.json", f"{d}/loot_table/blocks/{vid}_{part}.json")
        for suffix in ("tree", "wild_tree"):
            clone(f"{d}/worldgen/configured_feature/{base}_{suffix}.json", f"{d}/worldgen/configured_feature/{vid}_{suffix}.json", wood=True)
        for suffix in ("checked", "wild_checked"):
            clone(f"{d}/worldgen/placed_feature/{base}_{suffix}.json", f"{d}/worldgen/placed_feature/{vid}_{suffix}.json")
        leaves, sapling = f"{NS}:{vid}_leaves", f"{NS}:{vid}_sapling"
        for kind in ("block", "item"):
            tag(kind, "minecraft:leaves", [leaves])
            tag(kind, "minecraft:saplings", [sapling])
        tag("block", "minecraft:mineable/hoe", [leaves])
        tag("block", "minecraft:replaceable_by_trees", [leaves])
        for code, leaves_name, sapling_name in (("ru_ru", leaves_ru, sapling_ru), ("en_us", leaves_en, sapling_en)):
            rel = f"assets/{NS}/lang/{code}.json"
            data = read(rel)
            data[f"block.{NS}.{vid}_leaves"] = leaves_name
            data[f"block.{NS}.{vid}_sapling"] = sapling_name
            write(rel, data)


# ============================== БИОМЫ ==============================

VEGETAL = 9  # индекс шага vegetal_decoration в списке features биома


def biomes():
    for biome, f in FORESTS.items():
        data = read(f"data/{NS}/worldgen/biome/{f['soil']}_plains.json")
        data["temperature"] = f["temperature"]
        data["downfall"] = f["downfall"]
        data["effects"]["grass_color"] = f["grass"]
        # сначала деревья, потом то, что под ними (иначе камешки и ветки не дали бы встать стволу)
        data["features"][VEGETAL] = [f"{NS}:{t}" for t in tree_layers(biome)] + [f"{NS}:{g}" for g in FOREST_GROUND]
        creatures = data["spawners"]["creature"]
        for mob, weight, lo, hi in f["mobs"]:
            creatures.append({"type": mob, "weight": weight, "minCount": lo, "maxCount": hi})
        write(f"data/{NS}/worldgen/biome/{biome}.json", data)


def entry(biome, t, h, c, w):
    return {"biome": f"{NS}:{biome}", "parameters": {
        "temperature": list(t), "humidity": list(h), "continentalness": list(c), "erosion": [-2.0, 2.0],
        "depth": 0.0, "weirdness": list(w), "offset": 0.0}}


def layout():
    """Все биомы мира trailworldgen: море, реки, берега, суша (равнины, леса, пустыни)."""
    ALL = (-2.0, 2.0)
    COLD, MID, WARM = (-2.0, T_CHERNOZEM), (T_CHERNOZEM, T_ZHELTOZEM), (T_ZHELTOZEM, 2.0)
    BOREAL, TEMPERATE = (-2.0, T_TAIGA), (T_TAIGA, T_CHERNOZEM)
    WARM_GROVES, SUBTROPIC, TROPIC = (T_ZHELTOZEM, T_SUBTROPIC), (T_SUBTROPIC, T_TROPIC), (T_TROPIC, 2.0)
    LAND, COAST, INLAND = (C_SEA, 2.0), (C_SEA, C_COAST), (C_COAST, 2.0)
    DESERT, RED = (T_ZHELTOZEM, T_RED_DESERT), (T_RED_DESERT, 2.0)
    soils = (("burozem", COLD), ("chernozem", MID), ("zheltozem", WARM))
    # Суша вдали от берега: биом, температура, влажность. Пара биомов — первый при странности < 0, второй при > 0.
    inland = [
        ("burozem_plains", BOREAL, (-2.0, H_PINE)),
        ("pine_forest", BOREAL, (H_PINE, H_TAIGA)),
        ("burozem_taiga", BOREAL, (H_TAIGA, 2.0)),
        ("burozem_plains", TEMPERATE, (-2.0, H_BROADLEAF)),
        ("burozem_forest", TEMPERATE, (H_BROADLEAF, H_MAPLE)),
        ("maple_forest", TEMPERATE, (H_MAPLE, H_WET)),
        (("alder_forest", "thuja_forest"), TEMPERATE, (H_WET, 2.0)),
        ("chernozem_plains", MID, (-2.0, H_FOREST_STEPPE)),
        ("chernozem_forest_steppe", MID, (H_FOREST_STEPPE, H_WALNUT)),
        ("walnut_grove", MID, (H_WALNUT, 2.0)),
        ("zheltozem_plains", WARM, (H_DESERT, H_ZHELTOZEM_FOREST)),
        (("sakura_grove", "lilac_grove"), WARM_GROVES, (H_ZHELTOZEM_FOREST, H_WET)),
        ("ironwood_forest", WARM_GROVES, (H_WET, 2.0)),
        ("acacia_grove", SUBTROPIC, (H_ZHELTOZEM_FOREST, H_WET)),
        ("eucalyptus_forest", SUBTROPIC, (H_WET, 2.0)),
        ("palm_grove", TROPIC, (H_ZHELTOZEM_FOREST, H_WET)),
        ("ebony_forest", TROPIC, (H_WET, 2.0)),
        ("desert", DESERT, (-2.0, H_DESERT)),
        ("red_desert", RED, (-2.0, H_DESERT)),
    ]

    out = [entry("sea", ALL, ALL, (-2.0, C_SEA), ALL)]
    out += [entry(f"{soil}_river", t, ALL, LAND, (-W_RIVER, W_RIVER)) for soil, t in soils]
    for side, w in enumerate(((-2.0, -W_RIVER), (W_RIVER, 2.0))):
        # берега: прибрежная полоса там, где сухо
        out += [entry(f"{soil}_shore", t, (-2.0, H_SHORE), COAST, w) for soil, t in soils]
        # суша
        for biome, t, h in inland:
            out.append(entry(biome[side] if isinstance(biome, tuple) else biome, t, h, INLAND, w))
        # прибрежная полоса там, где влажно: равнины (лес до самой воды не доходит)
        out += [
            entry("burozem_plains", COLD, (H_SHORE, 2.0), COAST, w),
            entry("chernozem_plains", MID, (H_SHORE, 2.0), COAST, w),
            entry("zheltozem_plains", WARM, (H_DESERT, 2.0), COAST, w),
            entry("desert", DESERT, (H_SHORE, H_DESERT), COAST, w),
            entry("red_desert", RED, (H_SHORE, H_DESERT), COAST, w),
        ]
    rel = f"data/{NS}/worldgen/world_preset/trailworldgen.json"
    data = read(rel)
    data["dimensions"]["minecraft:overworld"]["generator"]["biome_source"]["biomes"] = out
    write(rel, data)


def surface():
    """Под лесом та же почва, что на равнине: правило равнины действует на равнину и все леса этой почвы."""
    rel = f"data/{NS}/worldgen/noise_settings/trailcraft_world.json"
    data = read(rel)
    for rule in data["surface_rule"]["sequence"]:
        cond = rule.get("if_true", {})
        if cond.get("type") != "minecraft:biome":
            continue
        for soil in ("burozem", "chernozem", "zheltozem"):
            if f"{NS}:{soil}_plains" in cond["biome_is"]:
                cond["biome_is"] = [f"{NS}:{soil}_plains"] + [f"{NS}:{b}" for b, f in FORESTS.items() if f["soil"] == soil]
    write(rel, data)


def lang():
    for code, k in (("ru_ru", "ru"), ("en_us", "en")):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for biome in REMOVED_BIOMES:
            data.pop(f"biome.{NS}.{biome}", None)
        for biome, f in FORESTS.items():
            data[f"biome.{NS}.{biome}"] = f[k]
        write(rel, data)


if __name__ == "__main__":
    leaf_variants()
    trees()
    biomes()
    layout()
    surface()
    tag("worldgen/biome", f"{NS}:trailworldgen_biomes", [f"{NS}:{b}" for b in FORESTS],
        [f"{NS}:{b}" for b in REMOVED_BIOMES])
    lang()
    print("ok")

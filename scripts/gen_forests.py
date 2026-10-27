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
import re

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"

# Пятна красного клёна в кленовом лесу: шум биома выше порога (примерно треть площади) — пятно
RED_MAPLE_NOISE = 0.25
# Рощи лесостепи: шум биома выше порога (примерно 45% площади) — роща
GROVE_NOISE = 0.1
# Густые пятна саванны: шум биома выше порога (примерно 30% площади), там же водопои
SAVANNA_NOISE = 0.3

# Леса: почва, названия ru / en, температура и осадки биома, цвет травы, слои деревьев, лишние мобы к мобам равнины,
# водопои (необязательно). Слой деревьев: (имя, {порода: вес}, густота). Густота: ("count", n) — n попыток на чанк;
# ("noise", ниже, выше, порог) — столько попыток там, где шум биома ниже / выше порога (пятна размером в сотню блоков);
# ("rarity", n) — одно дерево раз в n чанков. Деревья не касаются друг друга, поэтому реальная густота ниже.
# Порода — дерево <порода>_wild_checked (или вариант из TREE_VARIANTS).
# Слои ставятся по порядку: крупные деревья — первым слоем, пока им есть место.
# Водопои: (порог шума, раз в n чанков) — маленькие озёра только там, где шум биома выше порога.
FORESTS = {
    # --- бурозём ---
    "burozem_taiga": dict(
        soil="burozem", ru="Тайга", en="Taiga", temperature=0.3, downfall=0.8, grass=0x6E9C6A,
        trees=[("trees", {"spruce": 4, "fir": 3, "cedar": 1, "birch": 1}, ("count", 9))],
        mobs=[("minecraft:wolf", 8, 4, 4), ("minecraft:fox", 8, 2, 4), ("minecraft:rabbit", 4, 2, 3)]),
    "pine_forest": dict(
        soil="burozem", ru="Сосновый бор", en="Pine Forest", temperature=0.35, downfall=0.5, grass=0x7A9E5E,
        trees=[("trees", {"pine": 1}, ("count", 12))],
        mobs=[("minecraft:wolf", 5, 4, 4), ("minecraft:fox", 4, 2, 4), ("minecraft:rabbit", 4, 2, 3)]),
    "birch_forest": dict(
        soil="burozem", ru="Березняк", en="Birch Forest", temperature=0.5, downfall=0.6, grass=0x88BB67,
        trees=[("trees", {"birch": 1}, ("count", 8))],
        mobs=[("minecraft:fox", 4, 2, 4), ("minecraft:rabbit", 4, 2, 3)]),
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
    # ивовый лес — только у воды: вдоль рек и на влажных берегах (раскладка — layout)
    "burozem_willow_forest": dict(
        soil="burozem", ru="Ивовый лес", en="Willow Forest", temperature=0.5, downfall=0.9, grass=0x6A9F58,
        trees=[("trees", {"willow": 1}, ("count", 7))],
        mobs=[]),
    # --- чернозём ---
    "chernozem_forest_steppe": dict(
        soil="chernozem", ru="Лесостепь", en="Forest Steppe", temperature=0.7, downfall=0.6, grass=0x79C05A,
        trees=[("trees", {"aspen": 3, "birch": 2, "linden": 1, "maple": 1}, ("noise", 0, 7, GROVE_NOISE))],
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    "walnut_grove": dict(
        soil="chernozem", ru="Ореховая роща", en="Walnut Grove", temperature=0.75, downfall=0.6, grass=0x86BC5A,
        trees=[("trees", {"walnut": 1}, ("count", 4))],
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    "chernozem_willow_forest": dict(
        soil="chernozem", ru="Ивовый лес", en="Willow Forest", temperature=0.7, downfall=0.9, grass=0x72B05A,
        trees=[("trees", {"willow": 1}, ("count", 7))],
        mobs=[]),
    # --- жёлтозём ---
    # сакура, сирень и железное дерево — маленькие рощи посреди равнины (сакура — только на возвышенностях)
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
    # 70% площади — редкие акации (одна на ~3 чанка), 30% — густые пятна с водопоями
    "savanna": dict(
        soil="zheltozem", ru="Саванна", en="Savanna", temperature=1.0, downfall=0.6, grass=0xB8B653,
        trees=[("trees", {"acacia": 1}, ("noise", 0, 8, SAVANNA_NOISE)),
               ("lone_trees", {"acacia": 1}, ("rarity", 3))],
        water=(SAVANNA_NOISE, 5),
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    # реликтовые эвкалипты (ствол 2×2) — примерно 30% деревьев
    "eucalyptus_forest": dict(
        soil="zheltozem", ru="Эвкалиптовый лес", en="Eucalyptus Forest", temperature=1.0, downfall=0.85, grass=0x99B455,
        trees=[("relicts", {"relict_eucalyptus": 1}, ("count", 2)),
               ("trees", {"eucalyptus": 1}, ("count", 10))],
        mobs=[]),
    # пальмы — только вдоль рек (раскладка — layout)
    "palm_shore": dict(
        soil="zheltozem", ru="Пальмовый берег", en="Palm Shore", temperature=1.1, downfall=0.8, grass=0xA9BC52,
        trees=[("trees", {"palm": 1}, ("count", 5))],
        mobs=[]),
    "ebony_forest": dict(
        soil="zheltozem", ru="Эбеновый лес", en="Ebony Forest", temperature=1.1, downfall=0.95, grass=0x7FAE45,
        trees=[("trees", {"ebony": 1}, ("count", 1))],
        mobs=[]),
}
# Больше не генерируются: скрипт удаляет их файлы и убирает их из тэга и переводов
REMOVED_BIOMES = ["zheltozem_forest", "acacia_grove", "palm_grove"]

# Одинокие деревья посреди равнин (всех трёх почв): имя слоя, {порода: вес}, густота (как у лесов)
PLAINS_TREES = [
    ("lone_lilacs", {"young_lilac": 1}, ("rarity", 16)),
    ("lone_pussy_willows", {"pussy_willow": 1}, ("rarity", 16)),
]
PLAINS = ["burozem_plains", "chernozem_plains", "zheltozem_plains"]

# Варианты деревьев на древесине породы: имя -> порода, форма, доля старых. Скрипт пишет <имя>_wild_tree и
# <имя>_wild_checked (растёт там же, где саженец породы). Из саженцев: <имя>_tree для имён из SAPLING_TREES —
# реликтовый эвкалипт растёт из 4 саженцев эвкалипта квадратом 2×2 (ModWood.MEGA_TREES), сразу большим.
TREE_VARIANTS = {
    "relict_eucalyptus": ("eucalyptus", "relict_eucalyptus", 1.0),
    "young_lilac": ("lilac", "sakura", 0.0),  # только молодая, из тонких брёвен
}
SAPLING_TREES = ["relict_eucalyptus"]

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
H_BROADLEAF, H_BIRCH, H_MAPLE, H_WET = -0.1, -0.03, 0.2, 0.45  # бурозём умеренный: равнина | березняк |
#                                                 широколиственный | кленовый | ольшаник, туя
H_FOREST_STEPPE, H_WALNUT = 0.15, 0.4           # чернозём: равнина | лесостепь | ореховая роща
H_ZHELTOZEM_FOREST = 0.2                        # жёлтозём: пустыни | равнина | саванна | эвкалипт, эбен (влажнее H_WET)
H_DESERT, H_SHORE = -0.02, -0.14
# Континентальность: море | прибрежная полоса | суша; странность около 0 — реки, по знаку странности — пары биомов
C_SEA, C_COAST = -0.19, -0.15
W_RIVER = 0.05
W_BANK = 0.09    # |странность| от W_RIVER до W_BANK — полоса вдоль реки, в среднем ~10 блоков от воды (ивы, пальмы)
# Маленькие рощи — вершины шума странности (|странность| > W_GROVE): пятна обычно в 2–5 чанков, изредка до 6×6 чанков и больше,
# примерно раз в 1000 блоков тёплой равнины (с верхней границей пятна становились кольцами вокруг полян)
W_GROVE = 0.92
# Сакура — на возвышенностях: |странность| около 2/3 (пики рельефа), эрозия низкая, но не горы, вдали от моря
W_SAKURA, E_SAKURA, C_SAKURA = (0.62, 0.72), (-0.5, -0.25), -0.05
SAKURA = dict(t=(T_ZHELTOZEM, T_SUBTROPIC), h=(H_DESERT, 2.0), c=(C_SAKURA, 2.0), e=E_SAKURA, w=W_SAKURA)
# Под сакуровой рощей земля поднята на SAKURA_HILL блоков (sakura_hill()); за краем биома — склон: подъём сходит на нет,
# когда параметр уходит за границу биома на SAKURA_PAD (склон обычно шириной 10–30 блоков)
SAKURA_HILL = 9
SAKURA_PAD = dict(t=0.02, h=0.03, c=0.03, e=0.06, w=0.09)


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


def water_holes(biome, level, rarity):
    """Водопои: маленькие озёра (ванильная фича lake) там, где шум биома выше порога, раз в rarity чанков.
    Шум считается в углу чанка, как и у слоя деревьев с тем же порогом, поэтому водопои — внутри густых пятен."""
    write(f"data/{NS}/worldgen/configured_feature/water_hole.json", {
        "type": "minecraft:lake",
        "config": {
            "fluid": {"type": "minecraft:simple_state_provider",
                      "state": {"Name": "minecraft:water", "Properties": {"level": "0"}}},
            "barrier": {"type": "minecraft:simple_state_provider", "state": {"Name": "minecraft:air"}},
        },
    })
    write(f"data/{NS}/worldgen/placed_feature/{biome}_water_holes.json", {
        "feature": f"{NS}:water_hole",
        "placement": [
            {"type": "minecraft:noise_threshold_count", "noise_level": level, "below_noise": 0, "above_noise": 1},
            {"type": "minecraft:rarity_filter", "chance": rarity},
            {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "WORLD_SURFACE_WG"},
            {"type": "minecraft:biome"},
        ],
    })


def tree_variants():
    for name, (wood, shape, old) in TREE_VARIANTS.items():
        tree = {"type": f"{NS}:trail_tree", "config": {"wood": wood, "shape": shape, "old_chance": old}}
        write(f"data/{NS}/worldgen/configured_feature/{name}_wild_tree.json", tree)
        if name in SAPLING_TREES:
            write(f"data/{NS}/worldgen/configured_feature/{name}_tree.json", tree)
        write(f"data/{NS}/worldgen/placed_feature/{name}_wild_checked.json", {
            "feature": f"{NS}:{name}_wild_tree",
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {
                "type": "minecraft:would_survive", "state": {"Name": f"{NS}:{wood}_sapling"}}}],
        })


def plains_tree_layers():
    return [f"plains_{name}" for name, _, _ in PLAINS_TREES]


def trees():
    for biome, f in FORESTS.items():
        for (name, weights, density), feature in zip(f["trees"], tree_layers(biome)):
            tree_mix(feature, weights)
            tree_placement(feature, density)
        if "water" in f:
            water_holes(biome, *f["water"])
    for (name, weights, density), feature in zip(PLAINS_TREES, plains_tree_layers()):
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

LAKES, VEGETAL = 1, 9  # индексы шагов lakes и vegetal_decoration в списке features биома


# Цвет воды: пресная (реки, озёра, суша) чуть светлее ванильной 3F76E4, морская (море и его берега *_shore) — ещё светлее
WATER_FRESH, WATER_SEA = 0x4E81E6, 0x5E8CE8
SEA_BIOMES = {"sea", "burozem_shore", "chernozem_shore", "zheltozem_shore"}


def water_colors():
    """Меняет только число water_color, не переписывая файл (у морей и берегов своё оформление JSON)."""
    folder = os.path.join(ROOT, "data", NS, "worldgen", "biome")
    for name in sorted(os.listdir(folder)):
        color = WATER_SEA if name[:-len(".json")] in SEA_BIOMES else WATER_FRESH
        p = os.path.join(folder, name)
        with open(p, encoding="utf-8") as f:
            text = f.read()
        text = re.sub(r'("water_color":\s*)\d+', lambda m: m.group(1) + str(color), text, count=1)
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            f.write(text)


def biomes():
    # одинокие деревья на равнинах — первыми в шаге (до травы, камешков и веток); старые слои plains_* заменяются
    for plains in PLAINS:
        rel = f"data/{NS}/worldgen/biome/{plains}.json"
        data = read(rel)
        rest = [ft for ft in data["features"][VEGETAL] if not ft.startswith(f"{NS}:plains_")]
        data["features"][VEGETAL] = [f"{NS}:{t}" for t in plains_tree_layers()] + rest
        write(rel, data)
    for biome, f in FORESTS.items():
        data = read(f"data/{NS}/worldgen/biome/{f['soil']}_plains.json")
        data["temperature"] = f["temperature"]
        data["downfall"] = f["downfall"]
        data["effects"]["grass_color"] = f["grass"]
        # сначала деревья, потом то, что под ними (иначе камешки и ветки не дали бы встать стволу)
        data["features"][VEGETAL] = [f"{NS}:{t}" for t in tree_layers(biome)] + [f"{NS}:{g}" for g in FOREST_GROUND]
        if "water" in f:
            data["features"][LAKES].append(f"{NS}:{biome}_water_holes")
        creatures = data["spawners"]["creature"]
        for mob, weight, lo, hi in f["mobs"]:
            creatures.append({"type": mob, "weight": weight, "minCount": lo, "maxCount": hi})
        write(f"data/{NS}/worldgen/biome/{biome}.json", data)


def entry(biome, t, h, c, e, w):
    return {"biome": f"{NS}:{biome}", "parameters": {
        "temperature": list(t), "humidity": list(h), "continentalness": list(c), "erosion": list(e),
        "depth": 0.0, "weirdness": list(w), "offset": 0.0}}


def carve(box, hole):
    """Коробка параметров без дырки: несколько коробок, которые не пересекаются (multi_noise не любит наложений)."""
    if any(hl >= hi or hh <= lo for (lo, hi), (hl, hh) in zip(box, hole)):
        return [box]
    out, rest = [], list(box)
    for i, ((lo, hi), (hl, hh)) in enumerate(zip(box, hole)):
        if hl > lo:
            out.append(tuple(rest[:i] + [(lo, hl)] + rest[i + 1:]))
        if hh < hi:
            out.append(tuple(rest[:i] + [(hh, hi)] + rest[i + 1:]))
        rest[i] = (max(lo, hl), min(hi, hh))
    return out


def layout_entries():
    """Все биомы мира trailworldgen: море, реки, берега, суша (равнины, леса, пустыни)."""
    ALL = (-2.0, 2.0)
    COLD, MID, WARM = (-2.0, T_CHERNOZEM), (T_CHERNOZEM, T_ZHELTOZEM), (T_ZHELTOZEM, 2.0)
    BOREAL, TEMPERATE = (-2.0, T_TAIGA), (T_TAIGA, T_CHERNOZEM)
    WARM_GROVES, SUBTROPIC, TROPIC = (T_ZHELTOZEM, T_SUBTROPIC), (T_SUBTROPIC, T_TROPIC), (T_TROPIC, 2.0)
    HOT = (T_SUBTROPIC, 2.0)
    LAND, COAST, INLAND = (C_SEA, 2.0), (C_SEA, C_COAST), (C_COAST, 2.0)
    DESERT, RED = (T_ZHELTOZEM, T_RED_DESERT), (T_RED_DESERT, 2.0)
    AW = (W_RIVER, 2.0)          # |странность| всей суши, кроме рек
    BANK = (W_RIVER, W_BANK)     # полоса вдоль реки
    soils = (("burozem", COLD), ("chernozem", MID), ("zheltozem", WARM))
    # Сетка: биом, температура, влажность, континентальность. Пара биомов — первый при странности < 0, второй при > 0.
    grid = [
        ("burozem_plains", BOREAL, (-2.0, H_PINE), INLAND),
        ("pine_forest", BOREAL, (H_PINE, H_TAIGA), INLAND),
        ("burozem_taiga", BOREAL, (H_TAIGA, 2.0), INLAND),
        ("burozem_plains", TEMPERATE, (-2.0, H_BROADLEAF), INLAND),
        ("birch_forest", TEMPERATE, (H_BROADLEAF, H_BIRCH), INLAND),
        ("burozem_forest", TEMPERATE, (H_BIRCH, H_MAPLE), INLAND),
        ("maple_forest", TEMPERATE, (H_MAPLE, H_WET), INLAND),
        (("alder_forest", "thuja_forest"), TEMPERATE, (H_WET, 2.0), INLAND),
        ("chernozem_plains", MID, (-2.0, H_FOREST_STEPPE), INLAND),
        ("chernozem_forest_steppe", MID, (H_FOREST_STEPPE, H_WALNUT), INLAND),
        ("walnut_grove", MID, (H_WALNUT, 2.0), INLAND),
        # тёплые рощи (сакура, сирень, железное дерево) — маленькими пятнами на этой равнине, см. ниже
        ("zheltozem_plains", WARM_GROVES, (H_DESERT, 2.0), INLAND),
        ("zheltozem_plains", HOT, (H_DESERT, H_ZHELTOZEM_FOREST), INLAND),
        ("savanna", HOT, (H_ZHELTOZEM_FOREST, H_WET), INLAND),
        ("eucalyptus_forest", SUBTROPIC, (H_WET, 2.0), INLAND),
        ("ebony_forest", TROPIC, (H_WET, 2.0), INLAND),
        ("desert", DESERT, (-2.0, H_DESERT), INLAND),
        ("red_desert", RED, (-2.0, H_DESERT), INLAND),
        # прибрежная полоса: где сухо — берег (пляж), где влажно — равнины
        ("burozem_shore", COLD, (-2.0, H_SHORE), COAST),
        ("chernozem_shore", MID, (-2.0, H_SHORE), COAST),
        ("zheltozem_shore", WARM, (-2.0, H_SHORE), COAST),
        ("burozem_plains", COLD, (H_SHORE, 2.0), COAST),
        ("chernozem_plains", MID, (H_SHORE, 2.0), COAST),
        ("zheltozem_plains", WARM, (H_DESERT, 2.0), COAST),
        ("desert", DESERT, (H_SHORE, H_DESERT), COAST),
        ("red_desert", RED, (H_SHORE, H_DESERT), COAST),
    ]
    # Пятна поверх сетки, по порядку (каждое вырезает себе место из того, что под ним):
    # биом, температура, влажность, континентальность, эрозия, |странность|
    patches = [
        # ивовый лес — вдоль рек и у воды, там, где растут леса
        ("burozem_willow_forest", BOREAL, (H_PINE, 2.0), INLAND, ALL, BANK),
        ("burozem_willow_forest", TEMPERATE, (H_BROADLEAF, 2.0), INLAND, ALL, BANK),
        ("chernozem_willow_forest", MID, (H_FOREST_STEPPE, 2.0), INLAND, ALL, BANK),
        ("burozem_willow_forest", BOREAL, (H_SHORE, 2.0), COAST, ALL, AW),
        ("burozem_willow_forest", TEMPERATE, (H_BROADLEAF, 2.0), COAST, ALL, AW),
        ("chernozem_willow_forest", MID, (H_FOREST_STEPPE, 2.0), COAST, ALL, AW),
        # пальмы — только вдоль рек
        ("palm_shore", HOT, (H_DESERT, 2.0), INLAND, ALL, BANK),
        # маленькие рощи посреди равнины: сирень при странности > 0, железное дерево при < 0
        (("ironwood_forest", "lilac_grove"), WARM_GROVES, (H_DESERT, 2.0), INLAND, ALL, (W_GROVE, 2.0)),
        # сакура — на возвышенностях
        ("sakura_grove", *(SAKURA[k] for k in "thcew")),
    ]
    boxes = [(b, (t, h, c, ALL, AW)) for b, t, h, c in grid]
    for b, *hole in patches:
        hole = tuple(hole)
        boxes = [(bb, piece) for bb, box in boxes for piece in carve(box, hole)] + [(b, hole)]

    out = [entry("sea", ALL, ALL, (-2.0, C_SEA), ALL, ALL)]
    out += [entry(f"{soil}_river", t, ALL, LAND, ALL, (-W_RIVER, W_RIVER)) for soil, t in soils]
    for side in (0, 1):
        for biome, (t, h, c, e, (lo, hi)) in boxes:
            w = (-hi, -lo) if side == 0 else (lo, hi)
            out.append(entry(biome[side] if isinstance(biome, tuple) else biome, t, h, c, e, w))
    return out


def layout():
    rel = f"data/{NS}/worldgen/world_preset/trailworldgen.json"
    data = read(rel)
    data["dimensions"]["minecraft:overworld"]["generator"]["biome_source"]["biomes"] = layout_entries()
    write(rel, data)


def sakura_hill():
    """Возвышенность под сакуровой рощей. Рельеф от биома не зависит (его считает noise_router), поэтому подъём —
    функция плотности на тех же шумах климата: 1 внутри коробки параметров сакуры, 0 за её краем дальше SAKURA_PAD.
    Она прибавляется к overworld/depth (глубина меняется на 1/128 за блок, подъём n блоков — n / 128) в копии ванильного
    overworld/sloped_cheese; поле depth самого noise_router (по нему выбираются биомы) остаётся ванильным."""
    def shifted(noise):
        return {"type": "minecraft:shifted_noise", "noise": noise, "xz_scale": 0.25, "y_scale": 0.0,
                "shift_x": "minecraft:shift_x", "shift_y": "minecraft:zero", "shift_z": "minecraft:shift_z"}

    def ramp(x, k, b):  # clamp(k·x + b, 0, 1)
        return {"type": "minecraft:clamp", "min": 0.0, "max": 1.0,
                "input": {"type": "minecraft:add", "argument1": {"type": "minecraft:mul", "argument1": k, "argument2": x},
                          "argument2": b}}
    inputs = dict(t=shifted("minecraft:temperature"), h=shifted("minecraft:vegetation"), c="minecraft:overworld/continents",
                  e="minecraft:overworld/erosion", w={"type": "minecraft:abs", "argument": "minecraft:overworld/ridges"})
    ramps = []
    for key, (lo, hi) in SAKURA.items():
        pad = SAKURA_PAD[key]
        if lo > -2.0:  # 1 при x >= lo, 0 при x <= lo - pad
            ramps.append(ramp(inputs[key], 1 / pad, (pad - lo) / pad))
        if hi < 2.0:   # 1 при x <= hi, 0 при x >= hi + pad
            ramps.append(ramp(inputs[key], -1 / pad, (hi + pad) / pad))
    mask = ramps[0]
    for r in ramps[1:]:
        mask = {"type": "minecraft:min", "argument1": mask, "argument2": r}
    d = f"data/{NS}/worldgen/density_function"
    write(f"{d}/sakura_hill.json", {"type": "minecraft:flat_cache", "argument": {
        "type": "minecraft:mul", "argument1": SAKURA_HILL / 128, "argument2": mask}})
    write(f"{d}/overworld_depth.json", {"type": "minecraft:add", "argument1": "minecraft:overworld/depth",
                                        "argument2": f"{NS}:sakura_hill"})
    # копия ванильного overworld/sloped_cheese 1.21.1, глубина — своя
    write(f"{d}/sloped_cheese.json", {
        "type": "minecraft:add",
        "argument1": {"type": "minecraft:mul", "argument1": 4.0, "argument2": {
            "type": "minecraft:quarter_negative", "argument": {
                "type": "minecraft:mul",
                "argument1": {"type": "minecraft:add", "argument1": f"{NS}:overworld_depth", "argument2": {
                    "type": "minecraft:mul", "argument1": "minecraft:overworld/jaggedness", "argument2": {
                        "type": "minecraft:half_negative", "argument": {
                            "type": "minecraft:noise", "noise": "minecraft:jagged", "xz_scale": 1500.0, "y_scale": 0.0}}}},
                "argument2": "minecraft:overworld/factor"}}},
        "argument2": "minecraft:overworld/base_3d_noise",
    })

    def swap(o, old, new):
        if isinstance(o, dict):
            return {k: swap(v, old, new) for k, v in o.items()}
        if isinstance(o, list):
            return [swap(v, old, new) for v in o]
        return new if o == old else o
    rel = f"data/{NS}/worldgen/noise_settings/trailcraft_world.json"
    data = read(rel)
    router = data["noise_router"]
    router["final_density"] = swap(router["final_density"], "minecraft:overworld/sloped_cheese", f"{NS}:sloped_cheese")
    router["initial_density_without_jaggedness"] = swap(router["initial_density_without_jaggedness"],
                                                        "minecraft:overworld/depth", f"{NS}:overworld_depth")
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


def remove_old():
    """Файлы убранных биомов: сам биом и его слои деревьев (<биом>_*.json)."""
    d = path("data", NS, "worldgen")
    for biome in REMOVED_BIOMES:
        old = [os.path.join(d, "biome", f"{biome}.json")]
        for kind in ("configured_feature", "placed_feature"):
            old += [os.path.join(d, kind, n) for n in os.listdir(os.path.join(d, kind)) if n.startswith(f"{biome}_")]
        for p in old:
            if os.path.exists(p):
                os.remove(p)


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
    remove_old()
    leaf_variants()
    tree_variants()
    trees()
    biomes()
    water_colors()
    layout()
    sakura_hill()
    surface()
    tag("worldgen/biome", f"{NS}:trailworldgen_biomes", [f"{NS}:{b}" for b in FORESTS],
        [f"{NS}:{b}" for b in REMOVED_BIOMES])
    lang()
    print("ok")

"""
Генерирует леса мира TrailCraft (пресет trailworldgen): лесные биомы, смеси деревьев, деревья у рек,
раскладку биомов (world_preset), почву под лесами (правила поверхности в noise_settings), тэг биомов и переводы.

Запуск из корня проекта:  python scripts/gen_forests.py
Лесной биом строится из равнины той же почвы (biome/<почва>_plains.json): те же мобы, пещеры, озёра лавы,
меняются температура, осадки, цвет травы, деревья и то, что лежит на земле. Почва под лесом — та же, что на равнине:
скрипт дописывает лес в правило поверхности равнины. Раскладка биомов целиком задаётся таблицей ниже (layout).
Скрипт перезаписывает только свои файлы; в тэг и переводы добавляет свои строки, не трогая чужие.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"

# Леса: почва, названия ru / en, температура и осадки биома, цвет травы, деревья (порода: вес),
# густота (count — столько попыток на чанк; groves — рощи: столько попыток там, где шум выше порога, иначе ни одной),
# лишние мобы к мобам равнины. Деревья одного леса не касаются друг друга, поэтому реальная густота ниже.
FORESTS = {
    "burozem_forest": dict(
        soil="burozem", ru="Широколиственный лес", en="Broadleaf Forest", temperature=0.6, downfall=0.8, grass=0x73A462,
        trees={"oak": 4, "linden": 3, "maple": 3, "chestnut": 1, "walnut": 1, "birch": 1},
        density=("count", 8),
        mobs=[("minecraft:wolf", 5, 4, 4)]),
    "burozem_taiga": dict(
        soil="burozem", ru="Тайга", en="Taiga", temperature=0.3, downfall=0.8, grass=0x6E9C6A,
        trees={"spruce": 4, "fir": 3, "pine": 3, "cedar": 1, "birch": 1, "aspen": 1},
        density=("count", 9),
        mobs=[("minecraft:wolf", 8, 4, 4), ("minecraft:fox", 8, 2, 4), ("minecraft:rabbit", 4, 2, 3)]),
    "chernozem_forest_steppe": dict(
        soil="chernozem", ru="Лесостепь", en="Forest Steppe", temperature=0.7, downfall=0.6, grass=0x79C05A,
        trees={"birch": 4, "aspen": 3, "oak": 2, "linden": 1, "maple": 1},
        density=("groves", 7),
        mobs=[("minecraft:rabbit", 4, 2, 3)]),
    "zheltozem_forest": dict(
        soil="zheltozem", ru="Субтропический лес", en="Subtropical Forest", temperature=0.9, downfall=0.9, grass=0x9DB650,
        trees={"chestnut": 3, "ironwood": 2, "eucalyptus": 2, "acacia": 1, "sakura": 1, "walnut": 1, "alder": 1,
               "lilac": 1, "palm": 1, "ebony": 1, "thuja": 1},
        density=("count", 8),
        mobs=[]),
}

# Пойменные деревья на берегах рек (редко): порода: вес, попыток на чанк
RIVER_TREES = {"willow": 3, "alder": 2, "white_poplar": 2, "pussy_willow": 2}
RIVER_TREE_COUNT = 2

# Рощи лесостепи: шум биома выше этого порога — роща (примерно половина площади)
GROVE_NOISE = 0.1

# На земле в лесу: трава, камешки, упавшие ветки (попыток на чанк). Высота берётся по земле до деревьев
# (WORLD_SURFACE / OCEAN_FLOOR), а не по кронам, как *_WG у равнин.
FOREST_GROUND = {
    "forest_grass_patch": ("grass_patch", 3, "WORLD_SURFACE"),
    "forest_loose_pebbles": ("loose_pebble", 4, "OCEAN_FLOOR"),
    "forest_fallen_branches": ("fallen_branch", 2, "WORLD_SURFACE"),
}

# === РАСКЛАДКА БИОМОВ (multi_noise) ===
# Температура делит сушу на полосы почв: бурозём (холодно) | чернозём | жёлтозём (тепло).
T_CHERNOZEM, T_ZHELTOZEM = -0.24, 0.02
T_TAIGA = -0.55          # бурозём холоднее — тайга, теплее — широколиственный лес
T_RED_DESERT = 0.37      # жёлтозём: сухо — пустыня, ещё теплее — красная пустыня
# Влажность: выше порога — лес, ниже — равнина (у жёлтозёма ещё ниже — пустыни)
H_BUROZEM_FOREST, H_FOREST_STEPPE, H_ZHELTOZEM_FOREST = 0.0, 0.15, 0.2
H_DESERT, H_SHORE = -0.02, -0.14
# Континентальность: море | прибрежная полоса | суша; странность около 0 — реки
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
    kind, n = density
    if kind == "groves":
        first = {"type": "minecraft:noise_threshold_count", "noise_level": GROVE_NOISE, "below_noise": 0, "above_noise": n}
    else:
        first = {"type": "minecraft:count", "count": n}
    write(f"data/{NS}/worldgen/placed_feature/{name}.json", {
        "feature": f"{NS}:{name}",
        "placement": [
            first,
            {"type": "minecraft:in_square"},
            {"type": "minecraft:surface_water_depth_filter", "max_water_depth": 0},
            {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR"},
            {"type": "minecraft:biome"},
        ],
    })


def trees():
    for biome, f in FORESTS.items():
        tree_mix(f"{biome}_trees", f["trees"])
        tree_placement(f"{biome}_trees", f["density"])
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


# ============================== БИОМЫ ==============================

VEGETAL = 9  # индекс шага vegetal_decoration в списке features биома


def biomes():
    for biome, f in FORESTS.items():
        data = read(f"data/{NS}/worldgen/biome/{f['soil']}_plains.json")
        data["temperature"] = f["temperature"]
        data["downfall"] = f["downfall"]
        data["effects"]["grass_color"] = f["grass"]
        # сначала деревья, потом то, что под ними (иначе камешки и ветки не дали бы встать стволу)
        data["features"][VEGETAL] = [f"{NS}:{biome}_trees", *(f"{NS}:{g}" for g in FOREST_GROUND)]
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
    LAND, COAST, INLAND = (C_SEA, 2.0), (C_SEA, C_COAST), (C_COAST, 2.0)
    DESERT, RED = (T_ZHELTOZEM, T_RED_DESERT), (T_RED_DESERT, 2.0)
    soils = (("burozem", COLD), ("chernozem", MID), ("zheltozem", WARM))

    out = [entry("sea", ALL, ALL, (-2.0, C_SEA), ALL)]
    out += [entry(f"{soil}_river", t, ALL, LAND, (-W_RIVER, W_RIVER)) for soil, t in soils]
    for w in ((-2.0, -W_RIVER), (W_RIVER, 2.0)):
        # берега: прибрежная полоса там, где сухо
        out += [entry(f"{soil}_shore", t, (-2.0, H_SHORE), COAST, w) for soil, t in soils]
        # суша
        out += [
            entry("burozem_plains", COLD, (-2.0, H_BUROZEM_FOREST), INLAND, w),
            entry("burozem_taiga", (-2.0, T_TAIGA), (H_BUROZEM_FOREST, 2.0), INLAND, w),
            entry("burozem_forest", (T_TAIGA, T_CHERNOZEM), (H_BUROZEM_FOREST, 2.0), INLAND, w),
            entry("chernozem_plains", MID, (-2.0, H_FOREST_STEPPE), INLAND, w),
            entry("chernozem_forest_steppe", MID, (H_FOREST_STEPPE, 2.0), INLAND, w),
            entry("zheltozem_plains", WARM, (H_DESERT, H_ZHELTOZEM_FOREST), INLAND, w),
            entry("zheltozem_forest", WARM, (H_ZHELTOZEM_FOREST, 2.0), INLAND, w),
            entry("desert", DESERT, (-2.0, H_DESERT), INLAND, w),
            entry("red_desert", RED, (-2.0, H_DESERT), INLAND, w),
        ]
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
    """Под лесом та же почва, что на равнине: лес дописывается в условие biome_is правила равнины."""
    rel = f"data/{NS}/worldgen/noise_settings/trailcraft_world.json"
    data = read(rel)
    for rule in data["surface_rule"]["sequence"]:
        cond = rule.get("if_true", {})
        if cond.get("type") != "minecraft:biome":
            continue
        for soil in {f["soil"] for f in FORESTS.values()}:
            if f"{NS}:{soil}_plains" in cond["biome_is"]:
                for biome, f in FORESTS.items():
                    if f["soil"] == soil and f"{NS}:{biome}" not in cond["biome_is"]:
                        cond["biome_is"].append(f"{NS}:{biome}")
    write(rel, data)


def lang():
    for code, k in (("ru_ru", "ru"), ("en_us", "en")):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for biome, f in FORESTS.items():
            data[f"biome.{NS}.{biome}"] = f[k]
        write(rel, data)


if __name__ == "__main__":
    trees()
    biomes()
    layout()
    surface()
    tag("worldgen/biome", f"{NS}:trailworldgen_biomes", [f"{NS}:{b}" for b in FORESTS])
    lang()
    print("ok")

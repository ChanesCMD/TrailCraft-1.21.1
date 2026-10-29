"""
Свои растения (block/plant/): водоросль, кактус, пшеница, сахарный тростник, сахарная свёкла, какао — текстуры, модели,
блокстейты, лут, тэги, рецепты, генерация мира, переводы и превью docs/textures/plants.png.

Запуск из корня проекта:  python scripts/gen_plants.py   (нужен Pillow; ванильные текстуры, модели и лут — из jar
клиента в кэше Gradle, как в gen_create_compat.py; после него — python scripts/gen_create_compat.py, там рецепты Create
с нашими растениями)

Текстуры — ванильные, перекрашенные по оттенку (RECOLOR): водоросль бурая, как ламинария (по яркости — SEAWEED:
ванильная трава морская серая, цвет ей даёт биом); кактус сизо-зелёный;
пшеница янтарная; тростник жёлто-зелёный с бурыми узлами; сахарная свёкла с белым корнем; какао темнее. Модели и
блокстейты — копии ванильных со своими id (MODELS), лут — копии ванильного с заменой предметов (ITEMS), кроме водоросли
(падает сама, без ножниц).
Где растут: водоросль — дно рек и моря; кактус — пустыни; тростник — у воды на реках, берегах, в ивняках и на
пальмовом берегу; какао — на стволах пальм и эбеновых деревьев (CocoaPodFeature). Семена пшеницы падают с травы
(вместо ванильных), реже — семена сахарной свёклы.
Рецепты: на костре водоросль — сушёная водоросль, кактус — свой зелёный краситель, тесто Create — хлеб; 9 сушёных
водорослей — блок; тростник и свёкла — сахар; зелёный краситель — из кактуса.
"""
import colorsys
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

# Ванильный предмет → наш (в луте и рецептах)
ITEMS = {
    "minecraft:wheat": f"{NS}:wheat", "minecraft:wheat_seeds": f"{NS}:wheat_seeds",
    "minecraft:beetroot": f"{NS}:sugar_beet", "minecraft:beetroot_seeds": f"{NS}:sugar_beet_seeds",
    "minecraft:cocoa_beans": f"{NS}:cocoa_beans", "minecraft:cactus": f"{NS}:cactus",
    "minecraft:sugar_cane": f"{NS}:sugar_cane", "minecraft:dried_kelp_block": f"{NS}:dried_seaweed_block",
}
# Наш блок → ванильный образец (блокстейт, модели, лут)
BLOCKS = {"wheat": "wheat", "sugar_beets": "beetroots", "cocoa": "cocoa", "cactus": "cactus", "sugar_cane": "sugar_cane",
          "dried_seaweed_block": "dried_kelp_block"}


def wheat(h, s, v):  # жёлтое — янтарнее и насыщеннее, зелёное остаётся
    return (h - 0.025, min(1, s * 1.15), v * 0.97) if 0.07 < h < 0.2 else (h, s, v)


def beet(h, s, v):  # красно-лиловый корень — белый с желтизной, ботва остаётся
    return (0.11, s * 0.22, min(1, v * 1.35 + 0.1)) if h > 0.8 or h < 0.04 else (h, s, v)


def cane(h, s, v):  # стебли сочно-зелёные с желтизной (ванильная текстура бледная — её красит биом), тёмные узлы бурые
    return (0.22, min(1, s * 2.2 + 0.25), v * 0.9) if v > 0.35 else (0.07, s * 0.8, v)


RECOLOR = {
    # наша текстура: (ванильная, перекраска (h, s, v) → (h, s, v))
    "item/dried_seaweed": ("item/dried_kelp", lambda h, s, v: (h - 0.05, s * 0.9, v)),
    "block/dried_seaweed_block_top": ("block/dried_kelp_top", lambda h, s, v: (h - 0.05, s * 0.9, v)),
    "block/dried_seaweed_block_side": ("block/dried_kelp_side", lambda h, s, v: (h - 0.05, s * 0.9, v)),
    "block/dried_seaweed_block_bottom": ("block/dried_kelp_bottom", lambda h, s, v: (h - 0.05, s * 0.9, v)),
    "block/cactus_top": ("block/cactus_top", lambda h, s, v: (h + 0.07, s * 0.8, v)),
    "block/cactus_side": ("block/cactus_side", lambda h, s, v: (h + 0.07, s * 0.8, v)),
    "block/cactus_bottom": ("block/cactus_bottom", lambda h, s, v: (h + 0.07, s * 0.8, v)),
    "item/green_dye": ("item/green_dye", lambda h, s, v: (h + 0.07, s * 0.85, v)),
    **{f"block/wheat_stage{i}": (f"block/wheat_stage{i}", wheat) for i in range(8)},
    "item/wheat": ("item/wheat", wheat), "item/wheat_seeds": ("item/wheat_seeds", wheat),
    "block/sugar_cane": ("block/sugar_cane", cane), "item/sugar_cane": ("item/sugar_cane", cane),
    **{f"block/sugar_beets_stage{i}": (f"block/beetroots_stage{i}", beet) for i in range(4)},
    "item/sugar_beet": ("item/beetroot", beet), "item/sugar_beet_seeds": ("item/beetroot_seeds", beet),
    **{f"block/cocoa_stage{i}": (f"block/cocoa_stage{i}", lambda h, s, v: (h - 0.015, min(1, s * 1.1), v * 0.85))
       for i in range(3)},
    "item/cocoa_beans": ("item/cocoa_beans", lambda h, s, v: (h - 0.015, min(1, s * 1.1), v * 0.85)),
}

LANG = {
    "block.trailcraft.seaweed": ("Водоросль", "Seaweed"),
    "item.trailcraft.dried_seaweed": ("Сушёная водоросль", "Dried Seaweed"),
    "block.trailcraft.dried_seaweed_block": ("Блок сушёных водорослей", "Dried Seaweed Block"),
    "block.trailcraft.cactus": ("Кактус", "Cactus"),
    "item.trailcraft.green_dye": ("Зелёный краситель", "Green Dye"),
    "block.trailcraft.wheat": ("Посевы пшеницы", "Wheat Crops"),
    "item.trailcraft.wheat": ("Пшеница", "Wheat"),
    "item.trailcraft.wheat_seeds": ("Семена пшеницы", "Wheat Seeds"),
    "block.trailcraft.sugar_cane": ("Сахарный тростник", "Sugar Cane"),
    "block.trailcraft.sugar_beets": ("Посевы сахарной свёклы", "Sugar Beets"),
    "item.trailcraft.sugar_beet": ("Сахарная свёкла", "Sugar Beet"),
    "item.trailcraft.sugar_beet_seeds": ("Семена сахарной свёклы", "Sugar Beet Seeds"),
    "block.trailcraft.cocoa": ("Какао", "Cocoa"),
    "item.trailcraft.cocoa_beans": ("Какао-бобы", "Cocoa Beans"),
}
SANDS = [f"{NS}:sand_{c}" for c in ("white", "yellow", "gray", "red")]
RIVERS = [f"{NS}:{s}_river" for s in ("burozem", "chernozem", "zheltozem")]
SHORES = [f"{NS}:{s}_shore" for s in ("burozem", "chernozem", "zheltozem")]
# растение: (биомы, тип размещения)
WORLD = {
    "seaweed": RIVERS + [f"{NS}:sea"] + SHORES,
    "cactus": [f"{NS}:desert", f"{NS}:red_desert"],
    "sugar_cane": RIVERS + SHORES + [f"{NS}:palm_shore", f"{NS}:burozem_willow_forest", f"{NS}:chernozem_willow_forest"],
    "cocoa": [f"{NS}:ebony_forest", f"{NS}:palm_shore"],
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


def hsv_map(img, fn):
    img = img.convert("RGBA")
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                h, s, v = fn(*colorsys.rgb_to_hsv(r / 255, g / 255, b / 255))
                r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def swap(text):
    """Ванильные id блоков, моделей и предметов → наши."""
    for ours, vanilla in BLOCKS.items():
        text = text.replace(f'"minecraft:block/{vanilla}', f'"{NS}:block/{ours}').replace(f'"minecraft:{vanilla}"', f'"{NS}:{ours}"')
    for vanilla, ours in ITEMS.items():
        text = text.replace(f'"{vanilla}"', f'"{ours}"')
    return text.replace("beetroots", "sugar_beets").replace("dried_kelp", "dried_seaweed_block")


# Водоросль: у ванильной травы морской текстура серая (цвет даёт биом) — красим целиком по яркости в бурую ламинарию
SEAWEED = [(34, 40, 14), (56, 62, 20), (82, 86, 28), (108, 108, 38), (136, 130, 52), (166, 156, 72)]


def textures(client):
    seaweed = recolor(Image.open(BytesIO(client.read("assets/minecraft/textures/block/seagrass.png"))), SEAWEED)
    seaweed.save(os.path.join(TEX, "block", "seaweed.png"))
    # анимация колыхания — как у травы морской (кадры 16×16 друг под другом)
    with open(os.path.join(TEX, "block", "seaweed.png.mcmeta"), "wb") as f:
        f.write(client.read("assets/minecraft/textures/block/seagrass.png.mcmeta"))
    images = [seaweed.crop((0, 0, 16, 16))]
    for ours, (vanilla, fn) in RECOLOR.items():
        img = hsv_map(Image.open(BytesIO(client.read(f"assets/minecraft/textures/{vanilla}.png"))), fn)
        os.makedirs(os.path.dirname(os.path.join(TEX, ours)), exist_ok=True)
        img.save(os.path.join(TEX, *ours.split("/")) + ".png")
        images.append(img)
    return images


def models(client):
    read_mc = lambda rel: client.read(f"assets/minecraft/{rel}").decode("utf-8")
    for ours, vanilla in BLOCKS.items():
        state = json.loads(read_mc(f"blockstates/{vanilla}.json"))
        write(f"assets/{NS}/blockstates/{ours}.json", json.loads(swap(json.dumps(state))))
        for variant in state["variants"].values():
            name = variant["model"].split("/")[-1]
            model = json.loads(read_mc(f"models/block/{name}.json"))
            # свои текстуры: та же модель, текстуры — наши копии
            model["textures"] = {k: swap_name(("minecraft:" + v if v.startswith("block/") else v)
                                              .replace("minecraft:block/", f"{NS}:block/"))
                                 .replace("dried_kelp", "dried_seaweed_block") if not v.startswith("#") else v
                                 for k, v in model.get("textures", {}).items()}
            # прозрачные пиксели (края кактуса, промежутки у посевов) — вырезать: ванильным блокам это задаёт сама
            # игра, нашим копиям — модель; иначе они чёрные
            if "dried_kelp" not in name:
                model["render_type"] = "minecraft:cutout"
            write(f"assets/{NS}/models/block/{swap_name(name)}.json", model)
    # тростник без окраски по биому (цвет — в самой текстуре)
    write(f"assets/{NS}/models/block/sugar_cane.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                       "textures": {"cross": f"{NS}:block/sugar_cane"}})
    # водоросль — крест, как трава морская (свой блокстейт без высокой формы)
    write(f"assets/{NS}/blockstates/seaweed.json", {"variants": {"": {"model": f"{NS}:block/seaweed"}}})
    write(f"assets/{NS}/models/block/seaweed.json", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout",
                                                    "textures": {"cross": f"{NS}:block/seaweed"}})
    flat = {"seaweed": "block/seaweed", "dried_seaweed": "item/dried_seaweed", "green_dye": "item/green_dye",
            "wheat": "item/wheat", "wheat_seeds": "item/wheat_seeds", "sugar_cane": "item/sugar_cane",
            "sugar_beet": "item/sugar_beet", "sugar_beet_seeds": "item/sugar_beet_seeds", "cocoa_beans": "item/cocoa_beans"}
    for item, texture in flat.items():
        write(f"assets/{NS}/models/item/{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:{texture}"}})
    for block in ("cactus", "dried_seaweed_block"):
        write(f"assets/{NS}/models/item/{block}.json", {"parent": f"{NS}:block/{block}"})


def swap_name(name):
    return name.replace("beetroots", "sugar_beets").replace("dried_kelp_block", "dried_seaweed_block")


def loot(client):
    for ours, vanilla in BLOCKS.items():
        data = client.read(f"data/minecraft/loot_table/blocks/{vanilla}.json").decode("utf-8")
        table = json.loads(swap(data))
        table["random_sequence"] = f"{NS}:blocks/{ours}"
        write(f"data/{NS}/loot_table/blocks/{ours}.json", table)
    write(f"data/{NS}/loot_table/blocks/seaweed.json", {
        "type": "minecraft:block", "random_sequence": f"{NS}:blocks/seaweed",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:seaweed"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # трава: свои семена пшеницы вместо ванильных, реже — семена сахарной свёклы
    for grass in ("grass_small", "grass_medium", "grass_tall"):
        rel = f"data/{NS}/loot_table/blocks/{grass}.json"
        table = read(rel, None)
        children = table["pools"][0]["entries"][0]["children"]
        for child in children:
            if child["name"] == "minecraft:wheat_seeds":
                child["name"] = f"{NS}:wheat_seeds"
        if not any(c["name"] == f"{NS}:sugar_beet_seeds" for c in children):
            seeds = next(c for c in children if c["name"] == f"{NS}:wheat_seeds")
            beet = json.loads(json.dumps(seeds))
            beet["name"] = f"{NS}:sugar_beet_seeds"
            for cond in beet["conditions"]:
                if cond["condition"] == "minecraft:random_chance":
                    cond["chance"] = round(cond["chance"] / 3, 4)
            children.append(beet)
        write(rel, table)


def recipes():
    def campfire(name, ingredient, result):
        write(f"data/{NS}/recipe/plants/{name}.json", {
            "type": "minecraft:campfire_cooking", "category": "food", "cookingtime": 600, "experience": 0.1,
            "ingredient": ingredient, "result": {"id": result}})

    def shapeless(name, ingredients, result, count=1):
        write(f"data/{NS}/recipe/plants/{name}.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients,
            "result": {"id": result, "count": count}})

    campfire("dried_seaweed", {"item": f"{NS}:seaweed"}, f"{NS}:dried_seaweed")
    campfire("green_dye", {"item": f"{NS}:cactus"}, f"{NS}:green_dye")
    campfire("bread_from_dough", {"item": "create:dough"}, "minecraft:bread")
    write(f"data/{NS}/recipe/plants/dried_seaweed_block.json", {
        "type": "minecraft:crafting_shaped", "category": "building", "key": {"S": {"item": f"{NS}:dried_seaweed"}},
        "pattern": ["SSS", "SSS", "SSS"], "result": {"id": f"{NS}:dried_seaweed_block", "count": 1}})
    shapeless("dried_seaweed", [{"item": f"{NS}:dried_seaweed_block"}], f"{NS}:dried_seaweed", 9)
    shapeless("sugar_from_cane", [{"item": f"{NS}:sugar_cane"}], "minecraft:sugar")
    shapeless("sugar_from_beet", [{"item": f"{NS}:sugar_beet"}], "minecraft:sugar")
    shapeless("super_glue_from_resin", [{"tag": "c:plates/iron"}, {"tag": "c:nuggets/iron"}, {"item": f"{NS}:resin"}],
              "create:super_glue")


def tags():
    tag("block", f"{NS}:cactus_soils", SANDS + ["#minecraft:sand"])
    tag("block", f"{NS}:cane_soils", SANDS + ["#minecraft:dirt", "#minecraft:sand"])
    tag("block", f"{NS}:cocoa_logs", [f"{NS}:{w}_{k}" for w in ("palm", "ebony") for k in ("log", "wood")])
    tag("block", "minecraft:crops", [f"{NS}:wheat", f"{NS}:sugar_beets"])
    tag("block", "minecraft:mineable/axe", [f"{NS}:cocoa"])
    tag("block", "minecraft:mineable/hoe", [f"{NS}:dried_seaweed_block"])
    tag("item", "c:seeds", [f"{NS}:wheat_seeds", f"{NS}:sugar_beet_seeds"])
    tag("item", "c:crops/wheat", [f"{NS}:wheat"])
    tag("item", "c:dyes/green", [f"{NS}:green_dye"])
    tag("item", "c:dyes", [f"{NS}:green_dye"])
    tag("item", "minecraft:villager_plantable_seeds", [f"{NS}:wheat_seeds", f"{NS}:sugar_beet_seeds"])


def worldgen(client):
    vanilla = lambda rel: client.read(f"data/minecraft/worldgen/{rel}.json").decode("utf-8")
    write(f"data/{NS}/worldgen/configured_feature/patch_seaweed.json", {
        "type": "minecraft:random_patch", "config": {"tries": 24, "xz_spread": 4, "y_spread": 2, "feature": {
            "feature": {"type": "minecraft:simple_block",
                        "config": {"to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{NS}:seaweed"}}}},
            "placement": [{"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:water"},
                {"type": "minecraft:would_survive", "state": {"Name": f"{NS}:seaweed"}}]}}]}}})
    write(f"data/{NS}/worldgen/placed_feature/patch_seaweed.json", {"feature": f"{NS}:patch_seaweed", "placement": [
        {"type": "minecraft:count", "count": 4}, {"type": "minecraft:in_square"},
        {"type": "minecraft:heightmap", "heightmap": "OCEAN_FLOOR_WG"}, {"type": "minecraft:biome"}]})
    for plant, rarity in (("cactus", 6), ("sugar_cane", 4)):
        write(f"data/{NS}/worldgen/configured_feature/patch_{plant}.json", json.loads(swap(vanilla(f"configured_feature/patch_{plant}"))))
        write(f"data/{NS}/worldgen/placed_feature/patch_{plant}.json", {"feature": f"{NS}:patch_{plant}", "placement": [
            {"type": "minecraft:rarity_filter", "chance": rarity}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING"}, {"type": "minecraft:biome"}]})
    write(f"data/{NS}/worldgen/configured_feature/cocoa_pods.json", {"type": f"{NS}:cocoa_pods", "config": {}})
    write(f"data/{NS}/worldgen/placed_feature/cocoa_pods.json", {"feature": f"{NS}:cocoa_pods",
                                                                 "placement": [{"type": "minecraft:biome"}]})
    for plant, biomes in WORLD.items():
        feature = "cocoa_pods" if plant == "cocoa" else f"patch_{plant}"
        write(f"data/{NS}/neoforge/biome_modifier/plants_{plant}.json", {
            "type": "neoforge:add_features", "biomes": biomes, "features": [f"{NS}:{feature}"],
            "step": "vegetal_decoration"})


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


def preview(images):
    scale, pad, per_row = 5, 6, 12
    rows = (len(images) + per_row - 1) // per_row
    sheet = Image.new("RGBA", (pad + per_row * (16 * scale + pad), pad + rows * (16 * scale + pad)), (60, 60, 60, 255))
    for i, img in enumerate(images):
        x, y = i % per_row, i // per_row
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST),
                              (pad + x * (16 * scale + pad), pad + y * (16 * scale + pad)))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "plants.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    images = textures(client)
    models(client)
    loot(client)
    recipes()
    tags()
    worldgen(client)
    lang()
    preview(images)
    print("ok")

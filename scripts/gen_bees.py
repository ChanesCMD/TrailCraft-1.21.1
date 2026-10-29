"""
Пчёлы (пакет bee, init/ModBees.java; простой вариант Forestry): текстуры, модели, блокстейты, лут, рецепты, тэги,
генерация диких ульев, переводы и превью docs/textures/bees.png.

Запуск из корня проекта:  python scripts/gen_bees.py   (нужен Pillow; ванильные текстуры — из jar клиента в кэше
Gradle, как в gen_create_compat.py)

Виды — SPECIES (как bee/BeeSpecies.java, менять вместе): дикие живут в ульях своих биомов (BIOMES; улей стоит на земле,
раз в HIVE_RARITY чанков, biome_modifier bees_<вид>.json), с улья — 1–2 пчелы и медовые соты. Остальные выводятся в
пасеке из двух родителей. Пчела — рисунок BEE (полоски — цвет вида), соты — ванильные соты в своих цветах, воск —
ванильный сгусток слизи в цвете воска, ульи — ванильное гнездо пчёл в оттенке вида, пасека — ванильный улей.
Соты: медовые — бутылка мёда (с пустой бутылкой), на костре — воск, в смесителе Create — мёд и воск; восковые — больше
воска; смоляные — смола; сахарные — сахар. Воск с нитью — свеча.
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
HIVE_RARITY = 24

# вид: (цвет полосок, русское и английское название, дикий ли)
SPECIES = {
    "forest": (0x7A5230, "Лесная пчела", "Forest Bee", True),
    "meadow": (0xE8B830, "Луговая пчела", "Meadow Bee", True),
    "steppe": (0xC89858, "Степная пчела", "Steppe Bee", True),
    "southern": (0xE0782C, "Южная пчела", "Southern Bee", True),
    "marsh": (0x627E3C, "Болотная пчела", "Marsh Bee", True),
    "honey": (0xF5A01A, "Медовая пчела", "Honey Bee", False),
    "wax": (0xF0E2A8, "Восковая пчела", "Wax Bee", False),
    "resin": (0xB8621C, "Смоляная пчела", "Resin Bee", False),
    "sugar": (0xF6F2EA, "Сахарная пчела", "Sugar Bee", False),
    "industrious": (0x4A4A52, "Трудолюбивая пчела", "Industrious Bee", False),
}
BIOMES = {
    "forest": ["burozem_taiga", "pine_forest", "birch_forest", "burozem_forest", "maple_forest", "alder_forest",
               "thuja_forest"],
    "meadow": ["burozem_plains", "mountain_meadow"],
    "steppe": ["chernozem_plains", "chernozem_forest_steppe", "walnut_grove"],
    "southern": ["zheltozem_plains", "savanna", "desert", "red_desert", "eucalyptus_forest", "ebony_forest",
                 "ironwood_forest", "lilac_grove", "sakura_grove"],
    "marsh": ["burozem_willow_forest", "chernozem_willow_forest", "burozem_river", "chernozem_river", "zheltozem_river"],
}
HIVE_NAMES = {"forest": ("лесных", "Forest"), "meadow": ("луговых", "Meadow"), "steppe": ("степных", "Steppe"),
              "southern": ("южных", "Southern"), "marsh": ("болотных", "Marsh")}
WILD_TIPS = {
    "forest": ("Дикая: леса бурозёма — тайга, бор, березняк, широколиственный, кленовый, ольшаник, туевый",
               "Wild: burozem forests"),
    "meadow": ("Дикая: равнины бурозёма, горный луг", "Wild: burozem plains, mountain meadow"),
    "steppe": ("Дикая: равнины чернозёма, лесостепь, ореховая роща", "Wild: chernozem plains, forest steppe, walnut grove"),
    "southern": ("Дикая: равнины, саванна, пустыни и рощи жёлтозёма", "Wild: zheltozem plains, savanna, deserts, groves"),
    "marsh": ("Дикая: ивовые леса, берега рек", "Wild: willow forests, river banks"),
}
# соты: (палитра — от тёмного к светлому, русское и английское название)
COMBS = {
    "honey_comb": ([(120, 62, 8), (178, 98, 14), (222, 140, 24), (246, 184, 52), (252, 222, 120)], "Медовые соты", "Honey Comb"),
    "wax_comb": ([(132, 112, 60), (182, 162, 98), (220, 204, 142), (240, 228, 180), (252, 246, 220)], "Восковые соты", "Wax Comb"),
    "resin_comb": ([(70, 32, 8), (118, 58, 14), (164, 92, 24), (204, 130, 40), (232, 172, 74)], "Смоляные соты", "Resin Comb"),
    "sugar_comb": ([(150, 140, 128), (194, 186, 176), (226, 220, 212), (244, 240, 234), (255, 255, 255)], "Сахарные соты", "Sugar Comb"),
}
WAX = [(150, 120, 40), (196, 166, 70), (226, 200, 104), (242, 224, 146), (252, 242, 196)]
# пчела: B — чёрное, Y — цвет вида, D — темнее, W — крылья, E — глаз, S — жало
BEE = [
    "................",
    "................",
    ".......WW..WW...",
    "......WWWWWWWW..",
    "......WWWWWWWW..",
    ".......WW.WWW...",
    "...BBBYYBBYYBB..",
    "..BEBBYYBBYYBBB.",
    "..BBBBDDBBDDBBBS",
    "..BBBBYYBBYYBBB.",
    "...BBBYYBBYYBB..",
    "....B...B...B...",
    "................",
    "................",
    "................",
    "................",
]

LANG = {
    "block.trailcraft.apiary": ("Пасека", "Apiary"),
    "item.trailcraft.beeswax": ("Пчелиный воск", "Beeswax"),
    "tooltip.trailcraft.bee.bred": ("Выводится в пасеке: %s + %s", "Bred in an apiary: %s + %s"),
    "tooltip.trailcraft.bee.comb": ("Соты: %s", "Combs: %s"),
    "gui.trailcraft.apiary.ok": ("Пчёлы работают", "Bees are working"),
    "gui.trailcraft.apiary.no_bees": ("Нет пчёл", "No bees"),
    "gui.trailcraft.apiary.night": ("Ночь — пчёлы спят", "Night — bees are asleep"),
    "gui.trailcraft.apiary.rain": ("Дождь — пчёлы в улье", "Rain — bees stay in"),
    "gui.trailcraft.apiary.no_sky": ("Над пасекой должно быть небо", "The apiary needs open sky"),
    "gui.trailcraft.apiary.full": ("Выход полон", "Output is full"),
    "jei.trailcraft.apiary.comb": ("Цикл — %s с: днём, без дождя, под открытым небом",
                                   "Cycle: %s s, by day, without rain, under open sky"),
    "jei.trailcraft.apiary.mutation": ("Обе пчелы в пасеке: шанс %s%% за цикл", "Both bees in an apiary: %s%% per cycle"),
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


def rgb(color):
    return (color >> 16) & 255, (color >> 8) & 255, color & 255


def item_model(name):
    write(f"assets/{NS}/models/item/{name}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})


def bee_texture(color):
    y = rgb(color)
    d = tuple(int(c * 0.72) for c in y)
    colors = {"B": (36, 30, 26, 255), "Y": y + (255,), "D": d + (255,), "W": (226, 238, 246, 190),
              "E": (250, 250, 250, 255), "S": (70, 60, 52, 255)}
    img = Image.new("RGBA", (16, 16))
    for yy, row in enumerate(BEE):
        for x, ch in enumerate(row):
            if ch in colors:
                img.putpixel((x, yy), colors[ch])
    return img


def hue_shift(img, color, strength=0.45):
    """Оттенок к цвету вида: тон — цвета вида, насыщенность и яркость — смесь."""
    th, ts, tv = colorsys.rgb_to_hsv(*(c / 255 for c in rgb(color)))
    out = img.convert("RGBA").copy()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = out.getpixel((x, y))
            if a:
                h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
                s = s * (1 - strength) + ts * strength * min(1, s * 2 + 0.2)
                r, g, b = colorsys.hsv_to_rgb(th, min(1, s), v * (0.7 + 0.3 * tv))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def textures(client):
    vanilla = lambda rel: Image.open(BytesIO(client.read(f"assets/minecraft/textures/{rel}.png"))).convert("RGBA")
    previews = []
    for sid, (color, *_rest) in SPECIES.items():
        img = bee_texture(color)
        img.save(os.path.join(TEX, "item", f"{sid}_bee.png"))
        item_model(f"{sid}_bee")
        previews.append(img)
    for comb, (palette, *_rest) in COMBS.items():
        img = recolor(vanilla("item/honeycomb"), palette)
        img.save(os.path.join(TEX, "item", f"{comb}.png"))
        item_model(comb)
        previews.append(img)
    wax = recolor(vanilla("item/slime_ball"), WAX)
    wax.save(os.path.join(TEX, "item", "beeswax.png"))
    item_model("beeswax")
    previews.append(wax)
    for sid, (color, _ru, _en, wild) in SPECIES.items():
        if not wild:
            continue
        for part in ("top", "side", "front", "bottom"):
            img = hue_shift(vanilla(f"block/bee_nest_{part}"), color)
            img.save(os.path.join(TEX, "block", f"{sid}_hive_{part}.png"))
            if part == "front":
                previews.append(img)
    return previews


def blocks():
    for sid, (_c, _ru, _en, wild) in SPECIES.items():
        if not wild:
            continue
        name = f"{sid}_hive"
        # отверстия гнезда со всех сторон: улей не повёрнут
        write(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{NS}:block/{name}_top", "side": f"{NS}:block/{name}_front", "bottom": f"{NS}:block/{name}_bottom"}})
        write(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        bee = {"type": "minecraft:item", "name": f"{NS}:{sid}_bee"}
        write(f"data/{NS}/loot_table/blocks/{name}.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/{name}", "pools": [
            {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [bee]},
            {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [bee], "conditions": [{"condition": "minecraft:random_chance", "chance": 0.4}]},
            {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:honey_comb", "functions": [
                {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 2}}]}]}]})
        # генерация: на земле, где сверху воздух
        write(f"data/{NS}/worldgen/configured_feature/{name}.json", {"type": "minecraft:simple_block", "config": {
            "to_place": {"type": "minecraft:simple_state_provider", "state": {"Name": f"{NS}:{name}"}}}})
        write(f"data/{NS}/worldgen/placed_feature/{name}.json", {"feature": f"{NS}:{name}", "placement": [
            {"type": "minecraft:rarity_filter", "chance": HIVE_RARITY}, {"type": "minecraft:in_square"},
            {"type": "minecraft:heightmap", "heightmap": "MOTION_BLOCKING_NO_LEAVES"},
            {"type": "minecraft:block_predicate_filter", "predicate": {"type": "minecraft:all_of", "predicates": [
                {"type": "minecraft:matching_blocks", "blocks": "minecraft:air"},
                {"type": "minecraft:solid", "offset": [0, -1, 0]}]}},
            {"type": "minecraft:biome"}]})
        write(f"data/{NS}/neoforge/biome_modifier/bees_{sid}.json", {
            "type": "neoforge:add_features", "biomes": [f"{NS}:{b}" for b in BIOMES[sid]],
            "features": [f"{NS}:{name}"], "step": "vegetal_decoration"})
    hives = [f"{NS}:{s}_hive" for s, v in SPECIES.items() if v[3]]
    # пасека — ванильный улей
    write(f"assets/{NS}/models/block/apiary.json", {"parent": "minecraft:block/orientable_with_bottom", "textures": {
        "bottom": "minecraft:block/beehive_end", "top": "minecraft:block/beehive_end",
        "side": "minecraft:block/beehive_side", "front": "minecraft:block/beehive_front_honey",
        "particle": "minecraft:block/beehive_side"}})
    write(f"assets/{NS}/blockstates/apiary.json", {"variants": {
        f"facing={f}": {"model": f"{NS}:block/apiary", **({"y": y} if y else {})}
        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(f"assets/{NS}/models/item/apiary.json", {"parent": f"{NS}:block/apiary"})
    write(f"data/{NS}/loot_table/blocks/apiary.json", {"type": "minecraft:block", "random_sequence": f"{NS}:blocks/apiary", "pools": [
        {"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:apiary"}],
         "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    tag("block", "minecraft:mineable/axe", hives + [f"{NS}:apiary"])


def recipes():
    rel = lambda name: f"data/{NS}/recipe/bees/{name}.json"
    item, tag_ = (lambda i: {"item": i}), (lambda t: {"tag": t})
    shapeless = lambda name, ingredients, result, count=1: write(rel(name), {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients,
        "result": {"id": result, "count": count}})
    campfire = lambda name, ingredient, result, count=1: write(rel(name), {
        "type": "minecraft:campfire_cooking", "category": "misc", "ingredient": item(ingredient),
        "result": {"id": result, "count": count}, "experience": 0.1, "cookingtime": 400})
    mixing = lambda name, ingredient, results: write(rel(name), {
        "type": "create:mixing", "ingredients": [item(ingredient)], "results": results})
    write(rel("apiary"), {"type": "minecraft:crafting_shaped", "category": "misc",
                          "key": {"B": tag_(f"{NS}:small_boards"), "P": tag_("minecraft:planks"), "C": tag_(f"{NS}:combs")},
                          "pattern": ["BBB", "PCP", "PPP"], "result": {"id": f"{NS}:apiary", "count": 1}})
    shapeless("honey_bottle", [item(f"{NS}:honey_comb"), item("minecraft:glass_bottle")], "minecraft:honey_bottle")
    shapeless("resin_from_comb", [item(f"{NS}:resin_comb")], f"{NS}:resin", 2)
    shapeless("sugar_from_comb", [item(f"{NS}:sugar_comb")], "minecraft:sugar", 2)
    shapeless("candle", [item(f"{NS}:beeswax"), tag_("c:strings")], "minecraft:candle")
    campfire("beeswax_from_honey_comb", f"{NS}:honey_comb", f"{NS}:beeswax")
    campfire("beeswax_from_wax_comb", f"{NS}:wax_comb", f"{NS}:beeswax", 2)
    honey = lambda amount: {"id": "create:honey", "amount": amount}
    mixing("honey_comb_mixing", f"{NS}:honey_comb", [honey(250), {"id": f"{NS}:beeswax"}])
    mixing("wax_comb_mixing", f"{NS}:wax_comb", [{"id": f"{NS}:beeswax", "count": 3}, honey(100)])
    mixing("resin_comb_mixing", f"{NS}:resin_comb", [{"id": f"{NS}:resin", "count": 3}])
    mixing("sugar_comb_mixing", f"{NS}:sugar_comb", [{"id": "minecraft:sugar", "count": 3}, honey(100)])
    tag("item", f"{NS}:combs", [f"{NS}:{c}" for c in COMBS])
    tag("item", f"{NS}:bees", [f"{NS}:{s}_bee" for s in SPECIES])


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        path_ = f"assets/{NS}/lang/{code}.json"
        data = read(path_, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        for sid, (_c, ru, en, wild) in SPECIES.items():
            data[f"item.{NS}.{sid}_bee"] = (ru, en)[k]
            if wild:
                ru_h, en_h = HIVE_NAMES[sid]
                data[f"block.{NS}.{sid}_hive"] = (f"Улей {ru_h} пчёл", f"{en_h} Bee Hive")[k]
                data[f"tooltip.{NS}.bee.wild.{sid}"] = WILD_TIPS[sid][k]
        for comb, (_p, ru, en) in COMBS.items():
            data[f"item.{NS}.{comb}"] = (ru, en)[k]
        write(path_, data)


def preview(images):
    scale, pad, per_row = 6, 6, 10
    cell = 16 * scale
    rows = (len(images) + per_row - 1) // per_row
    sheet = Image.new("RGBA", (pad + per_row * (cell + pad), pad + rows * (cell + pad)), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((cell, cell), Image.NEAREST),
                              (pad + i % per_row * (cell + pad), pad + i // per_row * (cell + pad)))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "bees.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    images = textures(client)
    blocks()
    recipes()
    lang()
    preview(images)
    print("ok")

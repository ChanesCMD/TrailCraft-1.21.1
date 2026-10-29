"""
Create под мир TrailCraft: что из рецептов Create убрать, какие общие тэги заполнить нашими предметами, свои рецепты
вместо ванильных и перекрашенный цинк Create.

Запуск из корня проекта:  python scripts/gen_create_compat.py   (нужен Pillow; jar Create и клиент Minecraft берутся из
кэша Gradle — сначала один раз собрать проект)

1. Тэги (replace: true — ванильные предметы из них уходят, JEI показывает наши): палки c:rods/wooden — обработанная
   палка; камень c:stones — наши породы; булыжник c:cobblestones — наш булыжник; песок c:sands/colorless и c:sands/red —
   наш песок; окорённые брёвна и древесина c:stripped_logs, c:stripped_woods — наши; шпалы create:sleepers — плиты пород;
   брёвна и доски minecraft:logs, minecraft:planks (предметы) — только наши. Рецепты Create по этим тэгам (длиннохват,
   деревянная скоба, рамка, картонный меч, парус, жёрнов, повторители импульса, липучка, корпуса, наждачка, шасси,
   ж/д путь…) так сами берут наше.
2. data/trailcraft/pruned_recipes.json — список для RecipePruner: рецепты обработки Create (дробление, помол,
   промывка, прессование, смешивание, призрачный огонь, плавка…), где ингредиент — только ванильные предметы, которых в
   мире нет (ванильные породы, руды, земля, песок, дерево, незер и край — DENY), рецепты с пустыми тэгами, окна Create из
   ванильных досок и рецепты Create, вместо которых свои (REPLACED). Ванильные печь и доменную печь RecipePruner убирает
   сам, по типу рецепта.
3. Свои рецепты (data/trailcraft/recipe/create_compat/): блоки меди, железа, золота из 9 слитков и обратно, кусочки
   железа и золота (медные — рецепты Create); андезитовый сплав из нашего андезита (верстак и смешивание, с железом и
   цинком); механический сборщик из наших верстаков; редстоуновый контакт из нашего булыжника; парус из тканевого
   полотна; на костре — стекло из песка (временно) и гладкий камень из любой породы; рычаг, редстоуновый факел,
   повторитель, компаратор из наших пород и палок; громоотвод из меди; удобрение для деревьев из кости, золы и листвы;
   жёрнов из гравия; наблюдатель; железные прутья и люк; компас из магнетита; лук из обработанных палок и нитей,
   раздатчик из булыжника; костная мука из нашей кости; жезл симметрии с аметистом вместо жемчуга (SUBSTITUTED); еда на костре и в коптильне
   (FOOD, recipe/food/); красители из цветов и смесей — копии ванильных (recipe/dyes/); дробление наших кусков и блоков
   руды в дроблёную руду Create (CRUSHED, recipe/crushing/; у гематита шанс редстоуна), киноварь — в редстоун.
4. Цинк Create (слиток, кусочек, дроблёная руда, блок) — формы Create в гамме цинка METALS["zinc"] из
   gen_ore_textures.py (как шихта цинка), по яркости. Лежит в assets/create/…:
   ресурсы TrailCraft грузятся после Create (ordering = AFTER в neoforge.mods.toml).
"""
import glob
import json
import os
import re
import zipfile
from io import BytesIO

from PIL import Image

from gen_ore_textures import METALS

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
GRADLE = os.path.join(os.path.expanduser("~"), ".gradle", "caches")
ROCKS = ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite", "gray_granite", "limestone",
         "marble", "sandstone", "shale"]
VANILLA_WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "bamboo", "crimson",
                 "warped"]

# Ванильные предметы, которых в мире TrailCraft нет: рецепты обработки Create из них убираются
DENY = re.compile(r"^minecraft:(" + "|".join([
    r"stone", r"cobblestone", r"mossy_.*", r"andesite", r"diorite", r"granite", r"calcite", r"tuff.*", r".*deepslate.*",
    r"dripstone_block", r"pointed_dripstone", r"gravel", r"sand", r"red_sand", r".*sandstone.*", r".*terracotta",
    r"clay", r"clay_ball", r"mud", r"packed_mud", r"dirt", r"coarse_dirt", r"rooted_dirt", r"podzol", r"mycelium",
    r"grass_block", r"netherrack", r"soul_.*", r".*blackstone.*", r"basalt", r"smooth_basalt", r"end_stone.*",
    r".*obsidian", r"magma_.*", r"glowstone.*", r".*quartz.*", r"nether_.*", r".*_ore", r"raw_(iron|copper|gold).*",
    r"ancient_debris", r"netherite.*", r"prismarine.*", r".*coral.*", r"sea_pickle", r".*amethyst.*", r"lapis.*",
    r"emerald.*", r"diamond.*", r"chorus.*", r"shulker.*", r"ender_.*", r"blaze_.*", r"ghast_tear", r"wither.*",
    r"stripped_.*", r".*stone_bricks", r"cracked_.*", r"infested_.*", r"coal", r"coal_block", r"charcoal",
    r"(" + "|".join(VANILLA_WOODS) + r")_.*",
]) + r")$")
# Рецепты Create, вместо которых свои (create_compat/*)
REPLACED = [
    "create:crafting/materials/andesite_alloy", "create:crafting/materials/andesite_alloy_from_zinc",
    "create:mixing/andesite_alloy", "create:mixing/andesite_alloy_from_zinc",
    "create:crafting/kinetics/mechanical_crafter", "create:crafting/logistics/redstone_contact",
    "create:crafting/appliances/tree_fertilizer", "create:crafting/tree_fertilizer",
    "create:crafting/kinetics/millstone",
    # прочный лист Create — лавой из заливки; свой рецепт (без лавы) — gen_reinforced_pipe.py
    "create:sequenced_assembly/sturdy_sheet",
    # блок киновари (c:ores/quicksilver) — свой рецепт, с редстоуном
    "create:crushing/quicksilver_ore",
    # убраны по просьбе автора: ванильные трава и семена, сёдла и конская броня, кварц из диорита, блок рудного
    # цинка, бумага из тростника
    "create:milling/short_grass", "create:milling/tall_grass", "create:milling/fern", "create:milling/large_fern",
    "create:milling/beetroot", "create:milling/saddle", "create:crushing/leather_horse_armor",
    "create:crushing/iron_horse_armor", "create:crushing/golden_horse_armor", "create:crushing/diorite_recycling",
    "create:crafting/materials/raw_zinc_block", "create:crafting/materials/raw_zinc", "create:pressing/sugar_cane",
] + [f"create:crushing/raw_{m}" for m in ("copper", "iron", "gold", "zinc", "tin", "lead", "nickel", "aluminum")]
# Дробление наших кусков руды и блоков руды (с шёлковым касанием): руда → дроблёная руда Create (+ побочное с шансом);
# рецепты Create по тэгам c:raw_materials/* заменены этими, чтобы у куска был один рецепт
CRUSHED = {
    "malachite": ("copper", []), "native_copper": ("copper", []), "tetrahedrite": ("copper", []),
    "hematite": ("iron", [("minecraft:redstone", 0.3)]), "magnetite": ("iron", []), "limonite": ("iron", []),
    "sphalerite": ("zinc", []), "cassiterite": ("tin", []), "galena": ("lead", []), "native_gold": ("gold", []),
    "pentlandite": ("nickel", []), "bauxite": ("aluminum", []), "nepheline": ("aluminum", []),
}
# Рецепты Create, в которых меняются ингредиенты: id → {что: на что}; копия ложится в create_compat/, оригинал уходит
KELP = {"minecraft:dried_kelp": f"{NS}:dried_seaweed"}
SUBSTITUTED = {
    "create:mechanical_crafting/wand_of_symmetry": {"#c:ender_pearls": "minecraft:amethyst_shard"},
    # свои растения (scripts/gen_plants.py)
    **{f"create:crafting/{r}": KELP for r in ("kinetics/belt_connector", "kinetics/spout", "logistics/andesite_funnel",
                                              "logistics/andesite_tunnel", "logistics/brass_funnel", "logistics/brass_tunnel")},
    "create:crafting/kinetics/elevator_pulley": {"minecraft:dried_kelp_block": f"{NS}:dried_seaweed_block"},
    "create:crafting/kinetics/hose_pulley": {"minecraft:dried_kelp_block": f"{NS}:dried_seaweed_block"},
    "create:milling/wheat": {"minecraft:wheat": f"{NS}:wheat", "minecraft:wheat_seeds": f"{NS}:wheat_seeds"},
    "create:milling/sugar_cane": {"minecraft:sugar_cane": f"{NS}:sugar_cane"},
    "create:mixing/chocolate": {"minecraft:cocoa_beans": f"{NS}:cocoa_beans"},
    "create:milling/cocoa_beans": {"minecraft:cocoa_beans": f"{NS}:cocoa_beans"},
    # хронометр вместо ванильных часов (их рецепта нет)
    "create:crafting/kinetics/cuckoo_clock": {"minecraft:clock": f"{NS}:chronometer"},
    "create:sequenced_assembly/precision_mechanism": {"minecraft:clock": f"{NS}:chronometer"},
    # наши ягоды и яблоко (scripts/gen_animal_goods.py, gen_farm.py)
    "create:filling/chocolate_glazed_berries": {"minecraft:sweet_berries": f"#{NS}:berries"},
    "create:filling/honeyed_apple": {"minecraft:apple": f"{NS}:apple"},
}
# Ванильные красители: что заменить своим, какие не копировать (свёкла у нас сахарная, белая)
DYE_SWAPS = {"minecraft:cocoa_beans": f"{NS}:cocoa_beans"}
DYE_SKIP = {"red_dye_from_beetroot"}
# Ванильная еда на костре и в коптильне: свои рецепты (ванильные RecipePruner убирает)
FOOD = {"potato": "baked_potato", "beef": "cooked_beef", "chicken": "cooked_chicken", "mutton": "cooked_mutton",
        "porkchop": "cooked_porkchop", "rabbit": "cooked_rabbit", "cod": "cooked_cod", "salmon": "cooked_salmon"}
# Папки Create, где рецепты обработки (их убираем по DENY); верстак и механический сборщик правятся вручную
PROCESSING = ("blasting", "smelting", "smoking", "campfire_cooking", "compacting", "crushing", "cutting", "deploying",
              "emptying", "filling", "haunting", "item_application", "milling", "mixing", "pressing",
              "sandpaper_polishing", "splashing")


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


def find_jar(pattern):
    jars = sorted(glob.glob(os.path.join(GRADLE, "**", pattern), recursive=True))
    if not jars:
        raise SystemExit(f"Не найден {pattern} в кэше Gradle: сначала соберите проект (gradlew build)")
    return jars[-1]


# ---------------------------------------------------------------- 1. тэги
def items_of(tag_rel):
    return [v for v in read(tag_rel, {"values": []})["values"]]


def tags(external):
    stripped_logs = items_of(f"data/{NS}/tags/item/stripped_logs.json")
    stripped_woods = [v.replace("_log", "_wood") for v in stripped_logs
                      if os.path.exists(path(f"assets/{NS}/models/item/{v.split(':')[1].replace('_log', '_wood')}.json"))]
    replaced = {
        "c:rods/wooden": [f"{NS}:stick_processed"],
        "c:stones": [f"{NS}:{r}" for r in ROCKS],
        "c:cobblestones": [f"{NS}:{r}_cobblestone" for r in ROCKS],
        "c:sands/colorless": [f"{NS}:sand_white", f"{NS}:sand_yellow", f"{NS}:sand_gray"],
        "c:sands/red": [f"{NS}:sand_red"],
        "c:stripped_logs": stripped_logs,
        "c:stripped_woods": stripped_woods,
        "create:sleepers": [f"{NS}:{r}_slab" for r in ROCKS],
        "c:leathers": [f"{NS}:leather"],
        "c:eggs": [f"{NS}:egg"],
        "c:barrels/wooden": [f"{NS}:water_barrel"],
    }
    for tag_id, values in replaced.items():
        ns, name = tag_id.split(":")
        # тэги NeoForge грузятся после наших, replace их не задевает — ванильное убирает remove (NeoForge)
        remove = sorted(i for i in expand(external, tag_id) if i not in values)
        data = {"replace": True, "values": values}
        if remove:
            data["remove"] = remove
        write(f"data/{ns}/tags/item/{name}.json", data)
    for name in ("logs", "planks"):  # только наши брёвна и доски, без ванильных
        rel = f"data/minecraft/tags/item/{name}.json"
        data = read(rel, {"values": []})
        write(rel, {"replace": True, "values": data["values"]})
    # предметы пород и гравия одним тэгом (рычаг, повторитель, гладкий камень, жёрнов)
    write(f"data/{NS}/tags/item/rocks.json", {"replace": False, "values": [f"{NS}:{r}" for r in ROCKS]})
    write(f"data/{NS}/tags/item/berries.json", {"replace": False, "values": [
        f"{NS}:{b}" for b in ("currant", "cloudberry", "gooseberry", "blackberry")]})
    write(f"data/{NS}/tags/item/gravels.json", {"replace": False, "values": [f"{NS}:{r}_gravel" for r in ROCKS]})


# ---------------------------------------------------------------- 2. что убрать
def load_tags(zips, ours=True):
    """Тэги предметов: ванильные, NeoForge, Create, потом наши (ours; наши могут заменять — replace)."""
    tags = {}

    def add(key, data):
        vals = [v if isinstance(v, str) else v["id"] for v in data.get("values", [])]
        tags[key] = vals if data.get("replace") else tags.get(key, []) + vals

    for z in zips:
        for name in z.namelist():
            m = re.match(r"data/([^/]+)/tags/items?/(.+)\.json$", name)
            if m:
                add(f"{m.group(1)}:{m.group(2)}", json.loads(z.read(name)))
    for f in glob.glob(os.path.join(ROOT, "data", "*", "tags", "item", "**", "*.json"), recursive=True) if ours else []:
        rel = os.path.relpath(f, os.path.join(ROOT, "data")).replace(os.sep, "/")
        ns, name = rel.split("/")[0], rel.split("/tags/item/")[1][:-5]
        with open(f, encoding="utf-8") as fh:
            add(f"{ns}:{name}", json.load(fh))
    return tags


def expand(tags, tag, seen=None):
    seen = seen or set()
    if tag in seen:
        return set()
    seen.add(tag)
    out = set()
    for v in tags.get(tag, []):
        out |= expand(tags, v[1:], seen) if v.startswith("#") else {v}
    return out


def ingredients(obj):
    """Ингредиенты рецепта: множества предметов, из которых можно выбрать (результаты не считаются)."""
    if isinstance(obj, dict):
        if "amount" in obj or obj.get("type") == "neoforge:block_tag":
            return  # жидкость (вода, молоко, мёд) или тэг блоков — не предметы
        if isinstance(obj.get("item"), str):
            yield [("item", obj["item"])]
        elif isinstance(obj.get("tag"), str):
            yield [("tag", obj["tag"])]
        else:
            for k, v in obj.items():
                if k not in ("result", "results", "id"):
                    yield from ingredients(v)
    elif isinstance(obj, list):
        if obj and all(isinstance(x, dict) and ("item" in x or "tag" in x) and "amount" not in x for x in obj):
            yield [("item", x["item"]) if "item" in x else ("tag", x["tag"]) for x in obj]
        else:
            for x in obj:
                yield from ingredients(x)


def pruned(create, tags):
    out = set(REPLACED) | set(SUBSTITUTED)
    for name in create.namelist():
        m = re.match(r"data/create/recipe/(.+)\.json$", name)
        if not m or "/compat/" in name:
            continue
        rid = m.group(1)
        if rid.split("/")[0] not in PROCESSING and "/" in rid:
            continue  # верстак и механический сборщик — вручную
        data = json.loads(create.read(name))
        result = json.dumps(data.get("result", data.get("results", "")))
        if re.search(r'"create:(' + "|".join(VANILLA_WOODS) + r')_window', result):
            out.add(f"create:{rid}")
            continue
        for options in ingredients(data):
            items = set()
            for kind, v in options:
                items |= expand(tags, v) if kind == "tag" else {v}
            if not items or all(DENY.match(i) for i in items):
                out.add(f"create:{rid}")
                break
    return sorted(out)


# ---------------------------------------------------------------- 3. свои рецепты
def shaped(name, pattern, key, result, count=1):
    write(f"data/{NS}/recipe/create_compat/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "misc", "key": key, "pattern": pattern,
        "result": {"id": result, "count": count}})


def shapeless(name, ingredients_, result, count=1):
    write(f"data/{NS}/recipe/create_compat/{name}.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": ingredients_,
        "result": {"id": result, "count": count}})


def campfire(name, ingredient, result, time=600):
    write(f"data/{NS}/recipe/create_compat/{name}.json", {
        "type": "minecraft:campfire_cooking", "category": "misc", "ingredient": ingredient,
        "result": {"id": result}, "experience": 0.1, "cookingtime": time})


def recipes():
    item, tag = (lambda i: {"item": i}), (lambda t: {"tag": t})
    for metal in ("copper", "iron", "gold"):
        ingot = f"minecraft:{metal}_ingot"
        shaped(f"{metal}_block", ["III", "III", "III"], {"I": item(ingot)}, f"minecraft:{metal}_block")
        shapeless(f"{metal}_ingot_from_block", [item(f"minecraft:{metal}_block")], ingot, 9)
        if metal != "copper":  # медные кусочки — рецепты Create
            shapeless(f"{metal}_nugget", [item(ingot)], f"minecraft:{metal}_nugget", 9)
            shaped(f"{metal}_ingot_from_nuggets", ["NNN", "NNN", "NNN"], {"N": item(f"minecraft:{metal}_nugget")}, ingot)
    andesite = item(f"{NS}:andesite")
    for metal in ("iron", "zinc"):
        suffix = "" if metal == "iron" else "_from_zinc"
        shaped(f"andesite_alloy{suffix}", ["NA", "AN"], {"A": andesite, "N": tag(f"c:nuggets/{metal}")},
               "create:andesite_alloy")
        write(f"data/{NS}/recipe/create_compat/andesite_alloy_mixing{suffix}.json", {
            "type": "create:mixing", "ingredients": [andesite, tag(f"c:nuggets/{metal}")],
            "results": [{"id": "create:andesite_alloy"}]})
    shaped("mechanical_crafter", ["B", "C", "R"], {"B": item("create:electron_tube"), "C": item("create:brass_casing"),
                                                    "R": tag(f"{NS}:workbenches")}, "create:mechanical_crafter", 3)
    shaped("redstone_contact", [" S ", "CWC", "CCC"], {"C": tag("c:cobblestones"), "S": tag("c:plates/iron"),
                                                      "W": tag("c:dusts/redstone")}, "create:redstone_contact", 2)
    shaped("white_sail_from_cloth", ["WS", "SA"], {"W": item(f"{NS}:cloth"), "S": tag("c:rods/wooden"),
                                                  "A": item("create:andesite_alloy")}, "create:white_sail", 2)
    campfire("glass_from_sand", tag("c:sands"), "minecraft:glass")
    campfire("smooth_stone", tag(f"{NS}:rocks"), "minecraft:smooth_stone")
    rocks, stick = tag(f"{NS}:rocks"), item(f"{NS}:stick_processed")
    redstone, torch = tag("c:dusts/redstone"), item("minecraft:redstone_torch")
    shaped("lever", ["S", "R"], {"S": stick, "R": rocks}, "minecraft:lever")
    shaped("redstone_torch", ["R", "S"], {"R": redstone, "S": stick}, "minecraft:redstone_torch")
    shaped("repeater", ["TRT", "SSS"], {"T": torch, "R": redstone, "S": rocks}, "minecraft:repeater")
    shaped("comparator", [" T ", "TQT", "SSS"], {"T": torch, "Q": tag("c:gems/quartz"), "S": rocks},
           "minecraft:comparator")
    shaped("lightning_rod", ["C", "C", "C"], {"C": item("minecraft:copper_ingot")}, "minecraft:lightning_rod")
    shapeless("tree_fertilizer", [tag("c:bones"), item(f"{NS}:ash"), item(f"{NS}:ash"), tag("minecraft:leaves")],
              "create:tree_fertilizer", 2)
    shaped("millstone", ["C", "S", "G"], {"C": item("create:cogwheel"), "S": item("create:andesite_casing"),
                                          "G": tag(f"{NS}:gravels")}, "create:millstone")
    cobble, quartz = tag("c:cobblestones"), tag("c:gems/quartz")
    shaped("observer", ["CCC", "RRQ", "CCC"], {"C": cobble, "R": redstone, "Q": quartz}, "minecraft:observer")
    iron = item("minecraft:iron_ingot")
    shaped("iron_bars", ["III", "III"], {"I": iron}, "minecraft:iron_bars", 16)
    shaped("iron_trapdoor", ["II", "II"], {"I": iron}, "minecraft:iron_trapdoor")
    shaped("compass", [" I ", "IMI", " I "], {"I": iron, "M": item(f"{NS}:raw_magnetite")}, "minecraft:compass")
    shaped("bow", [" SR", "S R", " SR"], {"S": stick, "R": tag("c:strings")}, "minecraft:bow")
    shaped("dispenser", ["CCC", "CBC", "CRC"], {"C": cobble, "B": item("minecraft:bow"), "R": redstone},
           "minecraft:dispenser")
    shapeless("bone_meal", [tag("c:bones")], "minecraft:bone_meal", 3)
    for ore, (metal, extra) in CRUSHED.items():
        side = [{"id": i, "chance": c} for i, c in extra]
        write(f"data/{NS}/recipe/crushing/raw_{ore}.json", {
            "type": "create:crushing", "ingredients": [item(f"{NS}:raw_{ore}")], "processing_time": 400,
            "results": [{"id": f"create:crushed_raw_{metal}"}, {"id": "create:experience_nugget", "chance": 0.75}] + side})
        write(f"data/{NS}/recipe/crushing/{ore}_ore.json", {
            "type": "create:crushing", "ingredients": [item(f"{NS}:{ore}_ore")], "processing_time": 250,
            "results": [{"id": f"create:crushed_raw_{metal}"}, {"id": f"create:crushed_raw_{metal}", "chance": 0.75},
                        {"id": "create:experience_nugget", "chance": 0.75}] + side})
    # киноварь — в редстоун: дробилка, жёрнов; дробилка даёт ещё дроблёную рудную ртуть Create
    quicksilver = "create:crushed_raw_quicksilver"
    write(f"data/{NS}/recipe/crushing/raw_cinnabar.json", {
        "type": "create:crushing", "ingredients": [item(f"{NS}:raw_cinnabar")], "processing_time": 250,
        "results": [{"id": "minecraft:redstone", "count": 3}, {"id": "minecraft:redstone", "chance": 0.5},
                    {"id": quicksilver}]})
    write(f"data/{NS}/recipe/crushing/cinnabar_ore.json", {
        "type": "create:crushing", "ingredients": [item(f"{NS}:cinnabar_ore")], "processing_time": 250,
        "results": [{"id": quicksilver}, {"id": quicksilver, "chance": 0.75}, {"id": "minecraft:redstone", "count": 3},
                    {"id": "create:experience_nugget", "chance": 0.75}]})
    write(f"data/{NS}/recipe/milling/raw_cinnabar.json", {
        "type": "create:milling", "ingredients": [item(f"{NS}:raw_cinnabar")], "processing_time": 150,
        "results": [{"id": "minecraft:redstone", "count": 3}]})
    for raw, cooked in FOOD.items():
        for kind, time in (("campfire_cooking", 600), ("smoking", 100)):
            write(f"data/{NS}/recipe/food/{cooked}_from_{kind}.json", {
                "type": f"minecraft:{kind}", "category": "food", "cookingtime": time, "experience": 0.35,
                "ingredient": {"item": f"minecraft:{raw}"}, "result": {"id": f"minecraft:{cooked}"}})


def dyes(vanilla):
    """Красители из цветов и смеси красителей — копии ванильных рецептов верстака (ванильные RecipePruner убирает)."""
    for name in vanilla.namelist():
        m = re.match(r"data/minecraft/recipe/(\w+_dye(_from_\w+)?)\.json$", name)
        if not m or name.endswith("armor_dye.json") or m.group(1) in DYE_SKIP:
            continue
        text = vanilla.read(name).decode("utf-8")
        for old, new in DYE_SWAPS.items():
            text = text.replace(f'"{old}"', f'"{new}"')
        data = json.loads(text)
        if data["type"] not in ("minecraft:crafting_shaped", "minecraft:crafting_shapeless"):
            continue
        used = re.findall(r'"(minecraft:[a-z_]+)"', json.dumps({k: v for k, v in data.items() if k != "result"}))
        if any(DENY.match(i) for i in used):
            continue
        write(f"data/{NS}/recipe/dyes/{m.group(1)}.json", data)


def substitute(create):
    """Копии рецептов Create с заменёнными ингредиентами (SUBSTITUTED)."""
    for rid, swaps in SUBSTITUTED.items():
        text = create.read(f"data/create/recipe/{rid.split(':')[1]}.json").decode("utf-8")
        for old, new in swaps.items():
            if old.startswith("#"):  # тэг → предмет или тэг
                new_key = "tag" if new.startswith("#") else "item"
                text = re.sub(r'"tag"\s*:\s*"%s"' % re.escape(old[1:]), '"%s": "%s"' % (new_key, new.lstrip("#")), text)
            elif new.startswith("#"):  # предмет → тэг (только в ингредиентах)
                text = re.sub(r'"item"\s*:\s*"%s"' % re.escape(old), '"tag": "%s"' % new[1:], text)
            else:  # предмет — везде, и в ингредиентах, и в результатах
                text = text.replace(f'"{old}"', f'"{new}"')
        write(f"data/{NS}/recipe/create_compat/{rid.split(':')[1].replace('/', '_')}.json", json.loads(text))


# ---------------------------------------------------------------- 4. цинк
def recolor(img, palette):
    """Перекрасить по яркости: тёмные пиксели — в тёмные цвета палитры, светлые — в светлые."""
    img = img.convert("RGBA")
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    pixels = [c for c in img.get_flattened_data() if c[3]]
    lo, hi = min(map(lum, pixels)), max(map(lum, pixels))
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3]:
                k = (lum(c) - lo) / max(1, hi - lo)
                p = palette[min(len(palette) - 1, int(k * len(palette)))]
                out.putpixel((x, y), p[:3] + (c[3],))
    return out


def zinc(create):
    palette = METALS["zinc"]
    out = os.path.join(ROOT, "assets", "create", "textures")
    os.makedirs(os.path.join(out, "item"), exist_ok=True)
    os.makedirs(os.path.join(out, "block"), exist_ok=True)
    for rel in ("item/zinc_ingot.png", "item/zinc_nugget.png", "item/crushed_raw_zinc.png", "block/zinc_block.png"):
        src = Image.open(BytesIO(create.read(f"assets/create/textures/{rel}")))
        recolor(src, palette).save(os.path.join(out, *rel.split("/")))


if __name__ == "__main__":
    create = zipfile.ZipFile(find_jar("create-1.21.1-*-slim.jar"))
    vanilla = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    neoforge = zipfile.ZipFile(find_jar("neoforge-21.1.*-universal.jar"))
    tags(load_tags([vanilla, neoforge, create], ours=False))
    recipes()
    dyes(vanilla)
    substitute(create)
    write(f"data/{NS}/pruned_recipes.json", pruned(create, load_tags([vanilla, neoforge, create])))
    zinc(create)
    print("ok")

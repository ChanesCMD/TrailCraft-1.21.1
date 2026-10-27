"""Малые доски и верстаки из досок каждой породы (ModWood: <порода>_small_board, <порода>_crafting_table).

Пишет: текстуры (верстак — ванильный, перекрашенный в цвет досок породы, полотно сверху — в цвет соломы;
малая доска — по шаблону), модели, блокстейты, лут, тэги, рецепты (доски + пила → 4 малые доски, тонкое бревно + пила → 1; 4 малые доски → верстак),
переводы. Убирает ванильный верстак из 4 любых досок (bootstrap_crafting_table). Нужен Pillow.
Запуск: python scripts/gen_workbench.py
"""
import json
import os
import re

from PIL import Image

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
NS = "trailcraft"

WOODS = ["oak", "birch", "acacia", "sakura", "eucalyptus", "chestnut", "thuja", "pine", "fir", "spruce", "white_poplar",
         "ebony", "maple", "cedar", "willow", "lilac", "palm", "ironwood", "aspen", "linden", "pussy_willow", "walnut",
         "alder"]

# Малая доска: брусок по диагонали; буквы — оттенки цвета досок (l — светлее, m — основной, d — темнее, o — контур)
BOARD_SHAPE = [
    "................",
    "................",
    "...........oo...",
    "..........olmo..",
    ".........olmmdo.",
    "........olmmdo..",
    ".......olmmdo...",
    "......olmmdo....",
    ".....olmmdo.....",
    "....olmmdo......",
    "...olmmdo.......",
    "..olmmdo........",
    ".olmmdo.........",
    ".omddo..........",
    "..ooo...........",
    "................",
]
BOARD_SHADES = {"l": 1.2, "m": 1.0, "d": 0.75, "o": 0.45}


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


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def png(rel, im):
    p = path("assets", NS, "textures", *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p + ".png")


def planks(wood):
    return Image.open(path("assets", NS, "textures", "block", f"{wood}_planks.png")).convert("RGBA")


def average(im):
    px = [p for p in im.getdata() if p[3]]
    return tuple(sum(p[i] for p in px) // len(px) for i in range(3))


def darken(im, x, y, k):
    r, g, b, a = im.getpixel((x, y))
    im.putpixel((x, y), shade((r, g, b), k) + (a,))


# Верстак — ванильные текстуры (scripts/templates/crafting_table_*.png), перекрашенные: дерево — в цвет досок породы
# (по яркости относительно основного цвета ванильных досок), полотно на столешнице — в цвет соломы, металл не трогаем
TEMPLATES = os.path.join(os.path.dirname(__file__), "templates")
VANILLA_WOOD = (175, 143, 85)
VANILLA_MAT = (174, 105, 60)
STRAW = (0xD2, 0xC6, 0x76)
TOP_FRAME = {(188, 152, 98), (25, 20, 12), (26, 21, 13)}  # дерево по краю столешницы, остальное на верху — полотно


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor(px, ref, color):
    return shade(color, lum(px) / lum(ref))


def is_metal(px):
    r, g, b = px[:3]
    return max(r, g, b) - min(r, g, b) < 12 and max(r, g, b) > 120  # серый и белый — инструменты


def from_template(name, wood_color, mat=False):
    src = Image.open(os.path.join(TEMPLATES, f"crafting_table_{name}.png")).convert("RGBA")
    im = Image.new("RGBA", src.size, (0, 0, 0, 0))
    for y in range(src.height):
        for x in range(src.width):
            px = src.getpixel((x, y))
            if px[3] == 0 or is_metal(px):
                im.putpixel((x, y), px)
            elif mat and px[:3] not in TOP_FRAME:
                im.putpixel((x, y), recolor(px, VANILLA_MAT, STRAW) + (px[3],))
            else:
                im.putpixel((x, y), recolor(px, VANILLA_WOOD, wood_color) + (px[3],))
    return im


def board(color):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(BOARD_SHAPE):
        for x, ch in enumerate(row):
            if ch != ".":
                k = BOARD_SHADES[ch] * (0.92 if ch == "m" and (x + 2 * y) % 5 == 0 else 1.0)  # волокна
                im.putpixel((x, y), shade(color, k) + (255,))
    return im


def textures():
    for wood in WOODS:
        base = planks(wood)
        color = average(base)
        png(f"block/{wood}_crafting_table_top", from_template("top", color, mat=True))
        png(f"block/{wood}_crafting_table_side", from_template("side", color))
        png(f"block/{wood}_crafting_table_front", from_template("front", color))
        png(f"item/{wood}_small_board", board(average(base)))


def assets():
    for wood in WOODS:
        t = f"{NS}:block/{wood}_crafting_table"
        write(f"assets/{NS}/models/block/{wood}_crafting_table.json", {
            "parent": "minecraft:block/cube", "textures": {
                "particle": f"{t}_front", "north": f"{t}_front", "south": f"{t}_side", "east": f"{t}_side",
                "west": f"{t}_front", "up": f"{t}_top", "down": f"{NS}:block/{wood}_planks"}})
        write(f"assets/{NS}/blockstates/{wood}_crafting_table.json",
              {"variants": {"": {"model": f"{NS}:block/{wood}_crafting_table"}}})
        write(f"assets/{NS}/models/item/{wood}_crafting_table.json", {"parent": f"{NS}:block/{wood}_crafting_table"})
        write(f"assets/{NS}/models/item/{wood}_small_board.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{wood}_small_board"}})
        write(f"data/{NS}/loot_table/blocks/{wood}_crafting_table.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                       "entries": [{"type": "minecraft:item", "name": f"{NS}:{wood}_crafting_table"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{wood}_crafting_table"})


def data():
    tables = [f"{NS}:{w}_crafting_table" for w in WOODS]
    tag("block", "minecraft:mineable/axe", tables)
    tag("item", f"{NS}:workbenches", tables)
    tag("item", f"{NS}:small_boards", [f"{NS}:{w}_small_board" for w in WOODS])
    for wood in WOODS:
        write(f"data/{NS}/recipe/{wood}_small_board.json", {
            "type": "minecraft:crafting_shapeless", "category": "building",
            "ingredients": [{"item": f"{NS}:{wood}_planks"}, {"tag": f"{NS}:saws"}],
            "result": {"id": f"{NS}:{wood}_small_board", "count": 4}})
        write(f"data/{NS}/recipe/{wood}_small_board_from_thin_log.json", {
            "type": "minecraft:crafting_shapeless", "category": "building",
            "ingredients": [{"item": f"{NS}:thin_{wood}_log"}, {"tag": f"{NS}:saws"}],
            "result": {"id": f"{NS}:{wood}_small_board", "count": 1}})
        write(f"data/{NS}/recipe/{wood}_crafting_table.json", {
            "type": "minecraft:crafting_shaped", "category": "misc",
            "key": {"#": {"item": f"{NS}:{wood}_small_board"}}, "pattern": ["##", "##"],
            "result": {"id": f"{NS}:{wood}_crafting_table", "count": 1}})
    # ванильный верстак из любых досок больше не крафтится — только верстаки пород
    old = path("data", NS, "recipe", "bootstrap_crafting_table.json")
    if os.path.exists(old):
        os.remove(old)
    # малый карман — 3 тканевых полотна
    write(f"data/{NS}/recipe/small_pocket.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [{"item": f"{NS}:cloth"}] * 3, "result": {"id": f"{NS}:small_pocket", "count": 1}})


def lang():
    for code in ("ru_ru", "en_us"):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for wood in WOODS:
            name = data[f"block.{NS}.{wood}_planks"]
            if code == "ru_ru":
                of = re.sub(r"^Доски ", "", name)          # «Доски дуба» → «дуба»
                data[f"item.{NS}.{wood}_small_board"] = f"Малая доска {of}"
                data[f"block.{NS}.{wood}_crafting_table"] = f"Верстак из {of}"
            else:
                wood_name = re.sub(r" Planks$", "", name)
                data[f"item.{NS}.{wood}_small_board"] = f"Small {wood_name} Board"
                data[f"block.{NS}.{wood}_crafting_table"] = f"{wood_name} Crafting Table"
        data[f"message.{NS}.flint_pickaxe.too_weak"] = (
            "Кремнёвая кирка слишком слаба для этого камня" if code == "ru_ru"
            else "The flint pickaxe is too weak for this stone")
        write(rel, data)


if __name__ == "__main__":
    textures()
    assets()
    data()
    lang()
    print("ok")

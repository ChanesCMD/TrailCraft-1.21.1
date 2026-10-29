"""
Укреплённая труба (pipe/ReinforcedPipeBlock.java) и прочный лист без лавы — текстуры, модели, блокстейт, лут, рецепты,
тэги, переводы и превью docs/textures/reinforced_pipe.png. Здесь же обсидиановый порошок (жёрнов) и порошок гематита.

Запуск из корня проекта:  python scripts/gen_reinforced_pipe.py   (нужен Pillow; jar Create — из кэша Gradle)
Рецепт прочного листа Create (заливка лавой) убран в gen_create_compat.py (REPLACED) —
после изменения этого списка перезапустить и его.

Обычная медная труба Create с лавой прогорает (pipe/LavaPipes), укреплённая — нет. Модели — копии моделей трубы Create
(blockstates/fluid_pipe.json, models/block/fluid_pipe/*) со своими текстурами: трубы Create в цветах прочного листа;
ободки и стыки рисует PipeAttachmentModel Create (ClientEvents.wrapPipeModels), они медные.
Рецепты: 2 прочных листа и медный слиток в ряд (и столбиком) — 4 трубы, как у трубы Create. Прочный лист: жёрнов
мелет обсидиан в обсидиановый порошок Create, дробильные колёса — дроблёное рудное железо в порошок гематита; в
смесителе железный лист, обсидиановый порошок и порошок гематита — незаконченный прочный лист Create, потом
последовательная сборка: прессовать, положить железный лист, прессовать; заготовка — incomplete_sturdy_sheet.
"""
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar, recolor
from gen_ore_textures import RAMPS, dust, ramp

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(HERE, "..", "docs", "textures")
PIPE = "reinforced_fluid_pipe"
INCOMPLETE = "incomplete_sturdy_sheet"
UNPROCESSED = "create:unprocessed_obsidian_sheet"
POWDER = "hematite_powder"
# части трубы, которые рисует PipeAttachmentModel Create (AllPartialModels), — в блокстейте их нет
PARTIALS = ("rim/", "rim_connector/", "drain/", "connection/", "casing", "window")

LANG = {
    f"block.{NS}.{PIPE}": ("Укреплённая труба", "Reinforced Fluid Pipe"),
    f"block.{NS}.{PIPE}.desc": ("Держит лаву: обычная медная труба с лавой прогорает за 20 с",
                                "Holds lava: a regular copper pipe burns through in 20 s"),
    f"item.{NS}.{INCOMPLETE}": ("Заготовка прочного листа", "Incomplete Sturdy Sheet"),
    f"item.{NS}.{POWDER}": ("Порошок гематита", "Hematite Powder"),
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


def palette(img):
    return sorted({c[:3] for c in img.convert("RGBA").get_flattened_data() if c[3]}, key=sum)


def retexture(text):
    """Модель Create → наша: свои пути моделей и текстуры."""
    for old, new in (("create:block/fluid_pipe/", f"{NS}:block/{PIPE}/"),
                     ("create:block/pipes_connected", f"{NS}:block/{PIPE}_connected"),
                     ("create:block/pipes", f"{NS}:block/{PIPE}"),
                     ("\"block/copper_block\"", f"\"{NS}:block/{PIPE}\"")):
        text = text.replace(old, new)
    return text


def models(create):
    prefix = "assets/create/models/block/fluid_pipe/"
    for name in create.namelist():
        if not name.startswith(prefix) or not name.endswith(".json"):
            continue
        rel = name[len(prefix):]
        if rel.startswith(PARTIALS):
            continue
        data = json.loads(retexture(create.read(name).decode("utf-8")))
        if "parent" not in data or data["parent"] in ("block/block", "minecraft:block/block"):
            data["render_type"] = "minecraft:cutout_mipped"
        write(f"assets/{NS}/models/block/{PIPE}/{rel}", data)
    state = json.loads(retexture(create.read("assets/create/blockstates/fluid_pipe.json").decode("utf-8")))
    write(f"assets/{NS}/blockstates/{PIPE}.json", state)
    write(f"assets/{NS}/models/item/{PIPE}.json", {"parent": f"{NS}:block/{PIPE}/item"})


def textures(create):
    load = lambda rel: Image.open(BytesIO(create.read(f"assets/create/textures/{rel}.png"))).convert("RGBA")
    sturdy = palette(load("item/sturdy_sheet"))
    out = []
    for src, dst in (("pipes", PIPE), ("pipes_connected", f"{PIPE}_connected")):
        img = recolor(load(f"block/{src}"), sturdy)
        img.save(os.path.join(TEX, "block", f"{dst}.png"))
        out.append(img)
    # заготовка: железный лист Create, пятнами — тёмный обсидиановый слой
    sheet = load("item/iron_sheet")
    spotted = recolor(sheet, palette(load("item/powdered_obsidian")))
    img = sheet.copy()
    for y in range(16):
        for x in range(16):
            if sheet.getpixel((x, y))[3] and (x * 5 + y * 3) % 7 < 4:
                img.putpixel((x, y), spotted.getpixel((x, y)))
    img.save(os.path.join(TEX, "item", f"{INCOMPLETE}.png"))
    out.append(img)
    # порошок гематита — кучка пыли в цветах рудного гематита с красными крупинками
    powder = dust(RAMPS["hematite"][1], [ramp("3a1210", "5a1c16", "8c2824", "aa3c30", "c85a46", "e0846c")], 0.3, 9)
    powder.save(os.path.join(TEX, "item", f"{POWDER}.png"))
    out.append(powder)
    for name in (INCOMPLETE, POWDER):
        write(f"assets/{NS}/models/item/{name}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})
    return out


def data():
    write(f"data/{NS}/loot_table/blocks/{PIPE}.json", {
        "type": "minecraft:block", "random_sequence": f"{NS}:blocks/{PIPE}",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{PIPE}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    key = {"S": {"item": "create:sturdy_sheet"}, "C": {"tag": "c:ingots/copper"}}
    for name, pattern in ((PIPE, ["SCS"]), (f"{PIPE}_vertical", ["S", "C", "S"])):
        write(f"data/{NS}/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "key": key, "pattern": pattern,
            "result": {"id": f"{NS}:{PIPE}", "count": 4}})
    # обсидиан — в порошок жёрновом (и колёсами: своего рецепта дробления у обсидиана нет)
    write(f"data/{NS}/recipe/milling/obsidian.json", {
        "type": "create:milling", "ingredients": [{"item": "minecraft:obsidian"}], "processing_time": 500,
        "results": [{"id": "create:powdered_obsidian"}]})
    write(f"data/{NS}/recipe/crushing/{POWDER}.json", {
        "type": "create:crushing", "ingredients": [{"item": "create:crushed_raw_iron"}], "processing_time": 250,
        "results": [{"id": f"{NS}:{POWDER}"}]})
    write(f"data/{NS}/recipe/{UNPROCESSED.split(':')[1]}.json", {
        "type": "create:mixing",
        "ingredients": [{"tag": "c:plates/iron"}, {"tag": "c:dusts/obsidian"}, {"item": f"{NS}:{POWDER}"}],
        "results": [{"id": UNPROCESSED}]})
    step = lambda kind, extra: {"type": f"create:{kind}", "ingredients": [{"item": f"{NS}:{INCOMPLETE}"}] + extra,
                                "results": [{"id": f"{NS}:{INCOMPLETE}"}]}
    write(f"data/{NS}/recipe/sturdy_sheet.json", {
        "type": "create:sequenced_assembly", "ingredient": {"item": UNPROCESSED}, "loops": 1,
        "results": [{"id": "create:sturdy_sheet"}], "transitional_item": {"id": f"{NS}:{INCOMPLETE}"},
        "sequence": [step("pressing", []), step("deploying", [{"tag": "c:plates/iron"}]), step("pressing", [])]})
    tag("block", "minecraft:mineable/pickaxe", [f"{NS}:{PIPE}"])
    tag("item", "c:dusts", [f"{NS}:{POWDER}"])
    tag("item", "c:dusts/hematite", [f"{NS}:{POWDER}"])
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        lang = read(rel, {})
        for key_, texts in LANG.items():
            lang[key_] = texts[k]
        write(rel, lang)


def preview(images):
    scale, pad = 8, 8
    sheet = Image.new("RGBA", (pad + len(images) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "reinforced_pipe.png"))


if __name__ == "__main__":
    create = zipfile.ZipFile(find_jar("create-1.21.1-*-slim.jar"))
    models(create)
    images = textures(create)
    data()
    preview(images)
    print("ok")

"""
Блок каменного угля и угольная горелка (block/CoalBurnerBlock.java) — текстуры, модели, блокстейты, лут, рецепты,
тэги, переводы и превью docs/textures/burner.png.

Запуск из корня проекта:  python scripts/gen_burner.py   (нужен Pillow; ванильные текстуры — из jar клиента в кэше
Gradle, как в gen_create_compat.py)

Блок каменного угля — ванильный угольный блок в цветах нашего каменного угля (textures/item/bituminous_coal.png),
9 кусков ↔ блок, в c:storage_blocks/coal. Угольная горелка — слабая горелка всполоха для чаши Create (нагрев «греет»,
не «сильно греет»): железная коробка с решёткой сверху и окошком сбоку, в окошке — уголь, когда горит — угли и пламя.
Рецепт — блок каменного угля в кольце из 4 железных листов Create.
"""
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
IRON = [(52, 52, 56), (74, 74, 80), (98, 98, 104), (124, 124, 130), (150, 150, 156)]
EMBERS = [(120, 30, 8), (184, 62, 12), (230, 110, 24), (250, 168, 48), (255, 224, 120)]
HEAT_LEVELS = ["none", "smouldering", "fading", "kindled", "seething"]

LANG = {
    "block.trailcraft.bituminous_coal_block": ("Блок каменного угля", "Bituminous Coal Block"),
    "block.trailcraft.coal_burner": ("Угольная горелка", "Coal Burner"),
    "block.trailcraft.coal_burner.burn": ("Горения осталось: %s с", "Burn time left: %s s"),
    "block.trailcraft.coal_burner.desc": ("Греет чашу Create, пока горит уголь (не сильный нагрев). ПКМ углём или блоком "
                                          "угля — подложить, пустой рукой — сколько горения осталось",
                                          "Heats a Create basin while coal burns (not superheated). Right-click with "
                                          "coal or a coal block to add fuel, empty hand to check"),
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


def loot_self(name):
    write(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block", "random_sequence": f"{NS}:blocks/{name}",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def coal_block(client):
    coal = Image.open(os.path.join(TEX, "item", "bituminous_coal.png")).convert("RGBA")
    palette = sorted({c[:3] for c in coal.get_flattened_data() if c[3]}, key=sum)
    src = Image.open(BytesIO(client.read("assets/minecraft/textures/block/coal_block.png")))
    img = recolor(src, palette)
    img.save(os.path.join(TEX, "block", "bituminous_coal_block.png"))
    name = "bituminous_coal_block"
    write(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/cube_all",
                                                    "textures": {"all": f"{NS}:block/{name}"}})
    write(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
    write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
    loot_self(name)
    write(f"data/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "building", "key": {"C": {"item": f"{NS}:bituminous_coal"}},
        "pattern": ["CCC", "CCC", "CCC"], "result": {"id": f"{NS}:{name}", "count": 1}})
    write(f"data/{NS}/recipe/bituminous_coal_from_block.json", {
        "type": "minecraft:crafting_shapeless", "category": "misc", "ingredients": [{"item": f"{NS}:{name}"}],
        "result": {"id": f"{NS}:bituminous_coal", "count": 9}})
    for kind in ("block", "item"):
        tag(kind, "c:storage_blocks/coal", [f"{NS}:{name}"])
        tag(kind, "c:storage_blocks", [f"{NS}:{name}"])
    tag("block", "minecraft:mineable/pickaxe", [f"{NS}:{name}"])
    return img


def burner_textures(client):
    iron = recolor(Image.open(BytesIO(client.read("assets/minecraft/textures/block/iron_block.png"))), IRON)
    coal = Image.open(os.path.join(TEX, "block", "bituminous_coal_block.png")).convert("RGBA")
    embers = recolor(coal, EMBERS)
    dark, rim = IRON[0] + (255,), IRON[-1] + (255,)
    out = {}
    for lit in (False, True):
        inside = embers if lit else coal
        # сбоку — железо с окошком 10×5 у низа, в окошке уголь (горящий — угли и язычки пламени)
        side = iron.copy()
        for y in range(7, 13):
            for x in range(3, 13):
                if y == 7 or x in (3, 12):
                    side.putpixel((x, y), dark)
                else:
                    c = inside.getpixel((x, y))
                    if lit and y < 10 and (x * 7 + y * 3) % 5:
                        c = EMBERS[4 - (y - 8)] + (255,) if (x + y) % 3 == 0 else dark
                    side.putpixel((x, y), c)
        for x in range(16):
            side.putpixel((x, 0), rim)
            side.putpixel((x, 15), dark)
        # сверху — решётка из прутьев через 3 пикселя над углём
        top = inside.copy()
        for i in range(16):
            for j in range(16):
                if i in (0, 15) or j in (0, 15):
                    top.putpixel((i, j), dark)
                elif i % 3 == 0 or j % 5 == 0:
                    top.putpixel((i, j), iron.getpixel((i, j)))
        suffix = "_lit" if lit else ""
        side.save(os.path.join(TEX, "block", f"coal_burner_side{suffix}.png"))
        top.save(os.path.join(TEX, "block", f"coal_burner_top{suffix}.png"))
        out[f"side{suffix}"], out[f"top{suffix}"] = side, top
    iron.save(os.path.join(TEX, "block", "coal_burner_bottom.png"))
    out["bottom"] = iron
    return out


def burner():
    for suffix in ("", "_lit"):
        write(f"assets/{NS}/models/block/coal_burner{suffix}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
            "top": f"{NS}:block/coal_burner_top{suffix}", "side": f"{NS}:block/coal_burner_side{suffix}",
            "bottom": f"{NS}:block/coal_burner_bottom"}})
    write(f"assets/{NS}/blockstates/coal_burner.json", {"variants": {
        f"blaze={h}": {"model": f"{NS}:block/coal_burner" + ("" if h == "none" else "_lit")} for h in HEAT_LEVELS}})
    write(f"assets/{NS}/models/item/coal_burner.json", {"parent": f"{NS}:block/coal_burner"})
    loot_self("coal_burner")
    write(f"data/{NS}/recipe/coal_burner.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"I": {"tag": "c:plates/iron"}, "B": {"item": f"{NS}:bituminous_coal_block"}},
        "pattern": [" I ", "IBI", " I "], "result": {"id": f"{NS}:coal_burner", "count": 1}})
    tag("block", "minecraft:mineable/pickaxe", [f"{NS}:coal_burner"])


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


def preview(images):
    scale, pad = 8, 8
    sheet = Image.new("RGBA", (pad + len(images) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "burner.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    block = coal_block(client)
    tex = burner_textures(client)
    burner()
    lang()
    preview([block] + list(tex.values()))
    print("ok")

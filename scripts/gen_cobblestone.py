"""
Плиты пород (`<порода>_slab`, ModBlocks.SLABS: 3 породы в ряд — 6 плит; шпалы ж/д пути Create — тэг #create:sleepers)
и булыжник пород (`<порода>_cobblestone`, ModBlocks.COBBLESTONES): нижние две трети обвала падают им, верхняя треть —
гравием (collapse/Collapse). Текстуры, модели, блокстейты, модели предметов, лут, тэг кирки, переводы и превью
docs/textures/cobblestone.png.

Запуск из корня проекта:  python scripts/gen_cobblestone.py   (нужен Pillow)

Текстура — ванильный булыжник (scripts/templates/cobblestone.png), перекрашенный в цвета породы: пиксели булыжника
по яркости сопоставляются с пикселями текстуры породы (textures/block/<порода>.png) той же доли яркости — рисунок
камней ванильный, цвета и разброс — как у породы.
"""
import json
import os

from PIL import Image

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures", "block")
DOCS = os.path.join(HERE, "..", "docs", "textures")
ROCKS = ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite", "gray_granite", "limestone",
         "marble", "sandstone", "shale"]
CONTRAST = 0.35


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


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def texture(rock):
    base = Image.open(os.path.join(HERE, "templates", "cobblestone.png")).convert("RGBA")
    colors = sorted(Image.open(os.path.join(TEX, f"{rock}.png")).convert("RGBA").get_flattened_data(), key=lum)
    pixels = list(base.get_flattened_data())
    order = sorted(range(len(pixels)), key=lambda i: lum(pixels[i]))
    # узкий разброс у светлых пород (мел, доломит) стирает камни — добавить тени ванильного рисунка
    levels = [lum(c) for c in pixels]
    mean, spread = sum(levels) / len(levels), max(levels) - min(levels)
    out = [None] * len(pixels)
    for rank, i in enumerate(order):
        k = 1 + CONTRAST * (levels[i] - mean) / spread
        out[i] = tuple(max(0, min(255, round(v * k))) for v in colors[rank * len(colors) // len(pixels)][:3]) + (255,)
    img = Image.new("RGBA", base.size)
    img.putdata(out)
    img.save(os.path.join(TEX, f"{rock}_cobblestone.png"))
    return img


def slab(rock, ru, en, pickaxe):
    """Плита породы: модели нижней и верхней половины на текстурах породы (у базальта торцы — basalt_top), двойная — сам
    блок породы; лут — 2 плиты с двойной."""
    name = f"{rock}_slab"
    side = f"{NS}:block/{rock}"
    end = f"{NS}:block/{rock}_top" if os.path.exists(os.path.join(TEX, f"{rock}_top.png")) else side
    textures = {"bottom": end, "top": end, "side": side}
    write(f"assets/{NS}/models/block/{name}.json", {"parent": "minecraft:block/slab", "textures": textures})
    write(f"assets/{NS}/models/block/{name}_top.json", {"parent": "minecraft:block/slab_top", "textures": textures})
    double = f"{NS}:block/{rock}"
    write(f"assets/{NS}/blockstates/{name}.json", {"variants": {
        "type=bottom": {"model": f"{NS}:block/{name}"}, "type=top": {"model": f"{NS}:block/{name}_top"},
        "type=double": {"model": double}}})
    write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
    write(f"data/{NS}/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{
            "type": "minecraft:item", "name": f"{NS}:{name}",
            "functions": [{"function": "minecraft:set_count", "count": 2.0, "add": False, "conditions": [{
                "condition": "minecraft:block_state_property", "block": f"{NS}:{name}",
                "properties": {"type": "double"}}]}, {"function": "minecraft:explosion_decay"}]}]}],
        "random_sequence": f"{NS}:blocks/{name}"})
    write(f"data/{NS}/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "key": {"R": {"item": f"{NS}:{rock}"}}, "pattern": ["RRR"],
        "result": {"id": f"{NS}:{name}", "count": 6}})
    if f"{NS}:{name}" not in pickaxe["values"]:
        pickaxe["values"].append(f"{NS}:{name}")
    # «Андезитовый гравий» → «Андезитовая плита», «Серый гранитный гравий» → «Серая гранитная плита»
    ru[f"block.{NS}.{name}"] = " ".join(w[:-2] + "ая" if w.endswith(("ый", "ой")) else w
                                        for w in ru[f"block.{NS}.{rock}_gravel"].replace(" гравий", " плита").split())
    en[f"block.{NS}.{name}"] = en[f"block.{NS}.{rock}_gravel"].replace("Gravel", "Slab")


def tag(kind, tag_id, values):
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write(rel, data)


def main():
    ru, en = read(f"assets/{NS}/lang/ru_ru.json", {}), read(f"assets/{NS}/lang/en_us.json", {})
    pickaxe = read("data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": []})
    images = []
    for rock in ROCKS:
        name = f"{rock}_cobblestone"
        images.append((texture(rock), Image.open(os.path.join(TEX, f"{rock}.png")).convert("RGBA")))
        write(f"assets/{NS}/models/block/{name}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{name}"}})
        write(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}"})
        write(f"data/{NS}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}],
            "random_sequence": f"{NS}:blocks/{name}"})
        if f"{NS}:{name}" not in pickaxe["values"]:
            pickaxe["values"].append(f"{NS}:{name}")
        # «Андезитовый гравий» → «Андезитовый булыжник»
        ru[f"block.{NS}.{name}"] = ru[f"block.{NS}.{rock}_gravel"].replace("гравий", "булыжник")
        en[f"block.{NS}.{name}"] = en[f"block.{NS}.{rock}_gravel"].replace("Gravel", "Cobblestone")
    for rock in ROCKS:
        slab(rock, ru, en, pickaxe)
    slabs = [f"{NS}:{rock}_slab" for rock in ROCKS]
    for kind in ("block", "item"):
        tag(kind, "minecraft:slabs", slabs)
    write("data/minecraft/tags/block/mineable/pickaxe.json", pickaxe)
    write(f"assets/{NS}/lang/ru_ru.json", ru)
    write(f"assets/{NS}/lang/en_us.json", en)

    # превью: порода над своим булыжником
    scale, pad = 6, 6
    cell = 16 * scale
    sheet = Image.new("RGBA", (pad + len(images) * (cell + pad), 2 * cell + 3 * pad), (60, 60, 60, 255))
    for i, (cobble, rock) in enumerate(images):
        x = pad + i * (cell + pad)
        sheet.alpha_composite(rock.resize((cell, cell), Image.NEAREST), (x, pad))
        sheet.alpha_composite(cobble.resize((cell, cell), Image.NEAREST), (x, 2 * pad + cell))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "cobblestone.png"))


if __name__ == "__main__":
    main()
    print("ok")

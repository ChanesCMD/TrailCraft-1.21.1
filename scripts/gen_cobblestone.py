"""
Булыжник пород (`<порода>_cobblestone`, ModBlocks.COBBLESTONES): нижние две трети обвала падают им, верхняя треть —
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

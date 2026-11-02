"""Каменные кирпичи и блоки укреплённых кирпичей по породам."""
import json
import os

from PIL import Image, ImageChops

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)

ROCKS = {
    "andesite": ("Андезит", "Andesite", (120, 120, 120)),
    "basalt": ("Базальт", "Basalt", (74, 74, 80)),
    "chalk": ("Мел", "Chalk", (225, 223, 210)),
    "diorite": ("Диорит", "Diorite", (180, 180, 180)),
    "dolomite": ("Доломит", "Dolomite", (200, 190, 170)),
    "gabbro": ("Габбро", "Gabbro", (66, 70, 66)),
    "granite": ("Гранит", "Granite", (150, 100, 85)),
    "gray_granite": ("Серый гранит", "Gray Granite", (105, 100, 98)),
    "limestone": ("Известняк", "Limestone", (205, 195, 150)),
    "marble": ("Мрамор", "Marble", (210, 210, 215)),
    "sandstone": ("Песчаник", "Sandstone", (200, 170, 120)),
    "shale": ("Сланец", "Shale", (85, 90, 100)),
}
CHISELS = ("copper", "bronze", "iron", "steel")


def write(path, data):
    p = os.path.join(ROOT, *path.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def copy_item_texture():
    src = os.path.join(ASSETS, "textures", "item", "brick_red.png")
    dst = os.path.join(ASSETS, "textures", "item", "stone_brick.png")
    Image.open(src).save(dst)


def block_texture(rock, color):
    src = Image.open(os.path.join(ASSETS, "textures", "block", "bricks_red.png")).convert("RGBA")
    tint = Image.new("RGBA", src.size, color + (255,))
    tinted = ImageChops.multiply(src, tint)
    out = Image.blend(src, tinted, 0.62)
    out.save(os.path.join(ASSETS, "textures", "block", f"reinforced_bricks_{rock}.png"))


def main():
    copy_item_texture()
    for rock, (ru, en, color) in ROCKS.items():
        block_texture(rock, color)
        write(f"assets/{NS}/models/item/stone_brick_{rock}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/stone_brick"}})
        write(f"assets/{NS}/models/block/reinforced_bricks_{rock}.json",
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/reinforced_bricks_{rock}"}})
        write(f"assets/{NS}/models/item/reinforced_bricks_{rock}.json",
              {"parent": f"{NS}:block/reinforced_bricks_{rock}"})
        write(f"assets/{NS}/blockstates/reinforced_bricks_{rock}.json",
              {"variants": {"": {"model": f"{NS}:block/reinforced_bricks_{rock}"}}})
        write(f"data/{NS}/loot_table/blocks/reinforced_bricks_{rock}.json",
              {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
                  {"type": "minecraft:item", "name": f"{NS}:reinforced_bricks_{rock}"}],
                  "conditions": [{"condition": "minecraft:survives_explosion"}]}],
               "random_sequence": f"{NS}:blocks/reinforced_bricks_{rock}"})
        # зубило + камушек породы -> каменный кирпич; зубило остаётся и тупится
        for chisel in CHISELS:
            write(f"data/{NS}/recipe/stone_brick_{rock}_{chisel}.json", {
                "type": "minecraft:crafting_shapeless", "category": "misc",
                "ingredients": [{"item": f"{NS}:{chisel}_chisel"}, {"item": f"{NS}:{rock}_pebble"}],
                "result": {"id": f"{NS}:stone_brick_{rock}", "count": 1},
            })
        # 2 укреплённых + 2 каменных кирпича -> блок
        write(f"data/{NS}/recipe/reinforced_bricks_{rock}.json", {
            "type": "minecraft:crafting_shaped", "category": "building",
            "pattern": ["RB", "BR"],
            "key": {"R": {"item": f"{NS}:reinforced_brick"}, "B": {"item": f"{NS}:stone_brick_{rock}"}},
            "result": {"id": f"{NS}:reinforced_bricks_{rock}", "count": 1},
        })

    # тэг кирки: добавляем в существующий, не теряя чужие записи
    tag_path = f"data/minecraft/tags/block/mineable/pickaxe.json"
    tag_file = os.path.join(ROOT, *tag_path.split("/"))
    with open(tag_file, encoding="utf-8") as f:
        tag_data = json.load(f)
    tag_values = tag_data.setdefault("values", [])
    for rock in ROCKS:
        value = f"{NS}:reinforced_bricks_{rock}"
        if value not in tag_values:
            tag_values.append(value)
    write(tag_path, tag_data)

    for code, idx in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        p = os.path.join(ROOT, rel)
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for rock, names in ROCKS.items():
            data[f"block.{NS}.reinforced_bricks_{rock}"] = f"Укреплённые кирпичи: {names[0]}" if code == "ru_ru" else f"Reinforced {names[1]} Bricks"
            data[f"item.{NS}.stone_brick_{rock}"] = f"Каменный кирпич: {names[0]}" if code == "ru_ru" else f"{names[1]} Stone Brick"
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

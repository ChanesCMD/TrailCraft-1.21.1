"""Люк большого сыродутного горна, крица, рецепт и тэг кирпичей."""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)

ROCKS = ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite", "gray_granite",
         "limestone", "marble", "sandstone", "shale"]


def write(path, data):
    p = os.path.join(ROOT, *path.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hatch_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dark, mid, light, rust = (55, 55, 60, 255), (92, 92, 98, 255), (135, 135, 142, 255), (125, 76, 44, 255)
    for y in range(2, 14):
        for x in range(1, 15):
            img.putpixel((x, y), mid if (x + y) % 5 else light)
    # рамка
    for i in range(1, 15):
        img.putpixel((i, 1), dark)
        img.putpixel((i, 14), dark)
    for i in range(2, 14):
        img.putpixel((0, i), dark)
        img.putpixel((15, i), dark)
    # решётка
    for x in (5, 8, 11):
        for y in range(3, 13):
            img.putpixel((x, y), dark)
    for y in (5, 8, 11):
        for x in range(2, 14):
            img.putpixel((x, y), dark)
    # заклёпки и ржавчина
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        img.putpixel((x, y), light)
    img.putpixel((4, 4), rust)
    img.putpixel((12, 10), rust)
    img.save(os.path.join(ASSETS, "textures", "block", "large_bloomery.png"))


def main():
    hatch_texture()
    write(f"assets/{NS}/models/block/large_bloomery.json",
          {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/large_bloomery"}})
    write(f"assets/{NS}/models/item/large_bloomery.json", {"parent": f"{NS}:block/large_bloomery"})
    variants = {}
    for facing in ("north", "east", "south", "west"):
        for lit in ("false", "true"):
            variants[f"facing={facing},lit={lit}"] = f"{NS}:block/large_bloomery"
    write(f"assets/{NS}/blockstates/large_bloomery.json", {"variants": variants})
    write(f"assets/{NS}/models/item/bloom.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": "minecraft:item/raw_iron"}})
    write(f"data/{NS}/loot_table/blocks/large_bloomery.json",
          {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
              {"type": "minecraft:item", "name": f"{NS}:large_bloomery"}],
              "conditions": [{"condition": "minecraft:survives_explosion"}]}],
           "random_sequence": f"{NS}:blocks/large_bloomery"})
    write(f"data/{NS}/recipe/large_bloomery.json", {
        "type": "minecraft:crafting_shaped", "category": "building",
        "pattern": [" B ", "B B", " B "],
        "key": {"B": {"tag": "c:ingots/bronze"}},
        "result": {"id": f"{NS}:large_bloomery", "count": 1},
    })
    write(f"data/{NS}/tags/block/bloomery_bricks.json",
          {"replace": False, "values": [f"{NS}:reinforced_bricks_{r}" for r in ROCKS]})
    tag_path = f"data/minecraft/tags/block/mineable/pickaxe.json"
    tag_file = os.path.join(ROOT, *tag_path.split("/"))
    with open(tag_file, encoding="utf-8") as f:
        tag_data = json.load(f)
    if f"{NS}:large_bloomery" not in tag_data.setdefault("values", []):
        tag_data["values"].append(f"{NS}:large_bloomery")
    write(tag_path, tag_data)

    for code, idx in (("ru_ru", 0), ("en_us", 1)):
        p = os.path.join(ASSETS, "lang", f"{code}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data[f"block.{NS}.large_bloomery"] = ("Люк большого сыродутного горна", "Large Bloomery Hatch")[idx]
        data[f"item.{NS}.bloom"] = ("Крица", "Bloom")[idx]
        data["message.trailcraft.large_bloomery.invalid"] = (
            "Нужна труба 3×3×3 из укреплённых кирпичей вокруг люка",
            "Build a 3×3×3 tube of reinforced bricks around the hatch")[idx]
        data["jei.trailcraft.large_bloomery"] = (
            "Большой горн: 8 руды + 8 топлива → крица",
            "Large bloomery: 8 ore + 8 fuel → bloom")[idx]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

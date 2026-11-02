"""Каменная наковальня: модель на 2 пикселя ниже блока, текстура, рецепт."""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
ASSETS = os.path.join(ROOT, "assets", NS)
DATA = os.path.join(ROOT, "data", NS)
ROCKS = ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite", "gray_granite",
         "limestone", "marble", "sandstone", "shale"]
ANVIL_ROCKS = ["andesite", "basalt", "diorite", "granite", "gray_granite", "gabbro"]


def write(path, data):
    p = os.path.join(ROOT, *path.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    mid, dark, light = (112, 112, 112, 255), (70, 70, 70, 255), (155, 155, 155, 255)
    for y in range(16):
        for x in range(16):
            c = mid if (x * 7 + y * 3) % 11 else dark
            if y < 2:
                c = dark
            elif y == 2:
                c = light
            img.putpixel((x, y), c)
    for x in (2, 13):
        for y in (3, 12):
            img.putpixel((x, y), dark)
    img.save(os.path.join(ASSETS, "textures", "block", "stone_anvil.png"))


def face(cull=None):
    d = {"uv": [0, 0, 16, 16], "texture": "#all"}
    if cull:
        d["cullface"] = cull
    return d


def main():
    texture()
    variants = {}
    for rock in ANVIL_ROCKS:
        tex = "minecraft:block/basalt" if rock == "basalt" else f"{NS}:block/{rock}"
        name = f"stone_anvil_{rock}"
        write(f"assets/{NS}/models/block/{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {"particle": tex, "all": tex},
            "elements": [{
                "from": [0, 0, 0], "to": [16, 14, 16],
                "faces": {"down": face("down"), "up": face("up"), "north": face("north"),
                          "south": face("south"), "west": face("west"), "east": face("east")}
            }]
        })
        variants[f"rock={rock}"] = {"model": f"{NS}:block/{name}"}
    write(f"assets/{NS}/blockstates/stone_anvil.json", {"variants": variants})
    write(f"assets/{NS}/models/item/stone_anvil.json", {"parent": f"{NS}:block/stone_anvil_andesite"})
    write(f"data/{NS}/loot_table/blocks/stone_anvil.json",
          {"type": "minecraft:block", "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [
              {"type": "minecraft:item", "name": f"{NS}:stone_anvil"}],
              "conditions": [{"condition": "minecraft:survives_explosion"}]}],
           "random_sequence": f"{NS}:blocks/stone_anvil"})
    write(f"data/{NS}/tags/item/stone_bricks.json",
          {"replace": False, "values": [f"{NS}:stone_brick_{r}" for r in ROCKS]})
    tag_path = "data/minecraft/tags/block/mineable/pickaxe.json"
    tag_file = os.path.join(ROOT, *tag_path.split("/"))
    with open(tag_file, encoding="utf-8") as f:
        tag_data = json.load(f)
    if f"{NS}:stone_anvil" not in tag_data.setdefault("values", []):
        tag_data["values"].append(f"{NS}:stone_anvil")
    write(tag_path, tag_data)
    for code, idx in (("ru_ru", 0), ("en_us", 1)):
        p = os.path.join(ASSETS, "lang", f"{code}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        data[f"block.{NS}.stone_anvil"] = ("Каменная наковальня", "Stone Anvil")[idx]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

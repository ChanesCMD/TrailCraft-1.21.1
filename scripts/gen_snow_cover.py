#!/usr/bin/env python3
"""Снег в клетке с мелкими блоками (block/SnowCover): blockstate со слоями снега, побелевшие копии моделей и текстур растений.

Запуск из корня проекта: python3 scripts/gen_snow_cover.py. Нужен Pillow. Повторный запуск перезаписывает результат.
Камешки, кремень и ветка (PebbleBlock): слой снега поверх модели (при 1 слое — тонкий, 1 пиксель, чтобы камень торчал).
Растения (кусты, дикие морковь/картофель/джут): при снеге рисуется побелевшая копия модели (<имя>_snow) и слой снега.
Траву (grass_*) красит клиент через тинт (SeasonColors.grass), её blockstate написан отдельно.
"""
import json
import os

from PIL import Image

ASSETS = "src/main/resources/assets/trailcraft"
WHITE = (238, 243, 248)
MIX = 0.62
LAYERS = {1: "snow_height2", 2: "snow_height4", 3: "snow_height6", 4: "snow_height8",
          5: "snow_height10", 6: "snow_height12", 7: "snow_height14", 8: "snow_block"}
SNOWY = "1|2|3|4|5|6|7|8"
PEBBLES = ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite", "gray_granite", "limestone",
           "marble", "sandstone", "shale", "flint"]


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n")


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def snow_texture(name):
    """block/<name>.png -> block/<name>_snow.png: тот же рисунок, сдвинутый к белому (прозрачность сохраняется)."""
    src = f"{ASSETS}/textures/block/{name}.png"
    if not os.path.exists(src):
        return False
    img = Image.open(src).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = tuple(round(c * (1 - MIX) + w * MIX) for c, w in zip((r, g, b), WHITE)) + (a,)
    img.save(f"{ASSETS}/textures/block/{name}_snow.png")
    return True


def snow_model(name):
    """models/block/<name>.json -> <name>_snow.json с побелевшими текстурами. Возвращает 'trailcraft:block/<name>_snow'."""
    model = read(f"{ASSETS}/models/block/{name}.json")
    parent = model.get("parent", "")
    if parent.startswith("trailcraft:block/"):
        model["parent"] = snow_model(parent.split("/", 1)[1])
    for key, tex in model.get("textures", {}).items():
        if tex.startswith("trailcraft:block/") and snow_texture(tex.split("/", 1)[1]):
            model["textures"][key] = tex + "_snow"
    write(f"{ASSETS}/models/block/{name}_snow.json", model)
    return f"trailcraft:block/{name}_snow"


def layer_parts(first_thin=False, when=None):
    parts = []
    for k, model in LAYERS.items():
        use = "trailcraft:block/snow_cover_1" if first_thin and k == 1 else f"minecraft:block/{model}"
        cond = {"snow": str(k)}
        parts.append({"when": {"AND": [when, cond]} if when else cond, "apply": {"model": use}})
    return parts


def thin_layer_model():
    face = {"texture": "#snow"}
    write(f"{ASSETS}/models/block/snow_cover_1.json", {
        "textures": {"snow": "minecraft:block/snow", "particle": "minecraft:block/snow"},
        "elements": [{"from": [0, 0, 0], "to": [16, 1, 16],
                      "faces": {d: dict(face) for d in ("down", "up", "north", "south", "west", "east")}}]})


def pebbles():
    thin_layer_model()
    for name in [f"{p}_pebble" for p in PEBBLES] + ["fallen_branch"]:
        path = f"{ASSETS}/blockstates/{name}.json"
        variants = read(path)
        if "multipart" in variants:
            continue
        write(path, {"multipart": [{"apply": variants["variants"][""]}] + layer_parts(first_thin=True)})


def plants():
    for name in ["wild_carrot", "wild_potato"]:
        model = f"trailcraft:block/{name}"
        write(f"{ASSETS}/blockstates/{name}.json", {"multipart": [
            {"when": {"snow": "0"}, "apply": {"model": model}},
            {"when": {"snow": SNOWY}, "apply": {"model": snow_model(name)}}] + layer_parts()})
    for bush in ["currant", "cloudberry", "gooseberry", "blackberry"]:
        parts = []
        for age in range(4):
            n = f"{bush}_bush_stage{age}"
            parts.append({"when": {"AND": [{"age": str(age)}, {"snow": "0"}]}, "apply": {"model": f"trailcraft:block/{n}"}})
            parts.append({"when": {"AND": [{"age": str(age)}, {"snow": SNOWY}]}, "apply": {"model": snow_model(n)}})
        write(f"{ASSETS}/blockstates/{bush}_bush.json", {"multipart": parts + layer_parts()})
    write(f"{ASSETS}/blockstates/wild_jute.json", {"multipart": [
        {"when": {"half": "upper"}, "apply": {"model": "trailcraft:block/wild_jute_top"}},
        {"when": {"AND": [{"half": "lower"}, {"snow": "0"}]}, "apply": {"model": "trailcraft:block/wild_jute_bottom"}},
        {"when": {"AND": [{"half": "lower"}, {"snow": SNOWY}]}, "apply": {"model": snow_model("wild_jute_bottom")}}]
        + layer_parts()})


if __name__ == "__main__":
    pebbles()
    plants()
    print("ok")

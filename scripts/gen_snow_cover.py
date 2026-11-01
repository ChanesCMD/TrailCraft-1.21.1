#!/usr/bin/env python3
"""Снег в клетке с мелкими блоками (block/SnowCover): blockstate со слоями снега, побелевшие копии моделей и текстур растений.

Запуск из корня проекта: python3 scripts/gen_snow_cover.py. Нужен Pillow. Повторный запуск перезаписывает результат.
Камешки, кремень, ветка и самородки (PebbleBlock): слой снега обычной высоты, камень приподнят и торчит из него (без наложения текстур).
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
POKE_LAYERS = 3  # до скольких слоёв снега камень ещё торчит из снега
NUGGETS = ["native_copper", "malachite", "tetrahedrite", "cassiterite", "sphalerite"]
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


def layer_parts(when=None):
    """Слои снега: обычная высота (snow_height2 для 1 слоя и т. д.)."""
    parts = []
    for k, model in LAYERS.items():
        cond = {"snow": str(k)}
        parts.append({"when": {"AND": [when, cond]} if when else cond, "apply": {"model": f"minecraft:block/{model}"}})
    return parts


def effective_elements(name):
    """Элементы модели с учётом родителей мода (у самородков форма лежит в родителе nugget_shape_*)."""
    model = read(f"{ASSETS}/models/block/{name}.json")
    if "elements" in model:
        return model["elements"]
    parent = model.get("parent", "")
    return effective_elements(parent.split("/", 1)[1]) if parent.startswith("trailcraft:block/") else []


def lift(name, k):
    """Копия модели (и её родителей мода), поднятая так, чтобы над слоем снега в k слоёв торчал ~1 пиксель.
    Ни одна горизонтальная плоскость модели не должна совпасть с верхом снега (2k): иначе грани сливаются и текстуры
    мерцают (z-fighting) — в таком случае модель сдвигается ещё на 0,25 пикселя. Возвращает имя без пространства имён."""
    out = f"{name}_s{k}"
    elems = effective_elements(name)
    top = 2 * k
    height = max(el["to"][1] for el in elems)
    poke = 1.0 if height >= 2 else 0.5
    offset = top - (height - poke)
    planes = {v for el in elems for v in (el["from"][1], el["to"][1])}
    for _ in range(4):  # плоскости ближе 0,2 пикселя к верху снега тоже сдвигаем: на расстоянии глубины не хватает
        if not any(abs(v + offset - top) < 0.2 for v in planes):
            break
        offset += 0.25
    model = read(f"{ASSETS}/models/block/{name}.json")
    if "elements" in model:
        for el in model["elements"]:
            el["from"][1] = round(el["from"][1] + offset, 4)
            el["to"][1] = round(el["to"][1] + offset, 4)
            if "rotation" in el:
                el["rotation"]["origin"][1] = round(el["rotation"]["origin"][1] + offset, 4)
            for face in el["faces"].values():
                face.pop("cullface", None)
    else:
        model["parent"] = "trailcraft:block/" + lift(model["parent"].split("/", 1)[1], k)
    write(f"{ASSETS}/models/block/{out}.json", model)
    return out


def pebbles():
    """Камешки, кремень, ветка и самородки (PebbleBlock): слой снега обычной высоты, а сам блок приподнят и торчит из
    снега на ~1 пиксель. С 4 слоёв и выше он полностью засыпан."""
    old = f"{ASSETS}/models/block/snow_cover_1.json"
    if os.path.exists(old):
        os.remove(old)
    names = [f"{p}_pebble" for p in PEBBLES] + ["fallen_branch"] + [f"{o}_nugget" for o in NUGGETS]
    for name in names:
        path = f"{ASSETS}/blockstates/{name}.json"
        data = read(path)
        base = data["variants"][""] if "variants" in data else data["multipart"][0]["apply"]
        parts = [{"when": {"snow": "0"}, "apply": base}]
        for k in range(1, POKE_LAYERS + 1):
            lifted = [{**v, "model": "trailcraft:block/" + lift(v["model"].split("/", 1)[1], k)} for v in base]
            parts.append({"when": {"snow": str(k)}, "apply": lifted})
        write(path, {"multipart": parts + layer_parts()})


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
    # дикий джут: белеют обе половины, слой снега рисует только нижняя
    write(f"{ASSETS}/blockstates/wild_jute.json", {"multipart": [
        {"when": {"AND": [{"half": "upper"}, {"snow": "0"}]}, "apply": {"model": "trailcraft:block/wild_jute_top"}},
        {"when": {"AND": [{"half": "upper"}, {"snow": SNOWY}]}, "apply": {"model": snow_model("wild_jute_top")}},
        {"when": {"AND": [{"half": "lower"}, {"snow": "0"}]}, "apply": {"model": "trailcraft:block/wild_jute_bottom"}},
        {"when": {"AND": [{"half": "lower"}, {"snow": SNOWY}]}, "apply": {"model": snow_model("wild_jute_bottom")}}]
        + layer_parts({"half": "lower"})})


if __name__ == "__main__":
    pebbles()
    plants()
    print("ok")

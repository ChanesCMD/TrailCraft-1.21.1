"""
Модели автора из Blockbench (scripts/templates/models/<блок>.bbmodel) → модели блоков Minecraft и их текстуры.

Запуск из корня проекта:  python scripts/gen_bbmodels.py   (нужен Pillow)

Для каждого файла пишет models/block/<блок>.json, текстуры из файла (они вшиты в .bbmodel) — textures/block/<блок>.png
для первой и <блок>_<имя>.png для остальных (имена — TEXTURE_NAMES), и models/item/<блок>.json — тот же блок, так что
в инвентаре предмет выглядит как поставленный блок. UV в Blockbench заданы в разрешении проекта (resolution), в модели
Minecraft — всегда в 16: делятся на resolution / 16.
Армейский вещмешок — 1backpack, кожаный рюкзак — 2backpack, могила — 1gravestone. Блок рюкзака рисуется и на спине
игрока (client/BackpackLayer), формы блоков — в BackpackBlock и GraveBlock: поменяв модель, поправить и их.
"""
import base64
import io
import json
import os

from PIL import Image

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(HERE, "templates", "models")
ROOT = os.path.join(HERE, "..", "src", "main", "resources", "assets", NS)
# блок → имена текстур по порядку (первая — просто <блок>)
TEXTURE_NAMES = {
    "duffel_backpack": [""],
    "leather_backpack": [""],
    "grave": ["", "arm", "base"],
}
# блоки без своего предмета
NO_ITEM = {"grave"}
AXES = "xyz"


def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def number(v):
    """Целые без «.0», дробные — как есть (округлены от шума float)."""
    v = round(v, 4)
    return int(v) if v == int(v) else v


def convert(name, bb):
    texture_ids = [str(t["id"]) for t in bb["textures"]]
    suffixes = TEXTURE_NAMES[name]
    tex_ref = {}
    for i, tex in enumerate(bb["textures"]):
        file = name + (f"_{suffixes[i]}" if suffixes[i] else "")
        data = tex["source"].split(",", 1)[1]
        Image.open(io.BytesIO(base64.b64decode(data))).convert("RGBA").save(
            os.path.join(ROOT, "textures", "block", file + ".png"))
        tex_ref[texture_ids[i]] = f"{NS}:block/{file}"
    su = 16 / bb["resolution"]["width"]
    sv = 16 / bb["resolution"]["height"]
    elements = []
    for el in bb["elements"]:
        if not el.get("export", True):
            continue
        out = {"from": [number(v) for v in el["from"]], "to": [number(v) for v in el["to"]]}
        rot = el.get("rotation", [0, 0, 0])
        turned = [(AXES[i], a) for i, a in enumerate(rot) if a]
        if len(turned) > 1:
            raise ValueError(f"{name}: элемент повёрнут больше чем по одной оси — в Minecraft так нельзя")
        if turned:
            axis, angle = turned[0]
            out["rotation"] = {"angle": number(angle), "axis": axis, "origin": [number(v) for v in el["origin"]]}
        faces = {}
        for side, face in el["faces"].items():
            if face.get("texture") is None:
                continue
            u0, v0, u1, v1 = face["uv"]
            f = {"uv": [number(u0 * su), number(v0 * sv), number(u1 * su), number(v1 * sv)],
                 "texture": "#" + str(face["texture"])}
            if face.get("rotation"):
                f["rotation"] = face["rotation"]
            faces[side] = f
        out["faces"] = faces
        elements.append(out)
    textures = {"particle": tex_ref[texture_ids[0]]}
    textures.update({tid: ref for tid, ref in tex_ref.items()})
    model = {"parent": "minecraft:block/block", "textures": textures, "elements": elements}
    if not bb.get("ambientocclusion", True):
        model["ambientocclusion"] = False
    write(f"models/block/{name}.json", model)
    if name not in NO_ITEM:
        write(f"models/item/{name}.json", {"parent": f"{NS}:block/{name}"})


def main():
    for name in TEXTURE_NAMES:
        with open(os.path.join(SRC, name + ".bbmodel"), encoding="utf-8") as f:
            convert(name, json.load(f))
    print("ok")


if __name__ == "__main__":
    main()

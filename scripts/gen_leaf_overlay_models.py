"""Ставит всем моделям листвы общий родитель с крестом-оверлеем.

Листва остаётся кубом, но поверх него добавлены две скрещённые плоскости (как у травы), чуть
выступающие за края блока: они дают объём и «лохматость» кроны. Текстура у креста та же, что у
куба, поэтому сезонная подмена спрайтов работает и для оверлея.

Запуск из корня проекта:  python scripts/gen_leaf_overlay_models.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
MODELS = os.path.join(ROOT, "assets", NS, "models", "block")
PARENT = "leaves_with_cross"


def write(name, data):
    with open(os.path.join(MODELS, name), "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def face(cull=None, texture="#all", size=16):
    out = {"uv": [0, 0, size, size], "texture": texture}
    if cull:
        out["cullface"] = cull
    return out


def plane(x0, z0, x1, z1, faces):
    return {
        "from": [x0, -5, z0],
        "to": [x1, 21, z1],
        "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
        "shade": False,
        "faces": faces,
    }


# Куб — как у обычной листвы, плюс два креста. Плоскости шире и выше блока (от -5 до 21 по X/Z и по Y),
# после поворота на 45° они заметно торчат за края. Крест рисуется отдельной текстурой 32×32 и берёт
# из неё 26×26 (UV 0..13), поэтому пиксель оверлея равен пикселю блока, а не растянут.
parent = {
    "parent": "minecraft:block/block",
    "render_type": "minecraft:cutout_mipped",
    "textures": {"particle": "#all"},
    "elements": [
        {
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {
                "down": face("down"),
                "up": face("up"),
                "north": face("north"),
                "south": face("south"),
                "west": face("west"),
                "east": face("east"),
            },
        },
        plane(-5, 8, 21, 8, {"north": face(texture="#overlay", size=13),
                              "south": face(texture="#overlay", size=13)}),
        plane(8, -5, 8, 21, {"west": face(texture="#overlay", size=13),
                             "east": face(texture="#overlay", size=13)}),
    ],
}
write(f"{PARENT}.json", parent)

count = 0
for name in sorted(os.listdir(MODELS)):
    if not name.endswith("_leaves.json") or name == f"{PARENT}.json":
        continue
    write(name, {
        "parent": f"{NS}:block/{PARENT}",
        "render_type": "minecraft:cutout_mipped",
        "textures": {"all": f"{NS}:block/{name[:-5]}", "overlay": f"{NS}:block/{name[:-5]}_overlay"},
    })
    count += 1
print(f"ok: {count} leaf models")

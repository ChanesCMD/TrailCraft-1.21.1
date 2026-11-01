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


def face(cull=None):
    out = {"uv": [0, 0, 16, 16], "texture": "#all"}
    if cull:
        out["cullface"] = cull
    return out


def plane(x0, z0, x1, z1, faces):
    return {
        "from": [x0, -6, z0],
        "to": [x1, 22, z1],
        "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": True},
        "shade": False,
        "faces": faces,
    }


# Куб — как у обычной листвы, плюс два креста. Плоскости шире и выше блока (от -6 до 22 по X/Z и по Y),
# после поворота на 45° они заметно торчат за края.
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
        plane(-6, 8, 22, 8, {"north": face(), "south": face()}),
        plane(8, -6, 8, 22, {"west": face(), "east": face()}),
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
        "textures": {"all": f"{NS}:block/{name[:-5]}"},
    })
    count += 1
print(f"ok: {count} leaf models")

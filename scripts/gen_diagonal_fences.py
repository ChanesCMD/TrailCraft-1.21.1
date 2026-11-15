"""Диагональные соединения заборов: модели и multipart-части блокстейтов."""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"
ASSETS = os.path.join(ROOT, "assets", NS)
STATES = os.path.join(ASSETS, "blockstates")
MODELS = os.path.join(ASSETS, "models", "block")


def read(path):
    with open(path, encoding="utf-8") as f:
        return json.load(f)


def write(path, data):
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def post(side_uv):
    """Столбик у дальнего конца диагонали, целиком внутри своего блока: за границу он не выходит, поэтому\n    не накладывается на текстуры соседних блоков и не даёт тёмных полос."""
    return {
        "from": [14, 0, 14], "to": [18, 17, 18],
        "faces": {
            "down": {"uv": side_uv, "texture": "#texture", "shade": False},
            "up": {"uv": side_uv, "texture": "#texture", "shade": False},
            "north": {"uv": side_uv, "texture": "#texture", "shade": False},
            "south": {"uv": side_uv, "texture": "#texture", "shade": False},
            "west": {"uv": side_uv, "texture": "#texture", "shade": False},
            "east": {"uv": side_uv, "texture": "#texture", "shade": False},
        },
    }


def rail(y0, y1, side_uv, end=19.32):
    return {
        "from": [7, y0, 8], "to": [9, y1, end],
        "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": False},
        "faces": {
            "down": {"uv": side_uv, "texture": "#texture", "shade": False},
            "up": {"uv": side_uv, "texture": "#texture", "shade": False},
            "south": {"uv": side_uv, "texture": "#texture", "shade": False},
            "west": {"uv": side_uv, "texture": "#texture", "shade": False},
            "east": {"uv": side_uv, "texture": "#texture", "shade": False},
        },
    }


def main():
    for name in sorted(os.listdir(STATES)):
        if not name.endswith("_fence.json") or name.endswith("_fence_gate.json"):
            continue
        wood = name[:-len("_fence.json")]
        side = read(os.path.join(MODELS, f"{wood}_fence_side.json"))
        texture = side.get("textures", {}).get("texture", f"{NS}:block/{wood}_planks")
        diagonal = f"{wood}_fence_diagonal"
        rails = [rail(6, 9, [7, 7, 9, 10]), rail(12, 15, [7, 1, 9, 4])]
        write(os.path.join(MODELS, diagonal + ".json"), {
            "parent": "minecraft:block/block",
            "ambientocclusion": False,
            "textures": {"particle": texture, "texture": texture},
            "elements": rails,
        })
        # столбик посреди диагонали рисует только одна сторона пары (NE и SE), иначе он двоился бы
        write(os.path.join(MODELS, diagonal + "_post.json"), {
            "parent": "minecraft:block/block",
            "ambientocclusion": False,
            "textures": {"particle": texture, "texture": texture},
            "elements": rails + [post([4, 4, 12, 12])],
        })
        path = os.path.join(STATES, name)
        data = read(path)
        parts = data.setdefault("multipart", [])
        # Убираем старые диагональные части, чтобы не дублировать их
        parts = [p for p in parts if not any(k.startswith("diagonal_") for k in p.get("when", {}))]
        for corner, y, own_post in (("diagonal_south_east", 0, True), ("diagonal_south_west", 90, False),
                                    ("diagonal_north_west", 180, False), ("diagonal_north_east", 270, True)):
            model = f"{NS}:block/{diagonal}" + ("_post" if own_post else "")
            parts.append({"when": {corner: "true"}, "apply": {"model": model, "y": y}})
        data["multipart"] = parts
        write(path, data)
    print("ok")


if __name__ == "__main__":
    main()

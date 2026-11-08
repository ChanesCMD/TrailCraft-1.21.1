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


def faces():
    return {face: {"uv": [0, 0, 16, 16], "texture": "#texture"}
            for face in ("down", "up", "north", "south", "west", "east")}


def rail(y0, y1):
    return {
        "from": [7, y0, 7], "to": [16, y1, 9],
        "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45, "rescale": False},
        "faces": faces(),
    }


def main():
    for name in sorted(os.listdir(STATES)):
        if not name.endswith("_fence.json") or name.endswith("_fence_gate.json"):
            continue
        wood = name[:-len("_fence.json")]
        side = read(os.path.join(MODELS, f"{wood}_fence_side.json"))
        texture = side.get("textures", {}).get("texture", f"{NS}:block/{wood}_planks")
        diagonal = f"{wood}_fence_diagonal"
        write(os.path.join(MODELS, diagonal + ".json"), {
            "parent": "minecraft:block/block",
            "textures": {"particle": texture, "texture": texture},
            "elements": [rail(6, 9), rail(12, 15)],
        })
        path = os.path.join(STATES, name)
        data = read(path)
        parts = data.setdefault("multipart", [])
        existing = {json.dumps(p, sort_keys=True) for p in parts}
        for corner, y in (("diagonal_south_east", 0), ("diagonal_north_east", 90),
                          ("diagonal_north_west", 180), ("diagonal_south_west", 270)):
            part = {"when": {corner: "true"}, "apply": {"model": f"{NS}:block/{diagonal}", "y": y}}
            if json.dumps(part, sort_keys=True) not in existing:
                parts.append(part)
        write(path, data)
    print("ok")


if __name__ == "__main__":
    main()

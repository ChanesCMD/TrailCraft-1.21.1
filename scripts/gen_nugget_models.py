"""
Модели самородка на земле (общие для всех руд, текстура — #all): три формы — models/block/nugget_shape_<n>.json,
самородок на земле случайно принимает одну из них (блокстейт пишет scripts/gen_ores.py — после смены форм
перезапустить и его).

Запуск из корня проекта:  python scripts/gen_nugget_models.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")

# Формы: список кубоидов (from, to в пикселях блока) и поворот вокруг вертикали (−45…45, шаг 22,5)
SHAPES = {
    13: ("Две крупные, три мелкие", [((4, 0, 5), (7, 1.5, 8), 22.5), ((9, 0, 8), (12, 1.5, 11), -22.5), ((10, 0, 4), (11.5, 1, 5.5), 0),
                                     ((4, 0, 10), (5.5, 1, 11.5), 0), ((7.5, 0, 12), (9, 1, 13.5), 22.5)]),
    15: ("Кольцо", [((7, 0, 3), (9, 1, 5), 0), ((11, 0, 5), (13, 1.5, 7), 22.5), ((11, 0, 9.5), (13, 1, 11.5), 0),
                    ((7, 0, 11.5), (9, 1.5, 13.5), 22.5), ((3, 0, 9.5), (5, 1, 11.5), 0), ((3, 0, 5), (5, 1.5, 7), -22.5)]),
    17: ("Разной высоты", [((3, 0, 4), (5, 2, 6), 0), ((7, 0, 3), (9, 1, 5.5), 22.5), ((10, 0, 7), (12, 2.5, 9), 0),
                           ((5, 0, 9), (7.5, 1, 11.5), -22.5), ((9, 0, 11), (11, 1.5, 13), 0)]),
}


def faces(frm, to):
    """Грани с UV по координатам, как в ванильных моделях (низ не рисуется — лежит на земле)."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    return {
        "up": [x1, z1, x2, z2],
        "north": [x1, 16 - y2, x2, 16 - y1],
        "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1],
        "east": [z1, 16 - y2, z2, 16 - y1],
    }


def model(shape):
    elements = []
    for frm, to, angle in SHAPES[shape][1]:
        element = {"from": list(frm), "to": list(to),
                   "faces": {d: {"uv": uv, "texture": "#all"} for d, uv in faces(frm, to).items()}}
        if angle:
            element["rotation"] = {"origin": [8, 0, 8], "axis": "y", "angle": angle}
        elements.append(element)
    return {"parent": "minecraft:block/block", "textures": {"particle": "#all"}, "elements": elements}



if __name__ == "__main__":
    folder = os.path.join(ASSETS, "models", "block")
    for name in os.listdir(folder):
        if name == "nugget_shape.json" or name.startswith("nugget_shape_"):
            os.remove(os.path.join(folder, name))
    for n in SHAPES:
        with open(os.path.join(folder, f"nugget_shape_{n}.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(model(n), f, indent=2)
            f.write("\n")
    print("ok")

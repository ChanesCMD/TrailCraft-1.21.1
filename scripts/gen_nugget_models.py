"""
Модель самородка на земле (общая для всех руд, текстура — #all) и превью 20 вариантов формы (11–20 — на основе «Крупинок»).

Запуск из корня проекта:  python scripts/gen_nugget_models.py [--shape 13,15,17]   (нужен Pillow)
В игру идут выбранные варианты (по умолчанию CHOSEN) — models/block/nugget_shape_<n>.json, самородок на земле случайно
принимает одну из них (блокстейт пишет scripts/gen_ores.py — после смены вариантов перезапустить и его);
лист с 3D-превью всех вариантов —
docs/textures/nugget_models.png (на текстуре самородной меди и малахита).
"""
import json
import math
import os
import sys

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")
DOCS = os.path.join(ROOT, "docs", "textures")
CHOSEN = [13, 15, 17]

# Варианты: список кубоидов (from, to в пикселях блока) и поворот вокруг вертикали (−45…45, шаг 22,5)
SHAPES = {
    1: ("Комок", [((5, 0, 6), (11, 2, 10), 22.5), ((6, 2, 7), (10, 3, 9), 22.5)]),
    2: ("Два комка", [((4, 0, 5), (9, 2.5, 10), 0), ((9.5, 0, 8), (12, 1.5, 10.5), 22.5)]),
    3: ("Россыпь из трёх", [((4, 0, 4), (8, 2, 8), 22.5), ((9, 0, 7), (12, 1.5, 10), -22.5), ((5, 0, 9.5), (7.5, 1, 12), 0)]),
    4: ("Плоский", [((4, 0, 5), (12, 1.5, 11), 22.5), ((6, 1.5, 6.5), (9, 2, 9), 22.5)]),
    5: ("Острый", [((6, 0, 6), (10, 2, 10), 0), ((7, 2, 7), (9, 4, 9), 0), ((7.5, 4, 7.5), (8.5, 5, 8.5), 0)]),
    6: ("Крупинки", [((3, 0, 4), (5, 1, 6), 0), ((7, 0, 3), (9, 1.5, 5.5), 22.5), ((10, 0, 7), (12.5, 1, 9), 0),
                     ((5, 0, 9), (7.5, 1.5, 11.5), -22.5), ((9, 0, 11), (11, 1, 13), 0)]),
    7: ("Ступенчатый", [((4, 0, 4), (12, 1, 12), 45), ((5.5, 1, 5.5), (10.5, 2, 10.5), 45), ((7, 2, 7), (9, 3, 9), 45)]),
    8: ("Вытянутый", [((3, 0, 6.5), (13, 2, 9.5), 45), ((5, 2, 7), (10, 2.5, 9), 45)]),
    9: ("Глыба", [((4, 0, 4), (11, 3, 11), 22.5), ((5, 3, 5), (9, 4, 9), 22.5), ((11, 0, 6), (12.5, 2, 9), 22.5)]),
    10: ("Самородок в камне", [((3, 0, 4), (13, 1, 12), 0), ((5, 1, 5), (11, 2.5, 10), 22.5), ((7, 2.5, 6.5), (9.5, 3.5, 8.5), 22.5)]),
    # Дальше — варианты «Крупинок» (6)
    11: ("Мелкие крупинки", [((3, 0, 5), (4.5, 1, 6.5), 0), ((6, 0, 3), (7.5, 1, 4.5), 22.5), ((9, 0, 5), (10.5, 1, 6.5), 0),
                             ((12, 0, 8), (13.5, 1, 9.5), -22.5), ((4, 0, 10), (5.5, 1, 11.5), 22.5), ((8, 0, 9), (9.5, 1, 10.5), 0),
                             ((10, 0, 12), (11.5, 1, 13.5), 0)]),
    12: ("Комок и крупинки", [((6.5, 0, 6.5), (9.5, 1.5, 9.5), 22.5), ((3, 0, 5), (4.5, 1, 6.5), 0), ((11, 0, 4), (12.5, 1, 5.5), 22.5),
                              ((12, 0, 10), (13.5, 1, 11.5), 0), ((4, 0, 11), (5.5, 1, 12.5), -22.5)]),
    13: ("Две крупные, три мелкие", [((4, 0, 5), (7, 1.5, 8), 22.5), ((9, 0, 8), (12, 1.5, 11), -22.5), ((10, 0, 4), (11.5, 1, 5.5), 0),
                                     ((4, 0, 10), (5.5, 1, 11.5), 0), ((7.5, 0, 12), (9, 1, 13.5), 22.5)]),
    14: ("Цепочка", [((3, 0, 3), (5, 1, 5), 0), ((5.5, 0, 5.5), (7.5, 1.5, 7.5), 22.5), ((8, 0, 8), (10, 1, 10), 0),
                     ((10.5, 0, 10.5), (12, 1.5, 12), -22.5), ((12.5, 0, 12.5), (14, 1, 14), 0)]),
    15: ("Кольцо", [((7, 0, 3), (9, 1, 5), 0), ((11, 0, 5), (13, 1.5, 7), 22.5), ((11, 0, 9.5), (13, 1, 11.5), 0),
                    ((7, 0, 11.5), (9, 1.5, 13.5), 22.5), ((3, 0, 9.5), (5, 1, 11.5), 0), ((3, 0, 5), (5, 1.5, 7), -22.5)]),
    16: ("Тесная кучка", [((6, 0, 6), (8, 1.5, 8), 0), ((8, 0, 6.5), (10, 1, 8.5), 22.5), ((6.5, 0, 8), (8.5, 1, 10), 0),
                          ((8.5, 0, 8.5), (10.5, 2, 10.5), 0), ((5, 0, 8.5), (6.5, 1, 10), 22.5), ((7.5, 1, 7.5), (9, 2, 9), 0)]),
    17: ("Разной высоты", [((3, 0, 4), (5, 2, 6), 0), ((7, 0, 3), (9, 1, 5.5), 22.5), ((10, 0, 7), (12, 2.5, 9), 0),
                           ((5, 0, 9), (7.5, 1, 11.5), -22.5), ((9, 0, 11), (11, 1.5, 13), 0)]),
    18: ("Россыпь по всему блоку", [((2, 0, 2), (3, 1, 3), 0), ((7, 0, 2.5), (8, 1, 3.5), 0), ((12.5, 0, 3), (13.5, 1, 4), 0),
                                    ((4, 0, 7), (5, 1, 8), 0), ((10, 0, 7.5), (11, 1, 8.5), 0), ((2.5, 0, 12), (3.5, 1, 13), 0),
                                    ((7.5, 0, 12.5), (8.5, 1, 13.5), 0), ((12, 0, 12), (13, 1, 13), 0)]),
    19: ("Плоские чешуйки", [((3, 0, 4), (6, 0.5, 6.5), 22.5), ((7, 0, 3), (10, 0.5, 5), 0), ((10, 0, 7), (13, 0.5, 9.5), -22.5),
                             ((4, 0, 9), (7.5, 0.5, 11.5), 0), ((8.5, 0, 11), (11.5, 0.5, 13), 22.5)]),
    20: ("Тройка и одна", [((3, 0, 3), (5.5, 1.5, 5.5), 0), ((5.5, 0, 4), (7.5, 1, 6), 22.5), ((4, 0, 6), (6, 1, 8), 0),
                           ((11, 0, 10.5), (13, 1.5, 12.5), 22.5)]),
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


# === Простой рендер для превью: ортографическая проекция сверху-сбоку, грани по пикселям текстуры ===
YAW, PITCH = math.radians(35), math.radians(32)
SHADE = {"up": 1.0, "south": 0.82, "east": 0.68, "north": 0.6, "west": 0.75}


def rotate_y(p, angle):
    a = math.radians(angle)
    x, y, z = p[0] - 8, p[1], p[2] - 8
    return (x * math.cos(a) + z * math.sin(a) + 8, y, -x * math.sin(a) + z * math.cos(a) + 8)


def project(p, scale, cx, cy):
    x, y, z = p[0] - 8, p[1], p[2] - 8
    xr = x * math.cos(YAW) - z * math.sin(YAW)
    zr = x * math.sin(YAW) + z * math.cos(YAW)
    sx = cx + xr * scale
    sy = cy - (y * math.cos(PITCH) - zr * math.sin(PITCH)) * scale
    depth = zr * math.cos(PITCH) + y * math.sin(PITCH)
    return sx, sy, depth


def face_points(frm, to, face, u, v, du, dv):
    """Угол клетки текстуры (u, v)…(u+du, v+dv) на грани в координатах блока."""
    x1, y1, z1 = frm
    x2, y2, z2 = to
    if face == "up":
        pts = [(u, y2, v), (u + du, y2, v), (u + du, y2, v + dv), (u, y2, v + dv)]
    elif face in ("north", "south"):
        z = z1 if face == "north" else z2
        pts = [(u, 16 - v, z), (u + du, 16 - v, z), (u + du, 16 - v - dv, z), (u, 16 - v - dv, z)]
    else:
        x = x1 if face == "west" else x2
        pts = [(x, 16 - v, u), (x, 16 - v, u + du), (x, 16 - v - dv, u + du), (x, 16 - v - dv, u)]
    return pts


def render(shape, texture, size=240):
    scale = size / 14
    quads = []
    for frm, to, angle in SHAPES[shape][1]:
        for face, (u1, v1, u2, v2) in faces(frm, to).items():
            # Клетки текстуры, попадающие в UV грани (с дробными краями)
            us = sorted({u1, u2, *[k for k in range(math.ceil(u1), math.floor(u2) + 1)]})
            vs = sorted({v1, v2, *[k for k in range(math.ceil(v1), math.floor(v2) + 1)]})
            for i in range(len(us) - 1):
                for j in range(len(vs) - 1):
                    u, v, du, dv = us[i], vs[j], us[i + 1] - us[i], vs[j + 1] - vs[j]
                    if du <= 0 or dv <= 0:
                        continue
                    color = texture.getpixel((min(15, int(u + du / 2)), min(15, int(v + dv / 2))))
                    if color[3] == 0:
                        continue
                    k = SHADE[face]
                    color = tuple(int(c * k) for c in color[:3])
                    pts = [project(rotate_y(p, angle), scale, size / 2, size * 0.62)
                           for p in face_points(frm, to, face, u, v, du, dv)]
                    depth = sum(p[2] for p in pts) / 4
                    quads.append((depth, [(p[0], p[1]) for p in pts], color))
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    for _, pts, color in sorted(quads, key=lambda q: q[0]):
        draw.polygon(pts, fill=color + (255,), outline=color + (255,))
    return img


def preview():
    textures = [Image.open(os.path.join(ASSETS, "textures", "block", f"{ore}_nugget.png")).convert("RGBA")
                for ore in ("native_copper", "malachite")]
    cell, label = 240, 22
    rows = (len(SHAPES) + 4) // 5
    sheet = Image.new("RGBA", (5 * cell, rows * (2 * cell + label)), (70, 74, 78, 255))
    draw = ImageDraw.Draw(sheet)
    try:
        font = ImageFont.truetype("DejaVuSans.ttf", 15)  # шрифт с кириллицей, если есть
    except OSError:
        font = ImageFont.load_default()
    for n in SHAPES:
        col, row = (n - 1) % 5, (n - 1) // 5
        x, y = col * cell, row * (2 * cell + label)
        # Земля под самородком — светлый квадрат 16×16 той же проекции
        ground = Image.new("RGBA", (cell, cell), (0, 0, 0, 0))
        gd = ImageDraw.Draw(ground)
        corners = [project(p, cell / 14, cell / 2, cell * 0.62)[:2] for p in [(0, 0, 0), (16, 0, 0), (16, 0, 16), (0, 0, 16)]]
        gd.polygon(corners, fill=(112, 128, 84, 255))
        for i, texture in enumerate(textures):
            tile = ground.copy()
            tile.alpha_composite(render(n, texture, cell))
            sheet.alpha_composite(tile, (x, y + label + i * cell))
        draw.text((x + 8, y + 5), f"{n}. {SHAPES[n][0]}" + ("  (в игре)" if n in chosen else ""),
                  fill=(255, 255, 255, 255), font=font)
    sheet.save(os.path.join(DOCS, "nugget_models.png"))


if __name__ == "__main__":
    chosen = CHOSEN
    if "--shape" in sys.argv:
        chosen = [int(n) for n in sys.argv[sys.argv.index("--shape") + 1].split(",")]
    folder = os.path.join(ASSETS, "models", "block")
    for name in os.listdir(folder):
        if name == "nugget_shape.json" or name.startswith("nugget_shape_"):
            os.remove(os.path.join(folder, name))
    for n in chosen:
        with open(os.path.join(folder, f"nugget_shape_{n}.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump(model(n), f, indent=2)
            f.write("\n")
    preview()
    print("ok")

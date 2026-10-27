"""Глухарь (animal/Capercaillie, client/animal/CapercaillieModel): текстура, лут, модель яйца призыва, переводы
и превью модели (docs/textures/capercaillie.png).

Развёртка текстуры — коробки CapercaillieModel (texOffs и размеры в BOXES), менять вместе. Нужен Pillow.
Запуск: python scripts/gen_capercaillie.py
"""
import json
import math
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
DOCS = os.path.join(os.path.dirname(__file__), "..", "docs", "textures")
NS = "trailcraft"

# Цвета: самец глухаря — тёмно-серый, грудь с зелёным отливом, крылья бурые с белым пятном у плеча, красная бровь,
# светлый клюв, хвост чёрный с белыми крапинами, брюхо в белых пестринах
SLATE, SLATE_LIGHT, SLATE_DARK = (62, 63, 70), (84, 86, 94), (40, 40, 45)
BLACK, BLACK_LIGHT = (26, 26, 29), (44, 44, 48)
GREEN, GREEN_LIGHT, GREEN_DARK = (34, 76, 66), (58, 108, 90), (24, 50, 46)
BROWN, BROWN_LIGHT, BROWN_DARK = (100, 72, 46), (132, 98, 64), (70, 50, 32)
WHITE = (226, 224, 216)
RED = (204, 40, 34)
BEAK, BEAK_DARK = (222, 214, 184), (176, 166, 136)
LEG, TOE = (112, 102, 90), (78, 76, 74)

rnd = random.Random(7)


def mottle(base, light, dark, p=0.25):
    r = rnd.random()
    return light if r < p / 2 else dark if r < p else base


def neck(a, b):
    return mottle(SLATE, SLATE_LIGHT, SLATE_DARK, 0.3)


def paint_head(face, a, b):
    # a, b — см. paint_box; голова 4×7×4: верхние 3 ряда — голова, ниже — шея
    if face == "front":
        if b <= 1:
            return SLATE_DARK
        return BLACK if 1 <= a <= 2 else BLACK_LIGHT  # горло — чёрная «борода»
    if face in ("west", "east"):
        if b == 1 and a <= 1:
            return RED                                 # красная бровь
        if b == 2 and a == 1:
            return (10, 10, 12)                        # глаз
        if b == 2 and a == 0:
            return RED
        if b >= 3 and a == 0:
            return BLACK
        return neck(a, b)
    if face == "bottom":
        return BLACK
    return neck(a, b)


def paint_body(face, a, b):
    # тело 8×7×12
    if face == "front":
        return mottle(GREEN, GREEN_LIGHT, GREEN_DARK, 0.35) if b < 5 else BLACK
    if face in ("west", "east"):
        if a < 3 and b < 5:
            return mottle(GREEN, GREEN_LIGHT, GREEN_DARK, 0.3)  # грудь заходит на бока
        if b >= 5:
            return WHITE if rnd.random() < 0.18 else BLACK_LIGHT
        return mottle(SLATE, SLATE_LIGHT, SLATE_DARK, 0.35)
    if face == "bottom":
        return WHITE if (rnd.random() < 0.22 and a > 2) else BLACK
    if face == "back":
        return WHITE if b >= 3 and 2 <= a <= 5 and rnd.random() < 0.6 else BLACK_LIGHT
    # спина: пестро-серая, ближе к хвосту буроватая
    return mottle(BROWN_DARK, BROWN, SLATE_DARK, 0.5) if b > 8 else mottle(SLATE, SLATE_LIGHT, SLATE_DARK, 0.35)


def paint_tail(face, a, b):
    # веер 8×9×1: чёрный, у основания (нижние ряды) белые крапины, по краю бурая кайма
    if face in ("front", "back"):
        if b == 0:
            return BROWN_DARK
        if b >= 5 and (a + b) % 3 == 0:
            return WHITE
        return BLACK if (a % 2 == 0) else BLACK_LIGHT
    return BLACK


def paint_wing(face, a, b):
    # крыло 1×5×9: бурое пёстрое, белое пятно у плеча (спереди сверху), маховые к концу темнее
    if face in ("west", "east"):
        if a <= 1 and b <= 1:
            return WHITE
        if b == 4:
            return BROWN_DARK
        return mottle(BROWN, BROWN_LIGHT, BROWN_DARK, 0.45)
    return BROWN_DARK


def paint_beak(face, a, b):
    return BEAK_DARK if (face == "front" and b == 1) or face == "bottom" else BEAK


def paint_beard(face, a, b):
    return BLACK


def paint_leg(face, a, b):
    return mottle(LEG, (130, 120, 106), (92, 84, 74), 0.3)  # ноги оперены до пальцев


def paint_toe(face, a, b):
    return TOE


# Части CapercaillieModel: имя, texOffs, коробка (x, y, z, w, h, d), родитель, pivot, поворот (x, y, z), краска
BOXES = [
    ("head", (0, 20), (-2, -6, -2, 4, 7, 4), None, (0, 13, -5), (0, 0, 0), paint_head),
    ("beak", (16, 20), (-1, -5, -4, 2, 2, 2), "head", (0, 0, 0), (0, 0, 0), paint_beak),
    ("beard", (16, 25), (-1, -3, -3, 2, 2, 1), "head", (0, 0, 0), (0, 0, 0), paint_beard),
    ("body", (0, 0), (-4, -4, -6, 8, 7, 12), None, (0, 16, 0), (0, 0, 0), paint_body),
    ("tail", (24, 20), (-4, -8, 0, 8, 9, 1), None, (0, 14, 5), (-0.9, 0, 0), paint_tail),
    ("right_wing", (0, 32), (-1, 0, -4, 1, 5, 9), None, (-4, 13, 0), (0, 0, 0), paint_wing),
    ("left_wing", (0, 32), (0, 0, -4, 1, 5, 9), None, (4, 13, 0), (0, 0, 0), None),  # mirror — та же развёртка
    ("right_leg", (24, 32), (-1, 0, -1, 2, 5, 2), None, (-2, 19, 1), (0, 0, 0), paint_leg),
    ("right_toe", (24, 40), (-1, 4, -3, 2, 1, 2), None, (-2, 19, 1), (0, 0, 0), paint_toe),
    ("left_leg", (24, 32), (-1, 0, -1, 2, 5, 2), None, (2, 19, 1), (0, 0, 0), None),
    ("left_toe", (24, 40), (-1, 4, -3, 2, 1, 2), None, (2, 19, 1), (0, 0, 0), None),
]


def faces(u, v, w, h, d):
    """Грани коробки, как в ModelPart.Cube: (имя, область развёртки u0, v0, u1, v1, углы (вершина, (u, v))).
    Модель смотрит в −Z, y вниз; верхняя грань (minY) — область сверху слева, её нижний ряд — перед."""
    f4, f5, f6, f7, f8, f9 = u, u + d, u + d + w, u + d + w + w, u + d + w + d, u + d + w + d + w
    f10, f11, f12 = v, v + d, v + d + h
    return [
        ("top", (f5, f10, f6, f11), [(5, (f6, f10)), (4, (f5, f10)), (0, (f5, f11)), (1, (f6, f11))]),
        ("bottom", (f6, f10, f7, f11), [(2, (f7, f11)), (3, (f6, f11)), (7, (f6, f10)), (6, (f7, f10))]),
        ("west", (f4, f11, f5, f12), [(0, (f5, f11)), (4, (f4, f11)), (7, (f4, f12)), (3, (f5, f12))]),
        ("front", (f5, f11, f6, f12), [(1, (f6, f11)), (0, (f5, f11)), (3, (f5, f12)), (2, (f6, f12))]),
        ("east", (f6, f11, f8, f12), [(5, (f8, f11)), (1, (f6, f11)), (2, (f6, f12)), (6, (f8, f12))]),
        ("back", (f8, f11, f9, f12), [(4, (f9, f11)), (5, (f8, f11)), (6, (f8, f12)), (7, (f9, f12))]),
    ]


def paint_box(img, uv, size, paint):
    """a, b у боков — от переда назад и сверху вниз; у переда и зада — слева направо и сверху вниз;
    у верха и низа — поперёк и от переда назад."""
    u, v = uv
    w, h, d = size
    for name, (u0, v0, u1, v1), _ in faces(u, v, w, h, d):
        for py in range(v0, v1):
            for px in range(u0, u1):
                if name == "west":
                    a, b = u1 - 1 - px, py - v0
                elif name == "east":
                    a, b = px - u0, py - v0
                elif name in ("top", "bottom"):
                    a, b = px - u0, v1 - 1 - py
                else:
                    a, b = px - u0, py - v0
                img.putpixel((px, py), paint(name, a, b) + (255,))


def texture():
    img = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    for name, uv, box, _, _, _, paint in BOXES:
        if paint:
            paint_box(img, uv, box[3:], paint)
    return img


def rot(p, rx, ry, rz):
    """Как ModelPart: rotationZYX(z, y, x) — сначала X, потом Y, потом Z."""
    x, y, z = p
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return x, y, z


def preview(tex, yaw, pitch=0.45, scale=14):
    """Простой рендер модели: каждый тексель грани — четырёхугольник, дальние рисуются первыми."""
    parts = {b[0]: b for b in BOXES}

    def to_model(name, p):
        _, _, _, parent, pivot, r, _ = parts[name]
        p = rot(p, *r)
        p = (p[0] + pivot[0], p[1] + pivot[1], p[2] + pivot[2])
        return to_model(parent, p) if parent else p

    def view(p):
        x, y, z = p[0], p[1] - 16, p[2]
        x, z = x * math.cos(yaw) + z * math.sin(yaw), -x * math.sin(yaw) + z * math.cos(yaw)
        y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
        return x, y, z

    quads = []
    for name, (u, v), (bx, by, bz, w, h, d), *_ in BOXES:
        x0, y0, z0, x1, y1, z1 = bx, by, bz, bx + w, by + h, bz + d
        verts = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
                 (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
        for face, _, corners in faces(u, v, w, h, d):
            pts = [view(to_model(name, verts[i])) for i, _ in corners]
            uvs = [c for _, c in corners]
            nx = (pts[1][0] - pts[0][0]) * (pts[2][1] - pts[0][1]) - (pts[1][1] - pts[0][1]) * (pts[2][0] - pts[0][0])
            if abs(nx) < 1e-6:
                continue
            us, vs = sorted({c[0] for c in uvs}), sorted({c[1] for c in uvs})
            for tv in range(int(vs[0]), int(vs[1])):
                for tu in range(int(us[0]), int(us[1])):
                    color = tex.getpixel((tu, tv))
                    if not color[3]:
                        continue

                    def at(su, sv):
                        # билинейно по углам грани: углы 0..3 соответствуют uvs
                        a = (su - uvs[1][0]) / (uvs[0][0] - uvs[1][0])
                        b = (sv - uvs[1][1]) / (uvs[2][1] - uvs[1][1])
                        top = [pts[1][k] + (pts[0][k] - pts[1][k]) * a for k in range(3)]
                        bot = [pts[2][k] + (pts[3][k] - pts[2][k]) * a for k in range(3)]
                        return [top[k] + (bot[k] - top[k]) * b for k in range(3)]

                    q = [at(tu, tv), at(tu + 1, tv), at(tu + 1, tv + 1), at(tu, tv + 1)]
                    depth = sum(p[2] for p in q) / 4
                    shade = {"top": 1.0, "bottom": 0.5, "front": 0.8, "back": 0.8}.get(face, 0.65)
                    quads.append((depth, [(p[0], p[1]) for p in q], tuple(int(c * shade) for c in color[:3])))
    size = 24 * scale
    img = Image.new("RGBA", (size, size), (196, 206, 200, 255))
    draw = ImageDraw.Draw(img)
    for depth, q, color in sorted(quads, key=lambda t: -t[0]):
        draw.polygon([(size / 2 + x * scale, size * 0.62 + y * scale) for x, y in q], fill=color)
    return img


def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def lang():
    names = {"ru_ru": ("Глухарь", "Яйцо призыва глухаря"), "en_us": ("Capercaillie", "Capercaillie Spawn Egg")}
    for code, (mob, egg) in names.items():
        rel = os.path.join(ROOT, "assets", NS, "lang", f"{code}.json")
        with open(rel, encoding="utf-8") as f:
            data = json.load(f)
        data[f"entity.{NS}.capercaillie"] = mob
        data[f"item.{NS}.capercaillie_spawn_egg"] = egg
        write(f"assets/{NS}/lang/{code}.json", data)


def main():
    tex = texture()
    out = os.path.join(ROOT, "assets", NS, "textures", "entity", "capercaillie.png")
    os.makedirs(os.path.dirname(out), exist_ok=True)
    tex.save(out)
    write(f"assets/{NS}/models/item/capercaillie_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
    # Только перья: 1–3, «Добыча» прибавляет по 0–1 за уровень
    write(f"data/{NS}/loot_table/entities/capercaillie.json", {
        "type": "minecraft:entity", "random_sequence": f"{NS}:entities/capercaillie",
        "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": "minecraft:feather",
                   "functions": [
                       {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 1, "max": 3}},
                       {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
                        "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}]}]})
    lang()
    os.makedirs(DOCS, exist_ok=True)
    views = [preview(tex, yaw) for yaw in (math.radians(35), math.radians(-120), math.radians(90))]
    sheet = Image.new("RGBA", (sum(v.width for v in views) + 256, views[0].height), (196, 206, 200, 255))
    x = 0
    for v in views:
        sheet.paste(v, (x, 0))
        x += v.width
    sheet.paste(tex.resize((256, 256), Image.NEAREST), (x, (sheet.height - 256) // 2))
    sheet.save(os.path.join(DOCS, "capercaillie.png"))
    print("ok")


if __name__ == "__main__":
    main()

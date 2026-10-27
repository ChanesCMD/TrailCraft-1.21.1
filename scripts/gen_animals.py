"""Животные TrailCraft (animal/Species, client/animal/TrailAnimalModel): модели, текстуры, лут, корм, где живут,
яйца призыва, переводы и превью (docs/textures/animals.png).

Пишет:
- assets/trailcraft/geo/<id>.json — части модели (коробки, точки поворота, роли), их читает TrailAnimalModel;
- assets/trailcraft/textures/entity/<id>.png — развёртка коробок раскладывается автоматически, краска — PAINT у коробки;
- data/trailcraft/loot_table/entities/<id>.json, tags/item/<id>_food.json, модели яиц призыва, переводы;
- spawners.creature во всех биомах (SPAWNS; ванильных животных в мире нет). gen_forests.py вызывает spawns() в конце.
Таблица видов в Java — animal/Species.java (поведение, размер, здоровье), id совпадают. Нужен Pillow.
Запуск: python scripts/gen_animals.py
"""
import json
import math
import os
import random
import zlib

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
DOCS = os.path.join(os.path.dirname(__file__), "..", "docs", "textures")
NS = "trailcraft"


# ---------------------------------------------------------------- геометрия

def box(x, y, z, w, h, d, paint=None, mirror=False, same=None):
    """Коробка: координаты как в addBox (пиксели, y вниз, морда в −Z); same — взять развёртку другой коробки (имя)."""
    return {"box": [x, y, z, w, h, d], "paint": paint or {}, "mirror": mirror, "same": same}


def part(name, pivot, cubes, role=None, parent=None, rotation=None):
    return {"name": name, "pivot": list(pivot), "cubes": cubes, "role": role, "parent": parent,
            "rotation": list(rotation) if rotation else None}


def legs4(y, h, x, zf, zb, w, paint, d=None):
    """Четыре ноги: правые (−X) и левые (+X, та же развёртка зеркально), передние в zf, задние в zb."""
    d = d or w
    leg = lambda: box(-w / 2, 0, -d / 2, w, h, d, paint)
    return [
        part("leg_fr", (-x, y, zf), [dict(leg(), key="leg_front")], "leg_fr"),
        part("leg_fl", (x, y, zf), [box(-w / 2, 0, -d / 2, w, h, d, mirror=True, same="leg_front")], "leg_fl"),
        part("leg_br", (-x, y, zb), [dict(leg(), key="leg_back")], "leg_br"),
        part("leg_bl", (x, y, zb), [box(-w / 2, 0, -d / 2, w, h, d, mirror=True, same="leg_back")], "leg_bl"),
    ]


def legs2(y, h, x, z, paint, foot):
    """Две ноги птицы с пальцами (foot — краска пальцев)."""
    def cubes(mirror):
        if mirror:
            return [box(-0.5, 0, -0.5, 1, h, 1, mirror=True, same="leg"),
                    box(-1, h - 1, -2, 2, 1, 2, mirror=True, same="toes")]
        return [dict(box(-0.5, 0, -0.5, 1, h, 1, paint), key="leg"), dict(box(-1, h - 1, -2, 2, 1, 2, foot), key="toes")]
    return [part("leg_r", (-x, y, z), cubes(False), "leg_r"), part("leg_l", (x, y, z), cubes(True), "leg_l")]


def wings(x, y, z, h, d, paint, w=1):
    return [part("wing_r", (-x, y, z), [dict(box(-w, 0, -d / 2, w, h, d, paint), key="wing")], "wing_r"),
            part("wing_l", (x, y, z), [box(0, 0, -d / 2, w, h, d, mirror=True, same="wing")], "wing_l")]


def ears(parent, y, x, z, w, h, d, paint, rotation=None):
    return [part("ear_r", (-x, y, z), [dict(box(-w / 2, -h, -d / 2, w, h, d, paint), key="ear")], parent=parent,
                 rotation=rotation),
            part("ear_l", (x, y, z), [box(-w / 2, -h, -d / 2, w, h, d, mirror=True, same="ear")], parent=parent,
                 rotation=(rotation[0], -rotation[1], -rotation[2]) if rotation else None)]


# ---------------------------------------------------------------- краски

def rgb(h):
    return ((h >> 16) & 255, (h >> 8) & 255, h & 255)


def scale(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def P(c, **kw):
    """Краска коробки: c — основной цвет, m — доля пёстрых пикселей; остальное — см. paint()."""
    kw["c"] = rgb(c) if isinstance(c, int) else c
    return kw


def col(c):
    return rgb(c) if isinstance(c, int) else c


def paint(spec, face, a, b, size, rnd, cell):
    """Цвет текселя. a, b у боков (west/east) — от переда назад и сверху вниз; у переда и зада (front/back) — слева
    направо и сверху вниз; у верха и низа (top/bottom) — поперёк и от переда назад. size = (w, h, d)."""
    w, h, d = size
    base = spec["c"]
    m = spec.get("m", 0.22)
    r = rnd.random()
    color = scale(base, 1.12) if r < m / 2 else scale(base, 0.86) if r < m else base
    side = face in ("west", "east")
    flat = face in ("top", "bottom")
    z = a if side else b if flat else (0 if face == "front" else d - 1)  # расстояние от переда
    row = b if not flat else (0 if face == "top" else h - 1)           # ряд сверху
    if "patches" in spec:  # пятна (корова): шум по крупным клеткам, общий для всех граней
        pc, density = spec["patches"]
        if cell(face, a // 3, b // 3) < density:
            color = col(pc)
    if "spots" in spec:
        sc, prob = spec["spots"]
        if rnd.random() < prob:
            color = col(sc)
    if "top" in spec:
        tc, rows = spec["top"]
        if face == "top" or (not flat and row < rows):
            color = col(tc)
    if "belly" in spec:
        bc, rows = spec["belly"]
        if face == "bottom" or (not flat and row >= h - rows):
            color = col(bc)
    if "bot" in spec:
        bc, rows = spec["bot"]
        if face == "bottom" or (not flat and row >= h - rows):
            color = col(bc)
    if "tip" in spec:
        tc, n = spec["tip"]
        if face == "back" or (face != "front" and z >= d - n):
            color = col(tc)
    if "ttip" in spec:
        tc, n = spec["ttip"]
        if face == "top" or (not flat and row < n):
            color = col(tc)
    if "rings" in spec:
        rc, period, width = spec["rings"]
        if face not in ("front", "back") and z % period < width:
            color = col(rc)
    if face == "front" and "front" in spec:
        color = col(spec["front"])
    if face == "back" and "back" in spec:
        color = col(spec["back"])
    if face == "front" and "inner" in spec:  # уши: внутренняя сторона
        ic, border = spec["inner"]
        if border <= a < w - border and row >= border:
            color = col(ic)
    if side and "eyes" in spec:
        er, ez, ec = spec["eyes"][:3]
        if row == er and z == ez:
            color = col(ec)
        if len(spec["eyes"]) > 3 and row == er - 1 and z == ez:  # бровь / ободок над глазом
            color = col(spec["eyes"][3])
    for fface, fa, fb, fc in spec.get("px", []):
        if face == fface and a == fa and b == fb:
            color = col(fc)
    return color


# ---------------------------------------------------------------- животные
# Каждое: ru, en, родительный падеж (для яйца призыва), lie — на сколько пикселей опускается лёжа,
# parts — части. Ноги заканчиваются на y = 24 (земля).

EYE = 0x101012


def chicken():
    body = P(0xAC5C30, m=0.3, belly=(0xC27A4A, 2))
    return dict(ru="Курица", en="Chicken", gen="курицы", lie=0, parts=[
        part("body", (0, 16, 0), [box(-3, -3, -4, 6, 6, 8, body)]),
        part("head", (0, 14, -3), [box(-2, -5, -2, 4, 5, 4, P(0xB86434, m=0.2, eyes=(2, 0, EYE)))], "head"),
        part("comb", (0, 0, 0), [box(-0.5, -7, -2, 1, 2, 3, P(0xD02C24, m=0.1))], parent="head"),
        part("beak", (0, 0, 0), [box(-1, -3, -4, 2, 1, 2, P(0xE0A030, m=0))], parent="head"),
        part("wattle", (0, 0, 0), [box(-1, -2, -3, 2, 2, 1, P(0xD02C24, m=0.1))], parent="head"),
        part("tail", (0, 14, 3), [box(-2, -5, 0, 4, 5, 2, P(0x4A2A1C, m=0.3, ttip=(0x2E4A3A, 1)))], rotation=(-0.4, 0, 0)),
        *wings(3, 13, 0, 4, 6, P(0x96502A, m=0.35, belly=(0x6E3A20, 1))),
        *legs2(19, 5, 1.5, 0, P(0xE0A030, m=0), P(0xD09028, m=0)),
    ])


def goose():
    grey = P(0x8C8276, m=0.3, belly=(0xD6D2C8, 3))
    return dict(ru="Гусь", en="Goose", gen="гуся", lie=0, parts=[
        part("body", (0, 15, 0), [box(-3.5, -3, -5, 7, 6, 10, grey)]),
        part("neck", (0, 13, -4), [box(-1, -8, -1, 2, 9, 2, P(0x6E665E, m=0.2, belly=(0xBEB8AE, 2)))], "head"),
        part("head", (0, 0, 0), [box(-1.5, -11, -2, 3, 3, 4, P(0x5E5850, m=0.15, eyes=(1, 0, EYE)))], parent="neck"),
        part("beak", (0, 0, 0), [box(-1, -10, -5, 2, 1, 3, P(0xE8872C, m=0, px=[("front", 0, 0, 0xF0E8D8),
                                                                              ("front", 1, 0, 0xF0E8D8)]))], parent="neck"),
        part("tail", (0, 13, 5), [box(-2, -2, 0, 4, 2, 3, P(0x6E665E, m=0.2, tip=(0xE8E4DC, 1)))], rotation=(-0.3, 0, 0)),
        *wings(3.5, 12.5, 0.5, 5, 9, P(0x7A7066, m=0.4, belly=(0x5A524A, 1), tip=(0x4A443E, 2))),
        *legs2(18, 6, 1.5, 0, P(0xE8872C, m=0), P(0xD87A24, m=0)),
    ])


def duck():
    return dict(ru="Утка", en="Duck", gen="утки", lie=0, parts=[
        part("body", (0, 19, 0), [box(-2.5, -2, -3.5, 5, 4, 7, P(0x8C8A88, m=0.35, belly=(0xC8C4BE, 1),
                                                                   px=[("front", c, r, 0x7A3E28) for c in range(5) for r in range(3)]))]),
        part("neck", (0, 18, -3), [box(-1, -3, -1, 2, 3, 2, P(0x2E6E3A, m=0.2, belly=(0xF2F0EA, 1)))], "head"),
        part("head", (0, 0, 0), [box(-1.5, -6, -2, 3, 3, 4, P(0x2E6E3A, m=0.25, eyes=(1, 1, EYE)))], parent="neck"),
        part("beak", (0, 0, 0), [box(-1, -5, -5, 2, 1, 3, P(0xE2C23A, m=0))], parent="neck"),
        part("tail", (0, 18, 3.5), [box(-1.5, -2, 0, 3, 2, 2, P(0x2A2A2E, m=0.2, ttip=(0xF2F0EA, 1)))], rotation=(-0.5, 0, 0)),
        *wings(2.5, 17.5, 0, 3, 6, P(0x7A6650, m=0.35, px=[("west", 1, 1, 0x3A4EA0), ("west", 2, 1, 0x3A4EA0),
                                                         ("east", 3, 1, 0x3A4EA0), ("east", 4, 1, 0x3A4EA0)])),
        *legs2(21, 3, 1, 0.5, P(0xE8872C, m=0), P(0xD87A24, m=0)),
    ])


def turkey():
    bronze = P(0x463828, m=0.4, rings=(0x6A5434, 3, 1), belly=(0x2E2418, 2))
    return dict(ru="Индюк", en="Turkey", gen="индюка", lie=0, parts=[
        part("body", (0, 14, 0), [box(-4, -4, -5, 8, 8, 10, bronze)]),
        part("tail", (0, 11, 5), [box(-6, -10, 0, 12, 10, 1, P(0x5A4430, m=0.3, ttip=(0xE6DCC8, 1),
                                                             px=[(f, a, 2, 0x2A2016) for f in ("front", "back") for a in range(12)]))],
             rotation=(-0.2, 0, 0)),
        part("neck", (0, 11, -4), [box(-1, -6, -1, 2, 6, 2, P(0x8098B0, m=0.15))], "head"),
        part("head", (0, 0, 0), [box(-1.5, -9, -2, 3, 3, 3, P(0x8CA4BC, m=0.1, eyes=(1, 0, EYE)))], parent="neck"),
        part("wattle", (0, 0, 0), [box(-1, -7, -3, 2, 4, 1, P(0xC82828, m=0.15))], parent="neck"),
        part("beak", (0, 0, 0), [box(-1, -9, -4, 2, 1, 1, P(0xD8CCA8, m=0))], parent="neck"),
        *wings(4, 11, 0, 6, 9, P(0x6A5640, m=0.35, rings=(0xD8D0C0, 2, 1))),
        *legs2(18, 6, 2, 0.5, P(0xC89A8A, m=0), P(0xB08878, m=0)),
    ])


def corvid(ru, en, gen, size, body_color, head_color, wing_color):
    k = size
    body = P(body_color, m=0.15)
    dark = P(head_color, m=0.12, eyes=(1, 0, 0x6A5A3A))
    wing = P(wing_color, m=0.15)
    if k == 1:
        return dict(ru=ru, en=en, gen=gen, lie=0, parts=[
            part("body", (0, 19, 0), [box(-2, -2, -3, 4, 4, 6, P(body_color, m=0.15, front=wing_color))]),
            part("head", (0, 17.5, -3), [box(-1.5, -3, -2.5, 3, 3, 3, dark)], "head"),
            part("beak", (0, 0, 0), [box(-0.5, -2, -4.5, 1, 1, 2, P(0x1A1A1E, m=0))], parent="head"),
            part("tail", (0, 18, 3), [box(-1.5, -0.5, 0, 3, 1, 4, wing)], rotation=(-0.3, 0, 0)),
            *wings(2, 17.5, -0.5, 2, 7, wing),  # крылья узкие — видно серое туловище
            *legs2(21, 3, 1, 0.5, P(0x1E1E22, m=0), P(0x1E1E22, m=0)),
        ])
    return dict(ru=ru, en=en, gen=gen, lie=0, parts=[
        part("body", (0, 18.5, 0), [box(-2.5, -2.5, -4, 5, 5, 8, body)]),
        part("head", (0, 17, -4), [box(-2, -4, -3, 4, 4, 4, dark)], "head"),
        part("beak", (0, 0, 0), [box(-1, -3, -6, 2, 2, 3, P(0x18181C, m=0))], parent="head"),
        part("throat", (0, 0, 0), [box(-1.5, -1, -3, 3, 2, 1, P(0x22222A, m=0.3))], parent="head"),
        part("tail", (0, 17.5, 4), [box(-2, -0.5, 0, 4, 1, 5, wing)], rotation=(-0.25, 0, 0)),
        *wings(2.5, 16.5, 0, 4, 9, wing),
        *legs2(21, 3, 1.2, 0.5, P(0x18181C, m=0), P(0x18181C, m=0)),
    ])


def crow():
    return corvid("Ворона", "Crow", "вороны", 1, 0x96969A, 0x1E1E22, 0x24242A)


def raven():
    return corvid("Ворон", "Raven", "ворона", 2, 0x1E1E26, 0x1A1A22, 0x22263A)


def pig():
    pink = P(0xE2A096, m=0.25, belly=(0xEAB4AA, 2))
    return dict(ru="Свинья", en="Pig", gen="свиньи", lie=6, parts=[
        part("body", (0, 14, 0), [box(-5, -4, -7, 10, 8, 14, pink)]),
        part("head", (0, 13, -7), [box(-3.5, -3, -6, 7, 7, 6, P(0xE2A096, m=0.2, eyes=(2, 1, EYE)))], "head"),
        part("snout", (0, 0, 0), [box(-2, 0, -8, 4, 3, 2, P(0xD48C86, m=0.1, px=[("front", 1, 1, 0x7A4A48),
                                                                               ("front", 2, 1, 0x7A4A48)]))], parent="head"),
        *ears("head", -3, 2.5, -2, 2, 2, 1, P(0xD8948C, m=0.1), rotation=(0.4, 0, -0.5)),
        part("tail", (0, 11, 7), [box(-0.5, -1, 0, 1, 1, 2, P(0xD8948C, m=0))], "tail"),
        *legs4(18, 6, 3, -5, 5, 4, P(0xE2A096, m=0.2, bot=(0x6E5048, 1))),
    ])


def cow():
    hide = P(0xEAE6DE, m=0.12, patches=(0x2A2624, 0.45))
    return dict(ru="Корова", en="Cow", gen="коровы", lie=8, parts=[
        part("body", (0, 11, 0), [box(-6, -5, -9, 12, 10, 18, hide)]),
        part("udder", (0, 16, 5), [box(-2, 0, -2, 4, 1, 4, P(0xE6A8A0, m=0.1))]),
        part("head", (0, 8, -9), [box(-4, -4, -6, 8, 8, 6, P(0x2A2624, m=0.12, front=0xEAE6DE, eyes=(3, 1, EYE)))], "head"),
        part("muzzle", (0, 0, 0), [box(-3, 1, -8, 6, 3, 2, P(0xE6B0A8, m=0.1, px=[("front", 1, 1, 0x5A3A38),
                                                                                ("front", 4, 1, 0x5A3A38)]))], parent="head"),
        part("horn_r", (-4, -4, -3), [dict(box(-1, -3, -0.5, 1, 3, 1, P(0xDCCFB0, m=0, ttip=(0x5A5040, 1))), key="horn")],
             parent="head", rotation=(0, 0, -0.4)),
        part("horn_l", (4, -4, -3), [box(0, -3, -0.5, 1, 3, 1, mirror=True, same="horn")], parent="head", rotation=(0, 0, 0.4)),
        *ears("head", -2, 4.5, -2, 3, 2, 1, P(0x2A2624, m=0.1), rotation=(0, 0, -1.3)),
        part("tail", (0, 7, 9), [box(-0.5, 0, 0, 1, 9, 1, P(0xEAE6DE, m=0.1, bot=(0x2A2624, 2)))], "tail", rotation=(0.2, 0, 0)),
        *legs4(16, 8, 4, -7, 7, 4, P(0xEAE6DE, m=0.1, patches=(0x2A2624, 0.35), bot=(0x3A3430, 1))),
    ])


def sheep():
    wool = P(0xE8E6DE, m=0.4)
    return dict(ru="Овца", en="Sheep", gen="овцы", lie=7, parts=[
        part("body", (0, 13, 0), [box(-4, -3, -7, 8, 6, 14, P(0xD8C8B0, m=0.2))]),
        part("wool", (0, 13, 0), [box(-5, -4, -8, 10, 8, 16, wool)], "wool"),
        part("head", (0, 11, -7), [box(-2.5, -3, -5, 5, 5, 6, P(0xDCCDB4, m=0.15, eyes=(1, 1, 0x3A2A1C)))], "head"),
        part("head_wool", (0, 0, 0), [box(-3, -4, -3, 6, 2, 4, P(0xE8E6DE, m=0.4))], "wool", parent="head"),
        part("nose", (0, 0, 0), [box(-1.5, 0, -5.5, 3, 2, 1, P(0xC8B49A, m=0.1, px=[("front", 1, 0, 0x6A5444)]))], parent="head"),
        *ears("head", -2, 3, -2, 1, 3, 2, P(0xDCCDB4, m=0.1), rotation=(0, 0, -1.2)),
        part("tail", (0, 10, 8), [box(-1, 0, -1, 2, 4, 2, wool)], "tail", rotation=(0.3, 0, 0)),
        *legs4(17, 7, 2.5, -5, 5, 3, P(0xDCCDB4, m=0.15, bot=(0x4A3C30, 1))),
    ])


def goat():
    coat = P(0xDCD8CE, m=0.3, top=(0xC8C2B4, 1))
    return dict(ru="Коза", en="Goat", gen="козы", lie=8, parts=[
        part("body", (0, 13, 0), [box(-3.5, -4, -7, 7, 7, 13, coat)]),
        part("neck", (0, 10, -6), [box(-1.5, -3, -2, 3, 4, 3, P(0xDCD8CE, m=0.25))], "head"),
        part("head", (0, 0, 0), [box(-2, -6, -6, 4, 4, 5, P(0xDCD8CE, m=0.2, eyes=(1, 2, 0x6A5A20)))], parent="neck"),
        part("beard", (0, 0, 0), [box(-0.5, -2, -5.5, 1, 3, 1, P(0xBEB6A6, m=0.2))], parent="neck"),
        part("horn_r", (-1.2, -6, -2.5), [dict(box(-0.5, -4, -0.5, 1, 4, 1, P(0x968A6E, m=0.2, ttip=(0x5A5040, 1))), key="horn")],
             "horn_r", parent="neck", rotation=(-0.6, 0, -0.15)),
        part("horn_l", (1.2, -6, -2.5), [box(-0.5, -4, -0.5, 1, 4, 1, mirror=True, same="horn")], "horn_l", parent="neck",
             rotation=(-0.6, 0, 0.15)),
        *ears("neck", -5, 2.5, -3.5, 1, 2, 1, P(0xCFC8BA, m=0.1), rotation=(0, 0, -1.4)),
        part("tail", (0, 9, 6), [box(-0.5, -1, 0, 1, 2, 2, coat)], "tail", rotation=(-0.6, 0, 0)),
        *legs4(16, 8, 2, -5, 5, 2, P(0xD0CABC, m=0.2, bot=(0x4A4038, 1))),
    ])


def hare():
    fur = P(0x967652, m=0.4, belly=(0xF0E8DC, 1))
    return dict(ru="Заяц", en="Hare", gen="зайца", lie=0, parts=[
        part("body", (0, 18.5, 0), [box(-2.5, -2.5, -4, 5, 5, 8, fur)]),
        part("head", (0, 16.5, -4), [box(-2, -3, -3.5, 4, 4, 4, P(0x9E7E58, m=0.3, eyes=(1, 1, 0x3A2410),
                                                                   px=[("front", 1, 3, 0xF0E8DC), ("front", 2, 3, 0xF0E8DC)]))], "head"),
        *ears("head", -3, 1, -1, 1, 6, 2, P(0x9E7E58, m=0.2, ttip=(0x1E1A16, 1), inner=(0xD8B0A0, 0)), rotation=(-0.25, 0, 0.1)),
        part("tail", (0, 16.5, 4), [box(-1, -1, 0, 2, 2, 2, P(0xF6F2EA, m=0.1, top=(0x4A3A28, 1)))], "tail"),
        part("leg_fr", (-1.2, 21, -3), [dict(box(-0.5, 0, -0.5, 1, 3, 1, P(0x9E7E58, m=0.2)), key="fleg")], "leg_fr"),
        part("leg_fl", (1.2, 21, -3), [box(-0.5, 0, -0.5, 1, 3, 1, mirror=True, same="fleg")], "leg_fl"),
        part("leg_br", (-1.8, 21, 2.5), [dict(box(-1, 0, -3, 2, 3, 4, P(0x8E6E4A, m=0.3, bot=(0xE8E0D2, 1))), key="bleg")], "leg_br"),
        part("leg_bl", (1.8, 21, 2.5), [box(-1, 0, -3, 2, 3, 4, mirror=True, same="bleg")], "leg_bl"),
    ])


def capybara():
    fur = P(0x8C603C, m=0.35, belly=(0x9E7048, 2))
    return dict(ru="Капибара", en="Capybara", gen="капибары", lie=6, parts=[
        part("body", (0, 14, 0), [box(-4.5, -4, -7.5, 9, 8, 15, fur)]),
        part("head", (0, 12, -7), [box(-3, -3, -8, 6, 6, 8, P(0x8C603C, m=0.3, eyes=(1, 3, EYE),
                                                            front=0x4E3420, bot=(0x7A5232, 1)))], "head"),
        *ears("head", -3, 2.5, -1, 1, 1, 1, P(0x5E4028, m=0)),
        *legs4(18, 6, 3, -5, 5, 3, P(0x7A5232, m=0.25, bot=(0x3A2A1C, 1))),
    ])


def beaver():
    fur = P(0x6E482C, m=0.35, belly=(0x8A603C, 1))
    return dict(ru="Бобр", en="Beaver", gen="бобра", lie=0, parts=[
        part("body", (0, 18, 0), [box(-3.5, -3, -5, 7, 6, 10, fur)]),
        part("head", (0, 17, -5), [box(-2.5, -3, -4, 5, 4, 4, P(0x6E482C, m=0.3, eyes=(0, 1, EYE),
                                                             px=[("front", 2, 2, 0x1E1614), ("front", 2, 1, 0x2A201A)]))], "head"),
        part("teeth", (0, 0, 0), [box(-1, 1, -4.2, 2, 1, 1, P(0xE08A2A, m=0))], parent="head"),
        *ears("head", -3, 2.5, -1, 1, 1, 1, P(0x4E3220, m=0)),
        part("tail", (0, 19.5, 5), [box(-2, -0.5, 0, 4, 1, 6, P(0x3C3632, m=0.2, rings=(0x2A2624, 2, 1)))], "tail",
             rotation=(0.15, 0, 0)),
        *legs4(21, 3, 2.2, -3.5, 3.5, 2, P(0x3E2C1E, m=0.2)),
    ])


def fox_like(ru, en, gen, fur, light, dark, ear_size, ear_inner, tail_tip, legs):
    f = P(fur, m=0.25, belly=(light, 2))
    head = P(fur, m=0.2, eyes=(1, 1, 0x2A1E10), bot=(light, 1))
    ew, eh = ear_size
    return dict(ru=ru, en=en, gen=gen, lie=5, parts=[
        part("body", (0, 16.5, 0), [box(-2.5, -2.5, -5, 5, 5, 10, f)]),
        part("head", (0, 15, -5), [box(-2.5, -2.5, -4, 5, 4, 4, head)], "head"),
        part("snout", (0, 0, 0), [box(-1, 0, -6, 2, 2, 2, P(light, m=0.1, px=[("front", 0, 0, 0x141210),
                                                                            ("front", 1, 0, 0x141210)]))], parent="head"),
        *ears("head", -2.5, 1.5, -2, ew, eh, 1, P(fur, m=0.1, ttip=(dark, 1), inner=(ear_inner, 0 if ew < 2 else 1 // 1)),
              rotation=(0, 0, 0.15 if ew > 2 else 0)),
        part("tail", (0, 15, 5), [box(-1.5, -1.5, 0, 3, 3, 8, P(fur, m=0.25, tip=(tail_tip, 2)))], "tail",
             rotation=(-0.5, 0, 0)),
        *legs4(19, 5, 1.5, -3.5, 3.5, 2, P(legs, m=0.15)),
    ])


def fennec():
    return fox_like("Фенек", "Fennec Fox", "фенека", 0xE2C896, 0xF6E6C8, 0xC8A878, (3, 5), 0xE8B4A4, 0x3A2A1C, 0xE2C896)


def arctic_fox():
    return fox_like("Песец", "Arctic Fox", "песца", 0xECECE8, 0xFAFAFA, 0xC8CCD2, (2, 2), 0xD8D8D8, 0xFAFAFA, 0xE2E2DE)


def fox():
    return fox_like("Лиса", "Fox", "лисы", 0xD66E28, 0xF4F0E8, 0x2A2420, (2, 3), 0xF4F0E8, 0xF4F0E8, 0x2A2420)


def hedgehog():
    return dict(ru="Ёж", en="Hedgehog", gen="ежа", lie=0, parts=[
        part("body", (0, 20, 0), [box(-3, -3, -4, 6, 5, 8, P(0xC8B496, m=0.2))]),
        part("spines", (0, 20, 0), [box(-3.5, -3.5, -4.5, 7, 4, 9, P(0x5A4632, m=0.2, spots=(0xE0D6C2, 0.3), bot=(0x5A4632, 0)))]),
        part("head", (0, 20, -4), [box(-2, -1.5, -3, 4, 3, 3, P(0xC8B496, m=0.15, eyes=(0, 1, EYE)))], "head"),
        part("snout", (0, 0, 0), [box(-1, 0, -4.5, 2, 1, 2, P(0xBCA888, m=0, px=[("front", 0, 0, 0x141210),
                                                                               ("front", 1, 0, 0x141210)]))], parent="head"),
        *legs4(22, 2, 1.8, -2.5, 2.5, 1, P(0x8A7660, m=0)),
    ])


def manul():
    fur = P(0x968C7E, m=0.45, belly=(0xB4AA9A, 2))
    return dict(ru="Манул", en="Pallas's Cat", gen="манула", lie=5, parts=[
        part("body", (0, 16, 0), [box(-3.5, -3, -6, 7, 6, 12, P(0x968C7E, m=0.45, belly=(0xB4AA9A, 2),
                                                             rings=(0x6E665A, 4, 1)))]),
        part("head", (0, 14, -6), [box(-3, -2.5, -4, 6, 5, 4, P(0x9E9484, m=0.35, eyes=(1, 0, 0xC8B030),
                                                            px=[("front", c, 0, 0x3A342C) for c in (1, 3, 5)]
                                                            + [("front", 1, 1, 0xD2CC9A), ("front", 4, 1, 0xD2CC9A),
                                                               ("front", 1, 2, 0x2A2420), ("front", 4, 2, 0x2A2420),
                                                               ("front", 0, 3, 0x3A342C), ("front", 5, 3, 0x3A342C)]))],
             "head"),
        part("muzzle", (0, 0, 0), [box(-1.5, 0.5, -5, 3, 2, 1, P(0xE0DACC, m=0.1, px=[("front", 1, 0, 0x2A2420)]))],
             parent="head"),
        *ears("head", -2, 2, -2, 2, 1, 1, P(0x8A8070, m=0.1, ttip=(0x3A342C, 1))),
        part("tail", (0, 14, 6), [box(-1, -1, 0, 2, 2, 8, P(0x8A8070, m=0.3, rings=(0x2A2420, 3, 1), tip=(0x1E1A16, 2)))],
             "tail", rotation=(-0.6, 0, 0)),
        *legs4(19, 5, 2, -4, 4, 2, fur),
    ])


def wolf():
    grey = P(0x82807C, m=0.35, belly=(0xB4B0AA, 2), top=(0x5C5A58, 1))
    return dict(ru="Волк", en="Wolf", gen="волка", lie=6, parts=[
        part("body", (0, 14, 1), [box(-3, -3, -6, 6, 6, 11, grey)]),
        part("mane", (0, 14, -4), [box(-4, -4, -3, 8, 8, 6, P(0x8E8C86, m=0.4, belly=(0xBCB8B0, 3), top=(0x5C5A58, 1)))]),
        part("head", (0, 12.5, -7), [box(-3, -3, -4, 6, 6, 4, P(0x8A8882, m=0.3, eyes=(2, 0, 0xB89A30), bot=(0xC8C4BC, 1)))],
             "head"),
        part("snout", (0, 0, 0), [box(-1.5, 0, -7, 3, 3, 3, P(0x9C9890, m=0.2, bot=(0xD4D0C8, 1),
                                                            px=[("front", 1, 0, 0x141210)]))], parent="head"),
        *ears("head", -3, 2, -2, 2, 2, 1, P(0x6E6C68, m=0.1, ttip=(0x3A3836, 1))),
        part("tail", (0, 12, 6.5), [box(-1, 0, -1, 2, 8, 2, P(0x7A7874, m=0.3, bot=(0x2E2C2A, 2)))], "tail"),
        *legs4(17, 7, 1.5, -5, 4.5, 2, P(0x8A8882, m=0.25, bot=(0xB4B0AA, 2))),
    ])


ANIMALS = {
    "chicken": chicken, "goose": goose, "duck": duck, "turkey": turkey, "crow": crow, "raven": raven,
    "pig": pig, "cow": cow, "sheep": sheep, "goat": goat, "hare": hare,
    "capybara": capybara, "beaver": beaver, "fennec": fennec, "hedgehog": hedgehog, "manul": manul,
    "arctic_fox": arctic_fox, "fox": fox, "wolf": wolf,
}

# ---------------------------------------------------------------- лут, корм, где живут

BERRIES = [f"{NS}:currant", f"{NS}:cloudberry", f"{NS}:gooseberry", f"{NS}:blackberry"]
SEEDS = [f"{NS}:jute_seeds", "#minecraft:chicken_food"]
FOOD = {
    "chicken": SEEDS, "goose": SEEDS, "duck": SEEDS, "turkey": SEEDS,
    "crow": SEEDS + BERRIES, "raven": SEEDS + BERRIES,
    "pig": BERRIES, "cow": [f"{NS}:jute"], "sheep": [f"{NS}:jute"], "goat": [f"{NS}:jute", f"{NS}:stick_branch"],
    "hare": [], "capybara": [f"{NS}:jute", "minecraft:carrot", "minecraft:potato"], "beaver": [f"{NS}:stick_branch"],
    "fennec": BERRIES, "hedgehog": BERRIES, "manul": [f"{NS}:raw_capercaillie", "minecraft:chicken"],
    "arctic_fox": BERRIES, "fox": BERRIES, "wolf": [f"{NS}:raw_capercaillie"],
}
# Лут: (предмет, мин, макс[, жарится — горящее животное роняет жареное]); у овцы — ванильный лут по цвету шерсти
LOOT = {
    "chicken": [("minecraft:feather", 0, 2), ("minecraft:chicken", 1, 1, True)],
    "goose": [("minecraft:feather", 1, 3), ("minecraft:chicken", 1, 2, True)],
    "duck": [("minecraft:feather", 0, 2), ("minecraft:chicken", 1, 1, True)],
    "turkey": [("minecraft:feather", 1, 3), ("minecraft:chicken", 2, 3, True)],
    "crow": [("minecraft:feather", 0, 2)],
    "raven": [("minecraft:feather", 0, 2)],
    "pig": [("minecraft:porkchop", 1, 3, True)],
    "cow": [("minecraft:beef", 1, 3, True), ("minecraft:leather", 0, 2)],
    "goat": [("minecraft:mutton", 1, 2, True), ("minecraft:leather", 0, 1)],
    "hare": [("minecraft:rabbit", 0, 1, True), ("minecraft:rabbit_hide", 0, 1)],
    "capybara": [("minecraft:porkchop", 1, 2, True), ("minecraft:leather", 0, 1)],
    "beaver": [("minecraft:leather", 0, 1)],
}
# Где живут: биом → [(вид, вес, сколько в группе от, до)]
SPAWNS = {
    "burozem_plains": [("hare", 6, 1, 2), ("crow", 4, 1, 2), ("sheep", 5, 2, 4), ("cow", 5, 2, 4), ("chicken", 4, 2, 4),
                       ("goose", 3, 2, 4), ("hedgehog", 3, 1, 1)],
    "chernozem_plains": [("cow", 6, 2, 4), ("sheep", 4, 2, 4), ("hare", 6, 1, 2), ("crow", 4, 1, 2), ("chicken", 4, 2, 4),
                         ("turkey", 3, 2, 3), ("goose", 2, 2, 4), ("manul", 1, 1, 1)],
    "zheltozem_plains": [("hare", 4, 1, 2), ("fennec", 2, 1, 2), ("chicken", 3, 2, 4), ("turkey", 3, 2, 3), ("crow", 3, 1, 2),
                         ("goat", 3, 2, 3)],
    "arboretum": [("hare", 4, 1, 2), ("crow", 3, 1, 2), ("hedgehog", 3, 1, 1)],
    "winter_test": [("arctic_fox", 4, 1, 2), ("hare", 4, 1, 2)],
    "burozem_taiga": [("wolf", 6, 2, 4), ("fox", 5, 1, 2), ("hare", 5, 1, 2), ("raven", 4, 1, 2), ("capercaillie", 8, 2, 4)],
    "pine_forest": [("wolf", 4, 2, 4), ("fox", 4, 1, 2), ("hare", 4, 1, 2), ("raven", 4, 1, 2), ("capercaillie", 8, 2, 4)],
    "birch_forest": [("fox", 4, 1, 2), ("hare", 5, 1, 2), ("hedgehog", 4, 1, 1), ("crow", 3, 1, 2), ("capercaillie", 4, 2, 3)],
    "burozem_forest": [("pig", 6, 2, 4), ("wolf", 4, 2, 4), ("hedgehog", 4, 1, 1), ("fox", 3, 1, 2), ("crow", 3, 1, 2)],
    "maple_forest": [("hedgehog", 4, 1, 1), ("pig", 4, 2, 4), ("wolf", 3, 2, 4), ("crow", 3, 1, 2), ("fox", 3, 1, 2)],
    "alder_forest": [("beaver", 4, 1, 2), ("hedgehog", 3, 1, 1), ("duck", 4, 2, 4)],
    "thuja_forest": [("wolf", 3, 2, 4), ("raven", 3, 1, 2), ("fox", 3, 1, 2), ("hedgehog", 2, 1, 1)],
    "burozem_willow_forest": [("beaver", 5, 1, 2), ("duck", 6, 2, 4), ("goose", 4, 2, 4)],
    "chernozem_willow_forest": [("beaver", 4, 1, 2), ("duck", 6, 2, 4), ("goose", 4, 2, 4)],
    "chernozem_forest_steppe": [("hare", 6, 1, 2), ("fox", 4, 1, 2), ("turkey", 3, 2, 3), ("crow", 4, 1, 2), ("pig", 3, 2, 4),
                                ("hedgehog", 3, 1, 1)],
    "walnut_grove": [("pig", 4, 2, 4), ("turkey", 4, 2, 3), ("hedgehog", 3, 1, 1), ("crow", 3, 1, 2)],
    "burozem_river": [("duck", 6, 2, 4), ("goose", 4, 2, 4), ("beaver", 4, 1, 2)],
    "chernozem_river": [("duck", 6, 2, 4), ("goose", 4, 2, 4), ("beaver", 3, 1, 2)],
    "zheltozem_river": [("capybara", 6, 2, 4), ("duck", 4, 2, 4)],
    "burozem_shore": [("crow", 3, 1, 2), ("goose", 2, 2, 4)],
    "chernozem_shore": [("crow", 3, 1, 2), ("goose", 2, 2, 4)],
    "zheltozem_shore": [("crow", 3, 1, 2), ("capybara", 2, 2, 3)],
    "desert": [("fennec", 4, 1, 2)],
    "red_desert": [("fennec", 4, 1, 2)],
    "savanna": [("capybara", 3, 2, 3), ("fennec", 2, 1, 2), ("crow", 2, 1, 2), ("goat", 2, 2, 3)],
    "eucalyptus_forest": [("turkey", 3, 2, 3), ("pig", 2, 2, 3), ("crow", 2, 1, 2)],
    "ebony_forest": [("capybara", 4, 2, 3), ("crow", 2, 1, 2)],
    "palm_shore": [("capybara", 6, 2, 4), ("duck", 2, 2, 3)],
    "ironwood_forest": [("pig", 3, 2, 3), ("turkey", 3, 2, 3)],
    "lilac_grove": [("hedgehog", 3, 1, 1), ("hare", 3, 1, 2), ("crow", 2, 1, 2)],
    "sakura_grove": [("hare", 3, 1, 2), ("crow", 2, 1, 2), ("hedgehog", 2, 1, 1)],
    "mountain_meadow": [("sheep", 6, 2, 4), ("goat", 6, 2, 4), ("hare", 4, 1, 2), ("manul", 2, 1, 1), ("cow", 3, 2, 3)],
    "snowy_slopes": [("goat", 6, 2, 4), ("arctic_fox", 4, 1, 2), ("hare", 4, 1, 2), ("raven", 3, 1, 2)],
    "snowy_peaks": [("goat", 4, 1, 3), ("arctic_fox", 3, 1, 2), ("raven", 2, 1, 2)],
    "stony_peaks": [("goat", 6, 2, 4), ("manul", 4, 1, 1), ("raven", 3, 1, 2)],
}


# ---------------------------------------------------------------- развёртка и текстура

def net(w, h, d):
    return 2 * math.ceil(d) + 2 * math.ceil(w), math.ceil(d) + math.ceil(h)


def layout(animal):
    """Раскладывает развёртки коробок по текстуре полками (крупные первыми); возвращает размер текстуры."""
    cubes = [c for p in animal["parts"] for c in p["cubes"] if not c["same"]]
    order = sorted(cubes, key=lambda c: -net(*c["box"][3:])[1] * 100 - net(*c["box"][3:])[0])
    width = 64 if max(net(*c["box"][3:])[0] for c in cubes) <= 64 else 128
    x = y = shelf = 0
    for c in order:
        nw, nh = net(*c["box"][3:])
        if x + nw > width:
            x, y, shelf = 0, y + shelf, 0
        c["uv"] = [x, y]
        x += nw
        shelf = max(shelf, nh)
    height = 32
    while height < y + shelf:
        height *= 2
    by_key = {c.get("key"): c for p in animal["parts"] for c in p["cubes"] if c.get("key")}
    for p in animal["parts"]:
        for c in p["cubes"]:
            if c["same"]:
                c["uv"] = by_key[c["same"]]["uv"]
    return width, height


def faces(u, v, w, h, d):
    """Грани коробки, как в ModelPart.Cube: (имя, область развёртки u0, v0, u1, v1, углы (вершина, (u, v)))."""
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


def texture(name, animal, size):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    rnd = random.Random(zlib.crc32(name.encode()))
    for p in animal["parts"]:
        for c in p["cubes"]:
            if c["same"]:
                continue
            w, h, d = (math.ceil(v) for v in c["box"][3:])
            u, v = c["uv"]
            seed = zlib.crc32((name + p["name"]).encode())
            cell = lambda face, cx, cy: random.Random(seed ^ (cx * 7919 + cy * 104729 + zlib.crc32(face.encode()) % 997)).random()
            for fname, (u0, v0, u1, v1), _ in faces(u, v, w, h, d):
                for py in range(v0, v1):
                    for px in range(u0, u1):
                        if fname == "west":
                            a, b = u1 - 1 - px, py - v0
                        elif fname == "east":
                            a, b = px - u0, py - v0
                        elif fname in ("top", "bottom"):
                            a, b = px - u0, v1 - 1 - py
                        else:
                            a, b = px - u0, py - v0
                        img.putpixel((px, py), paint(c["paint"], fname, a, b, (w, h, d), rnd, cell) + (255,))
    return img


# ---------------------------------------------------------------- превью

def rot(p, rx, ry, rz):
    x, y, z = p
    y, z = y * math.cos(rx) - z * math.sin(rx), y * math.sin(rx) + z * math.cos(rx)
    x, z = x * math.cos(ry) + z * math.sin(ry), -x * math.sin(ry) + z * math.cos(ry)
    x, y = x * math.cos(rz) - y * math.sin(rz), x * math.sin(rz) + y * math.cos(rz)
    return x, y, z


def preview(animal, tex, yaw=math.radians(35), pitch=0.4, px=9, size=300):
    parts = {p["name"]: p for p in animal["parts"]}

    def to_model(name, point):
        p = parts[name]
        point = rot(point, *(p["rotation"] or (0, 0, 0)))
        point = tuple(point[i] + p["pivot"][i] for i in range(3))
        return to_model(p["parent"], point) if p["parent"] else point

    def view(point):
        x, y, z = point[0], point[1] - 16, point[2]
        x, z = x * math.cos(yaw) + z * math.sin(yaw), -x * math.sin(yaw) + z * math.cos(yaw)
        y, z = y * math.cos(pitch) - z * math.sin(pitch), y * math.sin(pitch) + z * math.cos(pitch)
        return x, y, z

    quads = []
    for p in animal["parts"]:
        for c in p["cubes"]:
            bx, by, bz, w, h, d = c["box"]
            x0, x1 = (bx + w, bx) if c["mirror"] else (bx, bx + w)
            y0, z0, y1, z1 = by, bz, by + h, bz + d
            verts = [(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0),
                     (x0, y0, z1), (x1, y0, z1), (x1, y1, z1), (x0, y1, z1)]
            u, v = c["uv"]
            for fname, _, corners in faces(u, v, *(math.ceil(s) for s in (w, h, d))):
                pts = [view(to_model(p["name"], verts[i])) for i, _ in corners]
                uvs = [cc for _, cc in corners]
                us, vs = sorted({cc[0] for cc in uvs}), sorted({cc[1] for cc in uvs})
                for tv in range(int(vs[0]), int(vs[-1])):
                    for tu in range(int(us[0]), int(us[-1])):
                        color = tex.getpixel((tu, tv))

                        def at(su, sv):
                            a = (su - uvs[1][0]) / (uvs[0][0] - uvs[1][0])
                            b = (sv - uvs[1][1]) / (uvs[2][1] - uvs[1][1])
                            top = [pts[1][k] + (pts[0][k] - pts[1][k]) * a for k in range(3)]
                            bot = [pts[2][k] + (pts[3][k] - pts[2][k]) * a for k in range(3)]
                            return [top[k] + (bot[k] - top[k]) * b for k in range(3)]

                        q = [at(tu, tv), at(tu + 1, tv), at(tu + 1, tv + 1), at(tu, tv + 1)]
                        shade = {"top": 1.0, "bottom": 0.5, "front": 0.8, "back": 0.8}.get(fname, 0.65)
                        quads.append((sum(pt[2] for pt in q) / 4, [(pt[0], pt[1]) for pt in q],
                                      tuple(int(cv * shade) for cv in color[:3])))
    img = Image.new("RGBA", (size, size), (196, 206, 200, 255))
    draw = ImageDraw.Draw(img)
    for _, q, color in sorted(quads, key=lambda t: -t[0]):
        draw.polygon([(size / 2 + x * px, size * 0.6 + y * px) for x, y in q], fill=color)
    return img


# ---------------------------------------------------------------- запись

def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(rel):
    with open(os.path.join(ROOT, *rel.split("/")), encoding="utf-8") as f:
        return json.load(f)


def loot(name):
    looting = {"function": "minecraft:enchanted_count_increase", "enchantment": "minecraft:looting",
               "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}
    pools = []
    for item, lo, hi, *cook in LOOT[name]:
        functions = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
        if cook:
            functions.append({"function": "minecraft:furnace_smelt", "conditions": [{
                "condition": "minecraft:entity_properties", "entity": "this",
                "predicate": {"flags": {"is_on_fire": True}}}]})
        functions.append(looting)
        pools.append({"rolls": 1, "bonus_rolls": 0, "entries": [{"type": "minecraft:item", "name": item, "functions": functions}]})
    write(f"data/{NS}/loot_table/entities/{name}.json",
          {"type": "minecraft:entity", "random_sequence": f"{NS}:entities/{name}", "pools": pools})


def spawns():
    """spawners.creature всех биомов — из SPAWNS (ванильных животных нет)."""
    folder = os.path.join(ROOT, "data", NS, "worldgen", "biome")
    for file in sorted(os.listdir(folder)):
        biome = file[:-len(".json")]
        rel = f"data/{NS}/worldgen/biome/{file}"
        data = read(rel)
        creatures = [{"type": f"{NS}:{mob}", "weight": w, "minCount": lo, "maxCount": hi}
                     for mob, w, lo, hi in SPAWNS.get(biome, [])]
        if data["spawners"].get("creature", []) != creatures:  # у морей своё оформление JSON — не трогаем зря
            data["spawners"]["creature"] = creatures
            write(rel, data)


def lang(animals):
    for code in ("ru_ru", "en_us"):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        for name, a in animals.items():
            data[f"entity.{NS}.{name}"] = a["ru"] if code == "ru_ru" else a["en"]
            data[f"item.{NS}.{name}_spawn_egg"] = (f"Яйцо призыва {a['gen']}" if code == "ru_ru" else f"{a['en']} Spawn Egg")
        data[f"itemGroup.{NS}_spawn_eggs"] = "TrailCraft: Яйца призыва" if code == "ru_ru" else "TrailCraft: Spawn Eggs"
        write(rel, data)


def main():
    animals = {name: make() for name, make in ANIMALS.items()}
    previews = []
    for name, a in animals.items():
        size = layout(a)
        tex = texture(name, a, size)
        tex.save(os.path.join(ROOT, "assets", NS, "textures", "entity", f"{name}.png"))
        write(f"assets/{NS}/geo/{name}.json", {
            "texture": list(size), "lie": a["lie"],
            "parts": [{k: v for k, v in {
                "name": p["name"], "parent": p["parent"], "role": p["role"], "pivot": p["pivot"], "rotation": p["rotation"],
                "cubes": [{"uv": c["uv"], "box": c["box"], **({"mirror": True} if c["mirror"] else {})} for c in p["cubes"]],
            }.items() if v is not None} for p in a["parts"]]})
        write(f"assets/{NS}/models/item/{name}_spawn_egg.json", {"parent": "minecraft:item/template_spawn_egg"})
        write(f"data/{NS}/tags/item/{name}_food.json", {"replace": False, "values": FOOD[name]})
        if name in LOOT:
            loot(name)
        img = preview(a, tex)
        ImageDraw.Draw(img).text((8, 8), a["en"], fill=(40, 40, 40, 255))
        previews.append(img)
    spawns()
    lang(animals)
    cols = 5
    rows = (len(previews) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 300, rows * 300), (196, 206, 200, 255))
    for i, img in enumerate(previews):
        sheet.paste(img, (i % cols * 300, i // cols * 300))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "animals.png"))
    print("ok")


if __name__ == "__main__":
    main()

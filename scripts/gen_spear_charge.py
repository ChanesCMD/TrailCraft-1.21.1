"""
Считает модели разгона копья от третьего лица: models/item/spear/charge_1..7.json (charge_0 — стойка, charge_8 — копьё
горизонтально плоской стороной вверх; их скрипт не трогает, а берёт за концы).

Запуск из корня проекта:  python scripts/gen_spear_charge.py

На шаге k (доля разгона p = k/8) рука поднята на угол из SpearArmPose (от -PI/10 до -PI/4), а копьё:
- древко всё время в вертикальной плоскости вдоль взгляда (не уходит вбок), подъём меняется линейно от стойки
  до горизонтали;
- плоская сторона так же линейно поворачивается вокруг древка: из «ребром к камере» в «плоской стороной вверх»;
- хват (треть древка от торца) остаётся в кисти: его место линейно переходит от стойки к концу разгона.
Раньше промежуточные шаги были slerp'ом поворота между концами — по такому пути древко уходило вбок до 18°.
"""
import json
import math
import os

MODELS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources",
                      "assets", "trailcraft", "models", "item", "spear")
STEPS = 8
SCALE = (1.7, 1.7, 0.85)           # display.scale моделей копья
AXIS = (1 / math.sqrt(2), 1 / math.sqrt(2), 0.0)  # древко на текстуре: от торца (внизу слева) к острию (вверху справа)
NORMAL = (0.0, 0.0, 1.0)           # плоская сторона
GRIP = (-0.14, -0.14, 0.0)         # хват: треть древка от торца (координаты предмета от его центра)


# --- матрицы 3×3 (списки строк) ---

def mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]


def apply(m, v):
    return tuple(sum(m[i][k] * v[k] for k in range(3)) for i in range(3))


def transpose(m):
    return [list(r) for r in zip(*m)]


def rx(a):
    c, s = math.cos(a), math.sin(a)
    return [[1, 0, 0], [0, c, -s], [0, s, c]]


def ry(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, 0, s], [0, 1, 0], [-s, 0, c]]


def rz(a):
    c, s = math.cos(a), math.sin(a)
    return [[c, -s, 0], [s, c, 0], [0, 0, 1]]


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def columns(*cols):
    return [[c[i] for c in cols] for i in range(3)]


def display_rotation(deg):
    """ItemTransform: rotationXYZ = Rx * Ry * Rz."""
    x, y, z = (math.radians(d) for d in deg)
    return mul(mul(rx(x), ry(y)), rz(z))


def euler_xyz(m):
    y = math.asin(max(-1.0, min(1.0, m[0][2])))
    return [math.degrees(math.atan2(-m[1][2], m[2][2])), math.degrees(y), math.degrees(math.atan2(-m[0][1], m[0][0]))]


# Кисть от третьего лица (ItemInHandLayer): рука поворачивается на xRot, потом XP(-90) и YP(180)
HAND = mul(rx(math.radians(-90)), ry(math.radians(180)))


def arm(p):
    """xRot руки при разгоне (SpearArmPose): от -PI/10 до -PI/4."""
    return -math.pi / 10 + (-math.pi / 4 + math.pi / 10) * p


def to_player(v):
    """Модель игрока: y вниз, вперёд = -z; переводим в (вбок, вверх, вперёд)."""
    return (v[0], -v[1], -v[2])


def pose(p, rotation):
    """Подъём древка и поворот плоской стороны (градусы) для поворота display на доле разгона p."""
    m = mul(mul(rx(arm(p)), HAND), display_rotation(rotation))
    d, n = to_player(apply(m, AXIS)), to_player(apply(m, NORMAL))
    pitch = math.atan2(d[1], d[2])
    up = (0.0, math.cos(pitch), -math.sin(pitch))
    roll = math.atan2(sum(n[i] * up[i] for i in range(3)), n[0])
    return math.degrees(pitch), math.degrees(roll)


def rotation_for(p, pitch, roll):
    """Поворот display, при котором древко поднято на pitch в плоскости взгляда, а плоская сторона повёрнута на roll."""
    pitch, roll = math.radians(pitch), math.radians(roll)
    d = (0.0, math.sin(pitch), math.cos(pitch))
    up = (0.0, math.cos(pitch), -math.sin(pitch))
    n = tuple(math.cos(roll) * s + math.sin(roll) * u for s, u in zip((1.0, 0.0, 0.0), up))
    want = columns(*(to_player(v) for v in (d, n, cross(d, n))))
    item = columns(AXIS, NORMAL, cross(AXIS, NORMAL))
    world = mul(want, transpose(item))
    return mul(transpose(mul(rx(arm(p)), HAND)), world)


def grip_point(rotation, translation):
    """Где хват в системе кисти (единицы — блоки)."""
    r = rotation if isinstance(rotation[0], list) else display_rotation(rotation)
    g = apply(r, tuple(GRIP[i] * SCALE[i] for i in range(3)))
    return tuple(translation[i] / 16 + g[i] for i in range(3))


def load(k):
    with open(os.path.join(MODELS, f"charge_{k}.json"), encoding="utf-8") as f:
        return json.load(f)


def fmt(values):
    return "[" + ", ".join(f"{round(v, 2) + 0.0:g}" for v in values) + "]"


def write(k, rotation, translation, first):
    """Формат как у остальных моделей: по строке на display; от первого лица — как в исходном файле."""
    left_rot = [rotation[0], -rotation[1], -rotation[2]]
    third = {"thirdperson_righthand": (rotation, translation), "thirdperson_lefthand": (left_rot, translation)}
    lines = []
    for name, (rot, tr) in third.items():
        lines.append(f'    "{name}": {{"rotation": {fmt(rot)}, "translation": {fmt(tr)}, "scale": {fmt(SCALE)}}}')
    for name in ("firstperson_righthand", "firstperson_lefthand"):
        t = first[name]
        lines.append(f'    "{name}": {{"rotation": {fmt(t["rotation"])}, "translation": {fmt(t["translation"])}, '
                     f'"scale": {fmt(t["scale"])}}}')
    text = '{\n  "parent": "minecraft:item/handheld",\n  "display": {\n' + ",\n".join(lines) + "\n  }\n}\n"
    with open(os.path.join(MODELS, f"charge_{k}.json"), "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


if __name__ == "__main__":
    start = load(0)["display"]["thirdperson_righthand"]
    end = load(STEPS)["display"]["thirdperson_righthand"]
    pitch0, roll0 = pose(0, start["rotation"])
    pitch1, roll1 = pose(1, end["rotation"])
    hand0 = grip_point(start["rotation"], start["translation"])
    hand1 = grip_point(end["rotation"], end["translation"])
    for k in range(1, STEPS):
        p = k / STEPS
        r = rotation_for(p, pitch0 + (pitch1 - pitch0) * p, roll0 + (roll1 - roll0) * p)
        hand = tuple(a + (b - a) * p for a, b in zip(hand0, hand1))
        g = apply(r, tuple(GRIP[i] * SCALE[i] for i in range(3)))
        translation = [(hand[i] - g[i]) * 16 for i in range(3)]
        write(k, euler_xyz(r), translation, load(k)["display"])
        pitch, roll = pose(p, euler_xyz(r))
        print(f"charge_{k}: подъём {pitch:5.1f}°, поворот {roll:5.1f}°")
    print("ok")

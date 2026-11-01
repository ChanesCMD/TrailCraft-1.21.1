"""Текстуры листвы всех пород (textures/block/<порода>_leaves.png) и превью docs/textures/leaves.png.

Как у ванильной листвы: отдельные листья-пятна из 2–12 пикселей и 3–4 тонов (свет сверху слева), между ними щели —
тёмные или прозрачные. Листья раскладываются «упаковкой»: новый лист ставится, только если не налезает на соседние
(со стилем deep — налезает и бросает тень на нижние). Сначала кладётся задний слой мелких тёмных листьев (back), чтобы
листва была плотной: в игре грани между соседними блоками листвы не видны, и в щелях — небо. У каждой породы своя
форма (SPECIES, формы — SHAPES): овальные, лопастные, лапчатые, сердцевидные, ланцетные, перистые и круглые листья,
пучки хвои (у пихты — с серебристой изнанкой), веер игл сосны, веерные веточки туи, вайи пальмы, свисающие листья ивы,
серповидные — эвкалипта, цветы сакуры, соцветия сирени, серёжки вербы. Стиль у каждой породы свой — выбран автором из
вариантов (CHOSEN, STYLES); у фруктовых деревьев (ModWood.FRUIT_TREES) листва та же, что у амадзу.
Всё рисуется на торе 16×16 — текстура бесшовная. Палитры — REF (5 тонов от тёмного к светлому).

Запуск из корня проекта:  python scripts/gen_leaves.py   (нужен Pillow), после него — gen_season_textures.py
(осенняя и зимняя листва) и gen_leaf_particles.py (лепестки сакуры и листья ивы в цветах листвы).
"""
import colorsys
import math
import os
import random

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trailcraft", "textures", "block")
DOCS = os.path.join(HERE, "..", "docs", "textures")
N = 16


def tones(h, s0, s1, v0, v1):
    """5 тонов от тёмного к светлому: оттенок h, насыщенность и яркость — от (s0, v0) до (s1, v1)."""
    out = []
    for k in range(5):
        t = k / 4
        r, g, b = colorsys.hsv_to_rgb(h, s0 + (s1 - s0) * t, v0 + (v1 - v0) * t)
        out.append("%02x%02x%02x" % (round(r * 255), round(g * 255), round(b * 255)))
    return " ".join(out)


# палитры пород: 5 тонов от тёмного к светлому; амадзу — нефритово-зелёная (цуйхуа — «нефритовый цвет»)
REF = {
    "acacia": "586d1e 6e8926 7a982a 89aa2f 98be34",
    "alder": "34582d 416e38 487a3e 518945 5a984e",
    "amadzu": tones(0.42, 0.55, 0.48, 0.30, 0.58),
    "aspen": "6d832d 89a438 98b63e aacc45 bee44e",
    "birch": "51752d 659238 70a23e 7db545 8cca4e",
    "cedar": "24422d 2d5338 325c3e 386745 3e734e",
    "chestnut": "30511d 3b6524 427028 4a7d2d 528c32",
    "ebony": "2a4224 34532d 3a5c32 416738 48733e",
    "eucalyptus": "3f6558 4f7e6e 588c7a 639d89 6eaf98",
    "fir": "254835 2f5a43 34644a 3a7053 417d5c",
    "ironwood": "2b4934 365c41 3c6648 437251 4b805a",
    "lilac": "4f653a 637e48 6e8c50 7b9d5a 8aaf64",
    "linden": "426d2d 538938 5c983e 67aa45 73be4e",
    "maple": "2c5419 3e7226 477e2c 518c33 70ae48",
    "oak": "375b23 45722b 4d7f30 568e36 609f3c",
    "palm": "2b602b 367936 3c863c 439643 4ba84b",
    "pine": "20442a 285534 2c5e3a 316941 377648",
    "pussy_willow": "667549 80925c 8ea266 9fb572 b2ca80",
    "red_maple": "4e0b0e 771317 8a171a 9e1d1e cd322c",
    "sakura": "ab798f d697b2 eea8c6 ffbcde ffd2f8",
    "spruce": "1d3c2b 244c36 28543c 2d5e43 32694b",
    "thuja": "1a3e27 204d31 245636 28603c 2d6c44",
    "walnut": "345124 41652d 487032 517d38 5a8c3e",
    "white_poplar": "516d42 658953 70985c 7daa67 8cbe73",
    "willow": "5f7534 779241 84a248 94b551 a5ca5a",
}
# вторые палитры: цветы, серебристая изнанка листа, серёжки
EXTRA = {
    "lilac_flower": "6c4590 8a5cab a875c6 c492dd e3b4f5",
    "sakura_center": "8a3e5e a24d70 b85d82 c96d93 d87fa4",
    "sakura_leaf": "3c5a2a 4f7236 5f8640 6f9a4a 82ad55",
    "poplar_silver": "8a9a86 a7b5a2 c0ccbb d6dfd2 eef3ea",
    "aspen_pale": "8c9a45 a8b754 bccc62 cfdf73 e2f08a",
    "catkin": "8d8c86 aeada6 c9c8c0 dfded6 f4f3ec",
    "fir_silver": "4f7a68 6a9584 86ae9e a3c6b8 c2ddd2",
}

YELLOW, BLUE = 0.15, 0.62
# наименьший разброс яркости между тёмным и светлым тоном палитры
MIN_SPREAD = 0.24
# роли пикселя → тон палитры (0 — глубина, 1 — тень, 2 — середина, 3 — свет, 4 — блик)
ROLE = {"B": 0, "D": 1, "M": 2, "L": 3, "H": 4}


def hexrgb(s):
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def shift_hue(h, target, amount):
    d = ((target - h + 0.5) % 1.0) - 0.5
    return (h + math.copysign(min(abs(d), abs(amount)), d)) % 1.0


def make_ramp(ref, contrast=1.0, hue_shift=0.06, lift=0.0):
    """5 тонов (0 — глубина … 4 — блик) и цвет щели — по тёмному и светлому тону палитры; в тенях оттенок уходит
    к синему, в светах — к жёлтому."""
    ends = [hexrgb(t) for t in ref.split()]
    hd, sd, vd = colorsys.rgb_to_hsv(*[c / 255 for c in ends[0]])
    hl, sl, vl = colorsys.rgb_to_hsv(*[c / 255 for c in ends[-1]])
    if vl - vd < MIN_SPREAD:   # тёмные палитры (хвоя) раздвигаем, иначе формы не читаются
        mid = (vl + vd) / 2
        vd, vl = mid - MIN_SPREAD * 0.45, mid + MIN_SPREAD * 0.55
    dh = ((hl - hd + 0.5) % 1.0) - 0.5
    out = []
    for t in (-0.3, 0.12, 0.5, 0.9, 1.25):
        t = 0.5 + (t - 0.5) * contrast + lift
        k = min(1, max(0, t))
        h = (hd + dh * k) % 1.0
        s = sd + (sl - sd) * t
        v = vd + (vl - vd) * t
        if t > 1:   # блик: светлее и чуть бледнее
            v = vl + (t - 1) * (vl - vd) * 1.5
            s = sl * (1 - 0.4 * (t - 1))
        h = shift_hue(h, YELLOW, hue_shift * (t - 0.5)) if t > 0.5 else shift_hue(h, BLUE, hue_shift * (0.5 - t))
        s, v = min(1, max(0, s)), min(1, max(0.03, v))
        out.append(tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, s, v)))
    h, s, v = colorsys.rgb_to_hsv(*[c / 255 for c in out[0]])
    gap = tuple(round(c * 255) for c in colorsys.hsv_to_rgb(h, min(1, s * 1.05), v * 0.7))
    return out, gap


# ------------------------------------------------------------------ формы листьев
def parse(rows):
    """Форма из строк: X — пиксель листа (светотень считается сама), V — жилка, C — середина цветка,
    S — серебристая изнанка, L M D H — заданный тон."""
    pix = {}
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                pix[(x, y)] = ch
    return pix


def dihedral(pix, k):
    """Поворот на k × 90° (k ≥ 4 — ещё и отражение)."""
    out = {}
    for (x, y), ch in pix.items():
        for _ in range(k % 4):
            x, y = -y, x
        if k >= 4:
            x = -x
        out[(x, y)] = ch
    mx, my = min(x for x, _ in out), min(y for _, y in out)
    return {(x - mx, y - my): ch for (x, y), ch in out.items()}


def auto_shade(pix):
    """Свет сверху слева: верхний и левый край — светлее, нижний и правый — темнее, внутри — середина."""
    roles = {}
    big = len(pix) >= 7
    for (x, y), ch in pix.items():
        if ch in "LMDHB":
            roles[(x, y)] = ch
            continue
        out = lambda dx, dy: (x + dx, y + dy) not in pix
        s = (out(0, -1) + out(-1, 0)) - (out(0, 1) + out(1, 0)) + 0.5 * (out(-1, -1) - out(1, 1))
        r = "H" if (s >= 1.75 and big) else "L" if s >= 0.75 else "M" if s > -0.75 else "D"
        if ch == "V":
            r = "D" if r != "D" else "B"
        elif ch == "C":
            r = "C"
        roles[(x, y)] = r
    return roles


SHAPES = {
    "oval": {
        "small": [("XX", "XX"), ("X.", "XX"), ("XX", "X."), (".X", "XX", "X.")],
        "medium": [(".XX", "XXX", "XX."), (".X.", "XXX", "XX."), ("XXX", "XX."), (".XX", "XXX", ".X.")],
        "large": [("..XX", ".XXX", "XXX.", "XX.."), (".XX.", "XXXX", "XXXX", ".XX."), (".XXX", "XXVX", "XXX.", ".X..")],
    },
    "oak": {
        "small": [("X.", "XX"), ("XX", "XX"), (".X", "XX", ".X")],
        "medium": [("X.X", "XXX", ".X."), (".X.", "XXX", "XX.", ".X.")],
        "large": [(".X.X", "XXXX", ".XX.", "XX.."), ("X.X.", "XXXX", ".XVX", ".X.X"), (".X.", "XXX", ".VX", "XXX", ".X.")],
    },
    "palmate": {
        "small": [("X.X", ".X."), (".X.", "XXX"), ("X.X", "XX.")],
        "medium": [("X.X", "XXX", ".X."), (".X.", "XXX", "X.X"), ("X.X.X", ".XXX.", "..X..")],
        "large": [("..X..", "X.X.X", "XXVXX", ".XXX.", "..X.."), ("X...X", ".XXX.", "XXVXX", "..X..")],
    },
    "heart": {
        "small": [("X.X", ".X."), ("XX", "X.")],
        "medium": [("X.X", "XXX", ".X."), ("XX.", "XXX", ".X.")],
        "large": [("XX.XX", "XXXXX", ".XVX.", "..X.."), ("X.X.", "XXXX", "XVXX", ".XX.", "..X.")],
    },
    "lance": {
        "small": [(".X", "XX"), ("..X", ".XX", "X..")],
        "medium": [("..X", ".XX", "XX."), ("...X", "..X.", ".XX.", "X...")],
        "large": [("....X", "...XX", "..XV.", ".XX..", "X...."), ("...X", "..XX", ".XV.", "XX..", "X...")],
    },
    "feather": {   # орех: перистый лист — черешок и листочки вперёд по обе стороны
        "small": [("..L", ".LM", "LMD"), ("L.L", ".M.", "..D")],
        "medium": [("L.L..", ".LML.", "..MMD", "...D."), ("..L.", "L.ML", ".MMD", "..D.")],
        "large": [("L.L...", ".LML..", "L.MML.", ".LMMD.", "...MD.", "....D."),
                  ("..L.L", "L.LM.", ".LMM.", "..MD.", "...D.")],
    },
    "round": {
        "small": [("XX", "XX"), (".X.", "XXX", ".X.")],
        "medium": [("XX.", "XXX", ".XX"), (".XX", "XXX", "XX."), (".X.", "XXX", "XXX")],
        "large": [(".XX.", "XXXX", "XXXX", ".XX."), (".XX.", "XXXX", "XVXX", ".XX.")],
    },
    "tuft": {   # ель, кедр: пучки игл с тёмной серединой
        "small": [("L.", "MD"), (".L.", "LMD", ".D.")],
        "medium": [(".L.", "LDM", ".M."), ("L.L", ".D.", "M.D"), ("DLD", "LDM", "DMD")],
        "large": [("..L..", ".LML.", "LMDMD", ".MDD.", "..D.."), ("L.L.L", ".LMM.", "LMDMD", ".MDD.", "M.D.D")],
    },
    "fan": {   # сосна: длинные иглы веером из одной точки
        "small": [("L.L", ".M.", ".D."), ("L..", ".LM", ".D.")],
        "medium": [("L.L.L", ".LML.", "..D.."), ("L..L", ".L.M", "..MD", "...D"), ("L...L", ".L.M.", "..M..", "..D..")],
        "large": [("L..L..L", ".L.L.M.", "..LMM..", "...D...", "...D..."), ("L...L", ".L.M.", "L.LM.", ".L.M.", "..DD.")],
    },
    "firtuft": {   # пихта: пучки, как у ели, снизу — серебристая изнанка игл (S)
        "small": [(".L.", "LMM", ".S."), ("L.L", ".M.", "S.S")],
        "medium": [(".L.", "LDM", ".S."), ("L.L", ".D.", "S.S"), ("DLD", "LDM", "SMS")],
        "large": [("..L..", ".LML.", "LMDMD", ".MDS.", "..S.."), ("L.L.L", ".LMM.", "LMDMD", ".SDS.", "S.D.S")],
    },
    "spray": {  # туя: плоские веерные веточки из чешуек
        "small": [("L.", "LM", ".D"), (".L.", "LMM", ".MD")],
        "medium": [(".L..", "LML.", ".MMD", "..D."), ("L.L.", ".LML", "LMMD", ".D.D")],
        "large": [(".L...", "LMLL.", ".MLML", "..MMD", ".M.D."), ("L..L.", ".LML.", "LMMLM", ".MDMD", "..D..")],
    },
    "flower": {  # сакура: лепестки и середина C
        "small": [(".X.", "XCX", ".X."), ("XX", "XC")],
        "medium": [(".XX.", "XXCX", "XCXX", ".XX."), ("X.X", ".C.", "X.X"), (".X.", "XCX", "X.X")],
        "large": [(".X.X.", "XXXXX", ".XCX.", "XXXXX", ".X.X."), ("..X..", ".XXX.", "XXCXX", ".XXX.", ".X.X.")],
    },
    "panicle": {  # сирень: конус мелких цветков
        "small": [(".X.", "XXX", ".X."), ("XX", "XX", "X.")],
        "medium": [(".X.", "XXX", "XXX", ".X."), (".XX", "XXX", "XXX", ".X.")],
        "large": [("..X..", ".XXX.", "XXXXX", ".XXX.", "..X.."), (".X.", "XXX", "XXX", "XXX", ".X.")],
    },
    "catkin": {   # верба: пушистые серёжки
        "small": [("X", "X"), ("XX",)],
        "medium": [(".X.", "XXX", ".X."), ("XX", "XX")],
        "large": [(".X.", "XXX", "XXX", ".X.")],
    },
    "hang": {   # ива: узкие листья свисают вниз
        "small": [("L", "M"), ("L", "M", "D")],
        "medium": [("L", "M", "M", "D"), ("L.", "M.", ".M", ".D"), (".L", ".M", "M.", "D.")],
        "large": [("L", "M", "M", "M", "D"), ("L.", "M.", "M.", ".M", ".D"), (".L", "M.", "M.", "M.", "D.")],
    },
    "sickle": {   # эвкалипт: серповидные листья по два пикселя
        "small": [("LM", "MD"), ("L.", "MM", ".D")],
        "medium": [("L.", "LM", "MM", ".D"), (".L", "LM", "MD", "D.")],
        "large": [("L.", "LM", "MM", "MD", ".D"), (".L", "LM", "MM", "MD", "D."), ("L.", "LM", ".M", ".M", ".D")],
    },
}
_CACHE = {}


def variants(kind, scale, fixed=False):
    """Все повороты форм; у хвои и свисающих листьев (fixed) светотень своя — только отражение по горизонтали."""
    key = (kind, scale, fixed)
    if key not in _CACHE:
        out = []
        for rows in SHAPES[kind][scale]:
            base = parse(rows)
            ks = range(8) if not fixed or kind == "feather" else (0, 4)
            for k in ks:
                v = dihedral(base, k)
                out.append(v if fixed or kind == "feather" else auto_shade(v))
        _CACHE[key] = out
    return _CACHE[key]


# ------------------------------------------------------------------ холст 16×16 на торе
class Tex:
    def __init__(self):
        self.pal = [[-1] * N for _ in range(N)]
        self.tone = [[0] * N for _ in range(N)]
        self.owner = [[0] * N for _ in range(N)]   # 0 — пусто или задний слой, >0 — передний лист
        self.next = 1

    def put(self, x, y, pal, tone, owner=0):
        x, y = x % N, y % N
        self.pal[y][x] = pal
        self.tone[y][x] = max(0, min(4, tone))
        self.owner[y][x] = owner

    def get_owner(self, x, y):
        return self.owner[y % N][x % N]


def fits(tex, motif, ox, oy, contact):
    """Помещается ли лист: не налезает на передние листья и касается их не больше чем contact сторонами."""
    touch = 0
    for (x, y) in motif:
        if tex.get_owner(ox + x, oy + y):
            return False
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            if (x + dx, y + dy) not in motif and tex.get_owner(ox + x + dx, oy + y + dy):
                touch += 1
                if touch > contact:
                    return False
    return True


def draw_motif(tex, motif, ox, oy, pals, offset, owner, cast=False, gloss=False, rng=None):
    """pals: {буква: (палитра, тон)} для середины цветка C и изнанки S; основная палитра — pals[None]."""
    main = pals[None]
    for (x, y), r in motif.items():
        if r in ("C", "S"):
            pal, tone = pals[r]
        else:
            pal, tone = main, ROLE[r] + offset
        tex.put(ox + x, oy + y, pal, tone, owner)
    if gloss and rng and rng.random() < 0.75:   # глянец: блик в верхнем левом углу листа
        corner = min(motif, key=lambda p: p[0] + p[1])
        tex.put(ox + corner[0], oy + corner[1], main, 4, owner)
    if cast:   # тень на то, что ниже справа
        for (x, y) in motif:
            q = (ox + x + 1, oy + y + 1)
            if (x + 1, y + 1) not in motif and tex.pal[q[1] % N][q[0] % N] >= 0 and tex.get_owner(*q) != owner:
                tex.tone[q[1] % N][q[0] % N] = max(0, tex.tone[q[1] % N][q[0] % N] - 1)


def pick_scale(rng, scale):
    w = {"small": (0.7, 0.3, 0.0), "medium": (0.25, 0.55, 0.2), "large": (0.05, 0.35, 0.6),
         "mix": (0.34, 0.33, 0.33)}[scale]
    return rng.choices(("small", "medium", "large"), w)[0]


def pack(tex, rng, kind, scale, pals_fn, offsets, contact, overlap=False, tries=2500, fixed=False, cast=False,
         gloss=False, limit=None):
    """Раскладывает листья, пока tries раз подряд не найдётся места (или пока не положено limit листьев)."""
    placed = fails = 0
    while fails < tries and (limit is None or placed < limit):
        m = rng.choice(variants(kind, pick_scale(rng, scale), fixed))
        ox, oy = rng.randrange(N), rng.randrange(N)
        if not overlap and not fits(tex, m, ox, oy, contact):
            fails += 1
            continue
        draw_motif(tex, m, ox, oy, pals_fn(), rng.choice(offsets), tex.next, cast, gloss, rng)
        tex.next += 1
        placed += 1
        if overlap and placed > 40:
            break
    return placed


def line(x0, y0, x1, y1):
    pts = []
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = (1 if x0 < x1 else -1), (1 if y0 < y1 else -1)
    err = dx + dy
    while True:
        pts.append((x0, y0))
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy
    return pts


def frond_motif(rng, length, leaflet):
    """Вайя пальмы: ось (светлая) и листочки назад по обе стороны."""
    a = rng.uniform(0, 2 * math.pi)
    d = (math.cos(a), math.sin(a))
    m = {}
    fx = fy = 0.0
    for k in range(length):
        px, py = round(fx), round(fy)
        if k % 2 == 0 and k > 0:
            for sgn in (1, -1):
                b = a + sgn * 2.4
                ex, ey = px + round(math.cos(b) * leaflet), py + round(math.sin(b) * leaflet)
                for j, q in enumerate(line(px, py, ex, ey)[1:], 1):
                    lit = -(ex - px + ey - py)
                    m.setdefault(q, "L" if lit > 0 and j == 1 else "M" if lit >= 0 or j == 1 else "D")
        m[(px, py)] = "L"
        fx += d[0]
        fy += d[1]
    mx, my = min(x for x, _ in m), min(y for _, y in m)
    return {(x - mx, y - my): r for (x, y), r in m.items()}


# ------------------------------------------------------------------ породы и выбранные стили
# kind — форма листьев; small/big — мельче/крупнее стиля; fixed — светотень формы своя; alt — доля листьев во второй
# палитре (серебристая изнанка, бледные); gloss — глянцевые блики; lift — светлее; special — своя раскладка
SPECIES = {
    "oak": dict(kind="oak"),
    "birch": dict(kind="oval", small=True),
    "aspen": dict(kind="round", small=True, alt="aspen_pale", alt_share=0.3),
    "white_poplar": dict(kind="palmate", alt="poplar_silver", alt_share=0.35),
    "alder": dict(kind="round"),
    "linden": dict(kind="heart"),
    "maple": dict(kind="palmate"),
    "red_maple": dict(kind="palmate", lift=0.12),
    "chestnut": dict(kind="lance"),
    "walnut": dict(kind="feather"),
    "acacia": dict(kind="oval", small=True),
    "eucalyptus": dict(kind="sickle", fixed=True),
    "ebony": dict(kind="oval", gloss=True),
    "palm": dict(special="fronds"),
    "ironwood": dict(kind="oval"),
    "willow": dict(kind="hang", fixed=True),
    "pussy_willow": dict(kind="lance", catkins=True),
    "sakura": dict(special="blossom", lift=0.15),
    "lilac": dict(special="lilac"),
    "amadzu": dict(kind="oval"),
    "pine": dict(kind="fan", fixed=True),
    "spruce": dict(kind="tuft", fixed=True),
    "fir": dict(kind="firtuft", fixed=True),
    "cedar": dict(kind="tuft", fixed=True, big=True),
    "thuja": dict(kind="spray", fixed=True),
}
# стиль: (зерно — номер варианта, из которого выбирал автор; параметры). scale — размер листьев, contact — сколько
# сторон лист может касаться соседей, dark_gaps — доля щелей, закрашенных тёмным (остальные прозрачные), deep —
# задний слой темнее и листья бросают тень, sun — доля светлых пикселей-бликов
STYLES = {
    "classic": (0, dict(scale="medium", contact=1, contrast=1.0, dark_gaps=0.4)),
    "large": (1, dict(scale="large", contact=1, contrast=1.0, dark_gaps=0.4)),
    "small": (2, dict(scale="small", contact=1, contrast=1.0, dark_gaps=0.4)),
    "dense": (3, dict(scale="medium", contact=2, contrast=0.95, dark_gaps=0.5)),
    "light": (4, dict(scale="medium", contact=1, contrast=1.0, dark_gaps=0.4, lift=0.1, sun=0.12)),
    "contrast": (5, dict(scale="medium", contact=1, contrast=1.45, dark_gaps=0.35, deep=True)),
    "soft": (6, dict(scale="medium", contact=3, contrast=0.65, dark_gaps=0.6)),
    "deep": (8, dict(scale="mix", contact=1, contrast=1.15, dark_gaps=0.2, deep=True)),
}
CHOSEN = {
    "pine": "dense", "spruce": "classic", "fir": "soft", "cedar": "classic", "thuja": "small",
    "oak": "small", "birch": "large", "aspen": "light", "white_poplar": "light", "alder": "deep",
    "linden": "deep", "maple": "dense", "red_maple": "large", "chestnut": "dense", "walnut": "classic",
    "acacia": "light", "eucalyptus": "large", "ebony": "large", "palm": "large", "ironwood": "contrast",
    "willow": "large", "pussy_willow": "small", "sakura": "soft", "lilac": "small", "amadzu": "classic",
}
# фруктовые деревья (на древесине амадзу) — с листвой амадзу
FRUIT_TREES = ("apple", "pear", "plum", "cherry", "apricot")


def generate(species):
    seed, st = STYLES[CHOSEN[species]]
    sp = SPECIES[species]
    rng = random.Random(f"{species}:{seed}:0")
    scale = st["scale"]
    if sp.get("small"):
        scale = {"large": "medium", "medium": "small", "mix": "mix", "small": "small"}[scale]
    if sp.get("big"):
        scale = {"small": "medium", "medium": "large", "mix": "mix", "large": "large"}[scale]
    ramps = [make_ramp(REF[species], st["contrast"], lift=sp.get("lift", 0.0) + st.get("lift", 0.0))]

    def add(ramp):
        ramps.append(ramp)
        return len(ramps) - 1

    alt_pals = []
    if sp.get("alt"):
        alt_pals.append((add(make_ramp(EXTRA[sp["alt"]], st["contrast"])), sp["alt_share"]))
    silver = add(make_ramp(EXTRA["fir_silver"], st["contrast"]))
    extra_roles = {"S": (silver, 1)}   # серебристая изнанка пихты — приглушённая, иначе рябит

    def pals():
        p = 0
        r = rng.random()
        for pal, share in alt_pals:
            if r < share:
                p = pal
                break
            r -= share
        return {None: p, **extra_roles}

    tex = Tex()
    offsets = (0, 0, 0, 1, -1) if st["contrast"] >= 0.9 else (0,)
    special = sp.get("special")
    fixed = sp.get("fixed", False)
    kind = sp.get("kind")
    # задний слой: мелкие тёмные листья в глубине кроны
    bk = kind or ("oval" if special != "fronds" else "lance")
    if special == "blossom":
        bk = "oval"
    pack(tex, rng, bk if special != "lilac" else "heart", "small", lambda: {None: 0, **extra_roles},
         (-1,) if not st.get("deep") else (-2, -1), contact=4, fixed=fixed and bk == kind, tries=300)
    for y in range(N):
        for x in range(N):
            tex.owner[y][x] = 0   # задний слой не мешает передним листьям
    if special is None:
        pack(tex, rng, kind, scale, pals, offsets, st["contact"], fixed=fixed, gloss=sp.get("gloss"),
             cast=st.get("deep", False))
        if sp.get("catkins"):
            c = add(make_ramp(EXTRA["catkin"], 1.0))
            pack(tex, rng, "catkin", scale, lambda: {None: c, **extra_roles}, (0, 1), contact=4, overlap=True,
                 limit=rng.randint(3, 5))
    elif special == "fronds":
        L = {"small": 7, "medium": 10, "large": 14, "mix": 11}[scale]
        ll = {"small": 2, "medium": 2, "large": 3, "mix": 3}[scale]
        fails = 0
        while fails < 400:
            m = frond_motif(rng, rng.randint(L - 2, L + 2), ll)
            ox, oy = rng.randrange(N), rng.randrange(N)
            if fits(tex, m, ox, oy, st["contact"] + 2):
                draw_motif(tex, m, ox, oy, pals(), rng.choice(offsets), tex.next, cast=st.get("deep", False))
                tex.next += 1
            else:
                fails += 1
    elif special == "blossom":
        leaf = add(make_ramp(EXTRA["sakura_leaf"], st["contrast"]))
        center = add(make_ramp(EXTRA["sakura_center"], st["contrast"]))
        pack(tex, rng, "oval", "small", lambda: {None: leaf, **extra_roles}, (0, -1), st["contact"], limit=3)
        pack(tex, rng, "flower", scale, lambda: {None: pals()[None], "C": (center, 1), **extra_roles}, offsets,
             st["contact"], cast=st.get("deep", False))
    elif special == "lilac":
        fl = add(make_ramp(EXTRA["lilac_flower"], st["contrast"]))
        pack(tex, rng, "panicle", scale, lambda: {None: fl, **extra_roles}, (0,), st["contact"],
             limit=rng.randint(2, 3))
        for y in range(N):   # мелкие цветочки: светлые и тёмные через один
            for x in range(N):
                if tex.pal[y][x] == fl and (x + y) % 2 == 0:
                    tex.tone[y][x] = max(0, tex.tone[y][x] - 1)
        pack(tex, rng, "heart", scale, pals, offsets, st["contact"])
    if st.get("sun"):
        for y in range(N):
            for x in range(N):
                if tex.pal[y][x] == 0 and tex.tone[y][x] == 3 and rng.random() < st["sun"]:
                    tex.tone[y][x] = 4
    return render(tex, ramps, st["dark_gaps"], rng)


def render(tex, ramps, dark_gaps, rng):
    img = Image.new("RGBA", (N, N))
    gap = ramps[0][1]
    empties = [(x, y) for y in range(N) for x in range(N) if tex.pal[y][x] < 0]
    # тёмными делаем самые узкие щели (больше всего занятых соседей) — это обводка листьев; прозрачный пиксель —
    # тоже цвета щели, иначе вдали (mipmap) листва светлеет по краям
    scored = sorted(empties, key=lambda p: -sum(tex.pal[(p[1] + dy) % N][(p[0] + dx) % N] >= 0
                                                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))) - rng.random())
    dark = set(scored[:round(dark_gaps * len(scored))])
    for y in range(N):
        for x in range(N):
            p = tex.pal[y][x]
            if p < 0:
                img.putpixel((x, y), gap + ((255,) if (x, y) in dark else (0,)))
            else:
                img.putpixel((x, y), ramps[p][0][tex.tone[y][x]] + (255,))
    return img


def preview(images):
    """Каждая листва плиткой 2×2 (видно, что бесшовная), по 6 в ряд."""
    scale, pad, cols = 4, 6, 6
    cell = 32 * scale
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new("RGBA", (pad + cols * (cell + pad), pad + rows * (cell + pad)), (60, 60, 60, 255))
    for i, img in enumerate(images):
        tile = Image.new("RGBA", (32, 32))
        for dx in (0, 16):
            for dy in (0, 16):
                tile.paste(img, (dx, dy))
        sheet.alpha_composite(tile.resize((cell, cell), Image.NEAREST),
                              (pad + (i % cols) * (cell + pad), pad + (i // cols) * (cell + pad)))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "leaves.png"))


if __name__ == "__main__":
    images = {sp: generate(sp) for sp in CHOSEN}
    for fruit in FRUIT_TREES:
        images[fruit] = images["amadzu"]
    for sp, img in images.items():
        img.save(os.path.join(TEX, f"{sp}_leaves.png"))
    preview(list(images.values()))
    print("ok")

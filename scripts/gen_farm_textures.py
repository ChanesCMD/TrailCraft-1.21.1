"""
Рисует текстуры для земледелия: тяпки, грядки, ягодные кусты и ягоды, дикие морковь и картошку, джут и верёвку.

Запуск из корня проекта:  python scripts/gen_farm_textures.py
Нужен Pillow (pip install pillow). Скрипт перезаписывает только свои текстуры (список — в конце файла).

Тяпки — рисунок автора (головка вправо, свой черенок) с обводкой головки, как у кирок и топоров (outline()), посевы джута — рисунок автора scripts/templates/jute_stages.png,
пучок джута — тонкая связка, как солома (straw_bundle), верёвка — рисунок автора (скрипт её не перерисовывает).
"""
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures")
DOCS = os.path.join(ROOT, "docs", "textures")


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def mix(a, b, t=0.5):
    return tuple(round(a[i] * (1 - t) + b[i] * t) for i in range(3)) + (255,)


def scale(c, k, cool=0.0):
    """Затемняет/осветляет цвет в k раз; cool > 0 немного сдвигает в холодный (влажная земля)."""
    r, g, b = (min(255, round(v * k)) for v in c[:3])
    r = round(r * (1 - cool))
    b = min(255, round(b * (1 + cool / 2)))
    return (r, g, b, c[3])


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def load(rel):
    return Image.open(os.path.join(TEX, *rel.split("/"))).convert("RGBA")


def save(img, rel):
    p = os.path.join(TEX, *rel.split("/")) + ".png"
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


def paint(img, rows, colors, dx=0, dy=0):
    """Рисует по строкам-маске: символ — ключ в colors, '.' — пропуск, 'x' — стереть."""
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            px, py = x + dx, y + dy
            if not (0 <= px < 16 and 0 <= py < 16):
                continue
            img.putpixel((px, py), (0, 0, 0, 0) if ch == "x" else colors[ch])
    return img


def sheet(rows, cell=16, scale_=6, pad=6, bg=(60, 60, 60, 255)):
    cols = max(len(r) for r in rows)
    size = cell * scale_
    out = Image.new("RGBA", (pad + cols * (size + pad), pad + len(rows) * (size + pad)), bg)
    for r, row in enumerate(rows):
        for c, im in enumerate(row):
            if im is None:
                continue
            big = im.resize((im.width * scale_, im.height * scale_), Image.NEAREST)
            out.paste(big, (pad + c * (size + pad), pad + r * (size + pad)), big)
    return out


# ============================== ТЯПКИ ==============================

HOE_MATERIALS = ["flint", "bone", "copper", "bronze", "iron", "steel"]

# h — блик, l — светлый, m — средний, n — между средним и тёмным, d — тёмный, s — тень, o — контур.
# Металлы — те же цвета, что у кирок этого металла.
HOE_RAMPS = {
    "flint": ["#8a8a96", "#6a6a75", "#52525b", None, "#36363d", "#26262b", "#141417"],
    "bone": ["#fffcee", "#e8e1c8", "#cbc4aa", None, "#a39c84", "#7d7663", "#4a4535"],
    "copper": ["#f6b88e", "#e08c5c", "#c06a3e", None, "#864024", "#6e341c", "#3a190e"],
    "bronze": ["#eec882", "#cc9a4e", "#a87632", None, "#744d1c", "#624115", "#2e1e06"],
    "iron": ["#f2f9ff", "#cdd3e0", "#b7bdc8", "#868b94", "#515459", "#404246", "#161718"],
    "steel": ["#e0e9f2", "#a6b2bf", "#7c8794", None, "#4a525d", "#3b414b", "#161a1e"],
}
# Тяпка — рисунок автора целиком, с черенком (1 — светлый, 2 — средний, 3 — тёмный, 4 — торец).
# Головка: h — блик, l — светлый, m — средний, d — тёмный, o — контур.
HOE = [
    "................",
    "................",
    "........hl......",
    ".......hlmm.....",
    ".......mdmmm....",
    ".........ddmd...",
    ".........12do...",
    "........123.....",
    ".......123......",
    "......123.......",
    ".....123........",
    "....123.........",
    "...123..........",
    "..123...........",
    ".134............",
    "................",
]
HOE_FULL_HANDLE = {"1": hx("#b8945a"), "2": hx("#8f6a3a"), "3": hx("#6b4c28"), "4": hx("#45301a")}
# Цвета головки металлов из рисунка автора: h, l, m, d, o
HOE_FULL_HEADS = {
    "copper": ["#f0aa82", "#d68258", "#b4623c", "#8a4226", "#602c1a"],
    "bronze": ["#f0c86e", "#d7a242", "#b27a26", "#825418", "#583610"],
    "iron": ["#f2f9ff", "#cdd3e0", "#b7bdc8", "#515459", "#323337"],
    "steel": ["#dce6f0", "#b0bcca", "#8c98a8", "#545c6a", "#2e323a"],
}


def hoe_tones(material):
    r = [hx(c) if c else None for c in HOE_RAMPS[material]]
    if r[3] is None:
        r[3] = mix(r[2], r[4])
    return dict(zip("hlmndso", r))


def hoe(material):
    tones = dict(HOE_FULL_HANDLE)
    if material in HOE_FULL_HEADS:
        tones.update(zip("hlmdo", map(hx, HOE_FULL_HEADS[material])))
    else:
        ramp = hoe_tones(material)
        tones.update({k: ramp[k] for k in "hlmdo"})
    img = paint(blank(), HOE, tones)
    if material == "flint":
        # сколы на кремне: светлые точки по краю полотна
        rnd = random.Random("D")
        tones = hoe_tones(material)
        for x, y in [(x, y) for y in range(16) for x in range(16)
                     if img.getpixel((x, y)) in (tones["m"], tones["n"])]:
            if rnd.random() < 0.3:
                img.putpixel((x, y), tones["l"])
    return img


# Цвета черенка (обработанная палка) — их обводка не трогает; у кости — своя пара цветов контура
HANDLE_COLORS = {(69, 48, 26), (107, 76, 40), (143, 106, 58), (184, 148, 90), (212, 176, 122)}
BONE_OUTLINE = ((74, 69, 53), (125, 118, 99))


def outline_colors(material):
    """Контур головки: (тёмный — снизу/справа, светлее — сверху/слева), по кирке того же материала."""
    if material == "bone":
        return BONE_OUTLINE
    img = load(f"item/{material}_pickaxe.png")
    tones = sorted({img.getpixel((x, y))[:3] for y in range(16) for x in range(16) if img.getpixel((x, y))[3]}
                   - HANDLE_COLORS, key=sum)
    if material == "flint":
        tones = [t for t in tones if abs(t[0] - t[2]) < 12]  # у кремнёвой кирки ещё и бечёвка
    return tones[0], tones[2]


def outline(img, material):
    """Обводка головки (всё, кроме черенка) в 1 пиксель снаружи; снизу и справа — тёмная, сверху и слева — светлее."""
    dark, light = (c + (255,) for c in outline_colors(material))
    px = img.load()

    def head(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and px[x, y][3] and px[x, y][:3] not in HANDLE_COLORS

    paint_ = {}
    for y in range(16):
        for x in range(16):
            if px[x, y][3]:
                continue
            if head(x - 1, y) or head(x, y - 1):
                paint_[x, y] = dark
            elif head(x + 1, y) or head(x, y + 1):
                paint_[x, y] = light
    for (x, y), c in paint_.items():
        px[x, y] = c
    return img


def hoes():
    for m in HOE_MATERIALS:
        save(outline(hoe(m), m), f"item/{m}_hoe")


# ============================== ГРЯДКИ ==============================

FARMLAND_SOILS = ["zheltozem", "burozem", "chernozem"]


def farmland(soil, moist):
    """Верх грядки: почва с бороздами (через 4 пикселя) и тёмной рамкой; влажная — темнее и чуть холоднее."""
    base = load(f"block/{soil}.png")
    img = blank()
    ridge = [1.12, 1.0, 0.86, 0.62]
    for y in range(16):
        for x in range(16):
            k = ridge[(y + 1) % 4]
            if x in (0, 15) or y in (0, 15):
                k *= 0.82
            if moist:
                k *= 0.62
            img.putpixel((x, y), scale(base.getpixel((x, y)), k, 0.06 if moist else 0.0))
    return img


def farmlands():
    for soil in FARMLAND_SOILS:
        save(farmland(soil, False), f"block/{soil}_farmland")
        save(farmland(soil, True), f"block/{soil}_farmland_moist")


# ============================== РАСТЕНИЯ ==============================

STEM = [hx("#463722"), hx("#5b482d"), hx("#6e5638")]
GREEN_STEM = [hx("#3f6326"), hx("#4f7a30"), hx("#64923c")]

# Листья: от тёмного к светлому
LEAVES = {
    "currant": ["#2f4f1e", "#3c6527", "#4b7c30", "#5d953b", "#74ad4a"],
    "gooseberry": ["#30532a", "#3e6b33", "#4e843c", "#639c49", "#7db45c"],
    "blackberry": ["#26401b", "#305226", "#3c652e", "#4b7a37", "#5f9045"],
    "cloudberry": ["#41611f", "#527a28", "#649231", "#79aa3e", "#93c052"],
    "carrot": ["#36591f", "#456f28", "#568733", "#6aa03f", "#82b54f"],
    "potato": ["#2f5320", "#3d6a2a", "#4d8135", "#619a40", "#78b04f"],
    "jute": ["#355a1e", "#436f26", "#53872f", "#66a03a", "#7fb64a"],
}

# Ягоды: тень, основной, блик
BERRY_COLORS = {
    "currant": ["#150f20", "#2c2040", "#5d4c82"],
    "gooseberry": ["#7a9636", "#b4cf62", "#e8f4ae"],
    "blackberry": ["#12081a", "#2c1636", "#6a4a8c"],
    "cloudberry": ["#b25e16", "#e8942c", "#ffd37a"],
}


def pal(name):
    return [hx(c) for c in LEAVES[name]]


def foliage(img, rows, leaves, seed, holes=0.07, origin=(0, 0)):
    """Заливает маску '#' листвой: сверху-слева светлее, к краям снизу-справа темнее, с шумом и просветами."""
    rnd = random.Random(seed)
    ox, oy = origin
    pts = {(x + ox, y + oy) for y, row in enumerate(rows) for x, ch in enumerate(row) if ch == "#"}
    if not pts:
        return []
    cx = sum(p[0] for p in pts) / len(pts)
    cy = sum(p[1] for p in pts) / len(pts)
    span = max(1.0, max(abs(p[0] - cx) + abs(p[1] - cy) for p in pts))
    placed = []
    for x, y in sorted(pts, key=lambda p: (p[1], p[0])):
        inner = all(n in pts for n in ((x - 1, y), (x + 1, y), (x, y - 1), (x, y + 1)))
        if inner and rnd.random() < holes:
            continue
        t = ((cx - x) * 0.5 + (cy - y) * 0.9) / span
        if (x, y + 1) not in pts or (x + 1, y) not in pts:
            t -= 0.35
        if (x, y - 1) not in pts:
            t += 0.3
        v = t + rnd.uniform(-0.75, 0.75)
        i = max(0, min(len(leaves) - 1, int((v + 1.2) / 2.4 * len(leaves))))
        img.putpixel((x, y), leaves[i])
        placed.append((x, y))
    return placed


def stems(img, rows, colors, origin=(0, 0)):
    """'b' — стебель (тёмный слева, светлее справа), 'B' — тёмная часть стебля."""
    ox, oy = origin
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == "b":
                img.putpixel((x + ox, y + oy), colors[1])
            elif ch == "B":
                img.putpixel((x + ox, y + oy), colors[0])
            elif ch == "c":
                img.putpixel((x + ox, y + oy), colors[2])


# --- ягодные кусты ---

BUSH_SPROUT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......#..##.....",
    ".....###.###....",
    "......##.##.....",
    ".......###......",
    "........#.......",
    "................",
    "................",
]
BUSH_SPROUT_STEM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "........b.......",
    "........b.......",
    ".......Bb.......",
]
BUSH_FULL = [
    "................",
    "................",
    "......##..##....",
    "....#########...",
    "...###########..",
    "..#############.",
    ".##############.",
    ".###############",
    "################",
    ".###############",
    ".##############.",
    "..############..",
    "...###.####.##..",
    "....#...##......",
    "................",
    "................",
]
BUSH_FULL_STEM = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....b..b..c....",
    "......bBb.c.....",
    ".....B.bBb......",
]
CLOUDBERRY_SPROUT = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......##.##.....",
    ".....###.###....",
    "......#...#.....",
    "................",
]
CLOUDBERRY_LEAVES = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......###......",
    "..##..#####.##..",
    ".####.#####.###.",
    ".#####.###.####.",
    "..####..#..###..",
    "...##.#.#..#....",
    "................",
    "................",
]
CLOUDBERRY_STEMS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "......b...b.....",
    ".....b.....b....",
]


def berry(img, kind, x, y, colors):
    shade, base, light = colors
    if kind == "currant":
        # кисть из трёх мелких ягод
        for i, (dx, dy) in enumerate(((0, 0), (1, 1), (0, 2))):
            img.putpixel((x + dx, y + dy), light if i == 0 else (base if i == 1 else shade))
    elif kind == "cloudberry":
        # крупная сборная ягода
        for dx, dy, c in ((0, 0, light), (1, 0, base), (-1, 1, base), (0, 1, base), (1, 1, shade), (0, 2, shade)):
            img.putpixel((x + dx, y + dy), c)
    else:
        for dx, dy, c in ((0, 0, light), (1, 0, base), (0, 1, base), (1, 1, shade)):
            img.putpixel((x + dx, y + dy), c)


def berry_spots(kind, foliage_pts, count, seed):
    """Места для ягод внутри листвы: не у самого края, не вплотную друг к другу."""
    rnd = random.Random(seed)
    pts = set(foliage_pts)
    need = {(0, 0), (1, 1), (0, 2)} if kind == "currant" else {(0, 0), (1, 0), (0, 1), (1, 1)}
    cands = sorted(p for p in pts if all((p[0] + dx, p[1] + dy) in pts for dx, dy in need) and 3 <= p[1] <= 11)
    rnd.shuffle(cands)
    spots = []
    for p in cands:
        if all(abs(p[0] - q[0]) + abs(p[1] - q[1]) >= 4 for q in spots):
            spots.append(p)
        if len(spots) == count:
            break
    return spots


def bush(kind, stage):
    img = blank()
    seed = f"{kind}"
    if kind == "cloudberry":
        if stage == 0:
            foliage(img, CLOUDBERRY_SPROUT, pal(kind), seed, holes=0)
            return img
        stems(img, CLOUDBERRY_STEMS, GREEN_STEM)
        foliage(img, CLOUDBERRY_LEAVES, pal(kind), seed, holes=0.03)
        colors = [hx(c) for c in BERRY_COLORS[kind]]
        # ягоды морошки — по одной на цветоносе над листьями
        spots = [(8, 4), (13, 5), (3, 5)][:{1: 0, 2: 1, 3: 3}[stage]]
        for x, y in spots:
            for sy in range(y + 3, 9):
                img.putpixel((x, sy), GREEN_STEM[1])
            berry(img, kind, x, y, colors)
        return img
    if stage == 0:
        stems(img, BUSH_SPROUT_STEM, STEM)
        foliage(img, BUSH_SPROUT, pal(kind), seed, holes=0)
        return img
    stems(img, BUSH_FULL_STEM, STEM)
    pts = foliage(img, BUSH_FULL, pal(kind), seed)
    if stage >= 2:
        spots = berry_spots(kind, pts, 7, seed)
        colors = [hx(c) for c in BERRY_COLORS[kind]]
        for x, y in spots[:3 if stage == 2 else 7]:
            berry(img, kind, x, y, colors)
    return img


# --- дикая морковь и картошка ---

WILD_CARROT = [
    "................",
    "................",
    "..wWw...........",
    ".wWpWw.....wWw..",
    "..wWw.....wWWWw.",
    "...k.......wWw..",
    "...k........k...",
    "....k......k....",
    "....k.....k.wWw.",
    ".....k...k..wWw.",
    ".....k..k..k....",
    "......k.k.k.....",
    "......kkkk......",
    ".......kk.......",
    ".......k........",
    ".......k........",
]
WILD_CARROT_LEAVES = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "..#.........#...",
    ".###.......###..",
    "..####...####...",
    "...###.#.###....",
    "....##.#.##.....",
    "................",
]
WILD_POTATO = [
    "................",
    "................",
    "................",
    "....v.....v.....",
    "...vyv...vyv....",
    "....v.....v.....",
    "....k.....k.....",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".......k........",
    ".......k........",
]
WILD_POTATO_LEAVES = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "................",
    ".....##...##....",
    "...#####.####...",
    "..#####.######..",
    ".#######.######.",
    ".##############.",
    "..############..",
    "...##########...",
    "....###..###....",
    "................",
    "................",
]


def wild_carrot():
    img = blank()
    foliage(img, WILD_CARROT_LEAVES, pal("carrot"), "carrot", holes=0)
    paint(img, WILD_CARROT, {"k": GREEN_STEM[1], "w": hx("#f2f0e4"), "W": hx("#d2d0c0"), "p": hx("#6a2a5a")})
    return img


def wild_potato():
    img = blank()
    foliage(img, WILD_POTATO_LEAVES, pal("potato"), "potato", holes=0.1)
    paint(img, WILD_POTATO, {"k": GREEN_STEM[1], "v": hx("#e6dcf4"), "y": hx("#f0c830")})
    return img


# --- джут --- рисунок автора, scripts/templates/jute_stages.png (см. plants())

# ============================== ПРЕДМЕТЫ ==============================

ITEM_BERRIES = {
    "currant": [
        "................",
        "................",
        "......gg........",
        ".....gGs........",
        "......s.........",
        "......s.........",
        ".....s.s........",
        "....s...s.......",
        "...hb..hb.......",
        "...bd..bd.s.....",
        "......s...s.....",
        ".....hb..hb.....",
        ".....bd..bd.....",
        "........hb......",
        "........bd......",
        "................",
    ],
    "gooseberry": [
        "................",
        "................",
        "................",
        ".......s........",
        "......gs........",
        ".....oooo.......",
        "....ohhlbbo.....",
        "...ohllbvbbo....",
        "...ohlbbvbbdo...",
        "...olbbbvbbdo...",
        "...obbbbvbddo...",
        "....obbbvbddo...",
        "....oddbvddo....",
        ".....oodddo.....",
        ".......ooo......",
        "................",
    ],
    "blackberry": [
        "................",
        "................",
        "......s.........",
        ".....gsg........",
        "....gg.gg.......",
        ".....obbo.......",
        "....ohbhbo......",
        "...obhbhbbo.....",
        "...ohbhbhbo.....",
        "...obhbhbdo.....",
        "...ohbhbdbo.....",
        "....obdbdo......",
        ".....oddo.......",
        "......oo........",
        "................",
        "................",
    ],
    "cloudberry": [
        "................",
        "................",
        "................",
        ".......s........",
        ".....g.s.g......",
        "....ggg.ggg.....",
        ".....ohho.......",
        "....ohlhlbo.....",
        "...ohlhlbhbo....",
        "...olhlbhbdo....",
        "...ohlbhbdbo....",
        "....obhbdbo.....",
        ".....obddo......",
        "......ooo.......",
        "................",
        "................",
    ],
}
ITEM_BERRY_COLORS = {
    "currant": {"h": "#6a5a92", "b": "#2c2040", "d": "#140e1e", "s": "#5a7a30", "g": "#4b7c30", "G": "#3c6527"},
    "gooseberry": {"h": "#f2fbc8", "l": "#d6ea92", "b": "#b4cf62", "v": "#93ae46", "d": "#7f9a3a",
                   "o": "#4e6422", "s": "#5b482d", "g": "#4b7c30"},
    "blackberry": {"h": "#6a4a8c", "b": "#2c1636", "d": "#160a1e", "o": "#0c0610", "s": "#4b7c30",
                   "g": "#5d953b"},
    "cloudberry": {"h": "#ffd37a", "l": "#f6b450", "b": "#e8942c", "d": "#b25e16", "o": "#6e3608",
                   "s": "#4f7a30", "g": "#649231"},
}

# Пучок джута — тонкая связка, как солома и ванильная пшеница (straw_bundle): две перевязи и веер прядей сверху.
# l/m/d — волокно (светлое, среднее, тёмное), o — контур, R/r — перевязь, b — контур перевязи
JUTE_BUNDLE_COLORS = {"l": "#e6cf92", "m": "#c9ab66", "d": "#97793f", "o": "#5e4524", "R": "#a07a3e", "r": "#6a4e26",
                      "b": "#3e2c16"}


def straw_bundle(width=3, ties=(0,), tail=None, fan=(0, 0), fray=0, spread=1, seed=1, length=(-12, 12)):
    """Тонкая связка волокна по диагонали (снизу слева вверх вправо), как солома или ванильная пшеница.
    d = x + y - 15 — сдвиг поперёк связки, u = x - y — вдоль неё. width — полуширина в пиксельных диагоналях,
    ties — где перевязи (u), tail — концы бечёвки от первой перевязи ('r' вправо-вниз, 'l' влево-вверх),
    fan — с какой длины от конца (низ, верх) пряди расходятся веером, fray — насколько неровны кончики."""
    rnd = random.Random(seed)
    lo, hi = length
    grid = [["."] * 16 for _ in range(16)]
    # как у соломы: светлая прожилка посередине, по бокам средние и тёмные, снизу-справа тёмный край
    stripes = {-4: "o", -3: "d", -2: "m", -1: "d", 0: "m", 1: "l", 2: "m", 3: "d", 4: "o"}
    tips = {}
    for d in range(-12, 13):
        tips[d] = (lo + rnd.randint(0, fray), hi - rnd.randint(0, fray))
    for y in range(16):
        for x in range(16):
            u = x - y
            d = x + y - 15
            t_lo, t_hi = tips.get(d, (lo, hi))
            if not (t_lo <= u <= t_hi):
                continue
            w = width
            fan_lo, fan_hi = fan
            if fan_lo and u < lo + fan_lo:
                w += (lo + fan_lo - u) * spread // 2
            if fan_hi and u > hi - fan_hi:
                w += (u - (hi - fan_hi)) * spread // 2
            if abs(d) > w:
                continue
            in_fan = (fan_lo and u < lo + fan_lo) or (fan_hi and u > hi - fan_hi)
            if in_fan and abs(d) > width and d % 2:
                continue  # в веере пряди врозь
            if abs(d) > width:  # прядь веера
                ch = "l" if d % 4 == 0 else "m"
            else:
                ch = stripes[max(-4, min(4, d))]
            if u in (t_lo, t_hi):
                ch = "d" if ch == "l" else ch
            for tu in ties:
                if tu - 1 <= u <= tu + 1 and abs(d) <= width + 1:
                    ch = "b" if abs(d) >= width or u != tu else ("R" if d % 2 else "r")
            grid[y][x] = ch
    if tail:
        tu = ties[0]
        # концы бечёвки: от края перевязи две нитки вниз
        side = 1 if tail == "r" else -1
        d0 = side * (width + 2)
        for k in range(4):
            for u in (tu + k // 2 * side - k % 2 * side, tu + 1 + k // 2 * side):
                x = (u + 15 + d0 + side * k) // 2
                y = x - u
                if 0 <= x < 16 and 0 <= y < 16 and grid[y][x] == ".":
                    grid[y][x] = "b" if k % 2 else "R"
    return ["".join(r) for r in grid]

JUTE_BUNDLE = straw_bundle(width=3, ties=(-6, -1), fan=(0, 7), spread=2, fray=2, seed=18)

JUTE_SEEDS = [
    "................",
    "................",
    "................",
    "................",
    ".......lm.......",
    "......lmd.......",
    "...lm..d...lm...",
    "..lmd.....lmd...",
    "...d.......d....",
    ".......lm.......",
    "......lmd.......",
    ".......d..lm....",
    "....lm...lmd....",
    "...lmd....d.....",
    "....d...........",
    "................",
]
JUTE_SEEDS_COLORS = {"l": "#8a7a52", "m": "#5e5034", "d": "#3a3020"}


def berry_item(kind):
    return paint(blank(), ITEM_BERRIES[kind], {k: hx(v) for k, v in ITEM_BERRY_COLORS[kind].items()})


def items():
    for kind in ITEM_BERRIES:
        save(berry_item(kind), f"item/{kind}")
    save(paint(blank(), JUTE_BUNDLE, {k: hx(v) for k, v in JUTE_BUNDLE_COLORS.items()}), "item/jute")
    save(paint(blank(), JUTE_SEEDS, {k: hx(v) for k, v in JUTE_SEEDS_COLORS.items()}), "item/jute_seeds")
    # верёвка — рисунок автора (textures/item/rope.png), скрипт её не перерисовывает


BERRY_BUSHES = ["currant", "cloudberry", "gooseberry", "blackberry"]


def plants():
    for kind in BERRY_BUSHES:
        for stage in range(4):
            save(bush(kind, stage), f"block/{kind}_bush_stage{stage}")
    save(wild_carrot(), "block/wild_carrot")
    save(wild_potato(), "block/wild_potato")
    # Джут — рисунок автора: 5 стадий по 16×32 (два блока), дикий джут — как спелый (5-я стадия, с цветами)
    stages = Image.open(os.path.join(os.path.dirname(__file__), "templates", "jute_stages.png")).convert("RGBA")
    for age in range(5):
        save(stages.crop((16 * age, 16, 16 * age + 16, 32)), f"block/jute_crop_stage{age}_bottom")
        save(stages.crop((16 * age, 0, 16 * age + 16, 16)), f"block/jute_crop_stage{age}_top")
    save(stages.crop((64, 16, 80, 32)), "block/wild_jute_bottom")
    save(stages.crop((64, 0, 80, 16)), "block/wild_jute_top")
    for old in ("jute_crop_stage0", "jute_crop_stage1", "jute_crop_stage2"):
        p = os.path.join(TEX, "block", f"{old}.png")
        if os.path.exists(p):
            os.remove(p)


def preview():
    """Общий лист всего, что рисует скрипт (docs/textures/farm.png)."""
    rows = [
        [load(f"block/{s}_farmland.png") for s in FARMLAND_SOILS] + [load(f"block/{s}_farmland_moist.png") for s in FARMLAND_SOILS],
    ]
    for kind in BERRY_BUSHES:
        rows.append([load(f"block/{kind}_bush_stage{i}.png") for i in range(4)] + [load(f"item/{kind}.png")])
    tall = []
    for top, bottom in [("wild_jute_top", "wild_jute_bottom")] + [(f"jute_crop_stage{i}_top", f"jute_crop_stage{i}_bottom")
                                                                for i in range(5)]:
        im = Image.new("RGBA", (16, 32))
        im.paste(load(f"block/{top}.png"), (0, 0))
        im.paste(load(f"block/{bottom}.png"), (0, 16))
        tall.append(im)
    rows.append([load("block/wild_carrot.png"), load("block/wild_potato.png")])
    rows.append([load("item/jute.png"), load("item/jute_seeds.png"), load("item/rope.png")])
    out = sheet(rows)
    big = sheet([[t] for t in tall], cell=32)
    full = Image.new("RGBA", (out.width + big.width, max(out.height, big.height)), (60, 60, 60, 255))
    full.paste(out, (0, 0))
    full.paste(big, (out.width, 0))
    full.save(os.path.join(DOCS, "farm.png"))


if __name__ == "__main__":
    hoes()
    farmlands()
    plants()
    items()
    preview()
    print("ok")

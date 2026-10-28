"""
Рисует текстуры для земледелия: тяпки, грядки, ягодные кусты и ягоды, дикие морковь и картошку, джут и верёвку.

Запуск из корня проекта:  python scripts/gen_farm_textures.py [--hoe A|B|C|D]
Нужен Pillow (pip install pillow). Скрипт перезаписывает только свои текстуры (список — в конце файла).

Тяпки рисуются в четырёх вариантах формы: A — классическая (головка влево, лезвие загнуто вниз), B — широкая
(плоское полотно), C — треугольная, D — рисунок автора (головка вправо, свой черенок). В игру идёт выбранный
вариант (по умолчанию D), все варианты лежат в docs/textures/hoes/<вариант>/, а общий лист для сравнения —
docs/textures/hoes.png. Черенок у A–C — обработанная палка (textures/item/stick_processed.png).
"""
import os
import random
import sys

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
# Кремень и кость привязаны к черенку жгутом (j — светлый, J — тёмный), у металлов на этом месте втулка
HOE_BINDING = {"j": hx("#c2a86a"), "J": hx("#7e6436")}

# Головка поверх черенка-палки. 'x' стирает кончик палки, j/J — обмотка или втулка.
HOE_VARIANTS = {
    "A": [
        "................",
        "...ohhhllllmmJx.",
        "..ohllmmmmnndJj.",
        "..odnd......j...",
        "..osd...........",
        "...oo...........",
    ],
    "B": [
        "................",
        "........ommmJJx.",
        "..ohhhllmmndoJj.",
        "..hllmmmnnds.j..",
        "..lmmmnndds.....",
        "..lmnnddss......",
        "..hhhllmmo......",
    ],
    "C": [
        "................",
        "......ohlllmmJx.",
        ".....ohlmmmnnJj.",
        "....ohlmmnds.j..",
        "...ohlmnds......",
        "..ohlnds........",
        "..hlds..........",
        "..lso...........",
        "...o............",
    ],
}


# D — рисунок автора целиком, с черенком (1 — светлый, 2 — средний, 3 — тёмный, 4 — торец).
# Головка: h — блик, l — светлый, m — средний, d — тёмный, o — контур.
HOE_FULL = {
    "D": [
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
    ],
}
HOE_FULL_HANDLE = {"1": hx("#b8945a"), "2": hx("#8f6a3a"), "3": hx("#6b4c28"), "4": hx("#45301a")}
# Цвета головки металлов из рисунка автора: h, l, m, d, o
HOE_FULL_HEADS = {
    "copper": ["#f0aa82", "#d68258", "#b4623c", "#8a4226", "#602c1a"],
    "bronze": ["#f0c86e", "#d7a242", "#b27a26", "#825418", "#583610"],
    "iron": ["#f2f9ff", "#cdd3e0", "#b7bdc8", "#515459", "#323337"],
    "steel": ["#dce6f0", "#b0bcca", "#8c98a8", "#545c6a", "#2e323a"],
}
HOE_VARIANTS.update(HOE_FULL)


def hoe_tones(material):
    r = [hx(c) if c else None for c in HOE_RAMPS[material]]
    if r[3] is None:
        r[3] = mix(r[2], r[4])
    tones = dict(zip("hlmndso", r))
    if material in ("flint", "bone"):
        tones.update(HOE_BINDING)
    else:
        tones.update({"j": tones["d"], "J": tones["s"]})
    return tones


def hoe(material, variant):
    if variant in HOE_FULL:
        tones = dict(HOE_FULL_HANDLE)
        if material in HOE_FULL_HEADS:
            tones.update(zip("hlmdo", map(hx, HOE_FULL_HEADS[material])))
        else:
            ramp = hoe_tones(material)
            tones.update({k: ramp[k] for k in "hlmdo"})
        img = paint(blank(), HOE_VARIANTS[variant], tones)
    else:
        img = paint(load("item/stick_processed.png"), HOE_VARIANTS[variant], hoe_tones(material))
    if material == "flint":
        # сколы на кремне: светлые точки по краю полотна
        rnd = random.Random(variant)
        tones = hoe_tones(material)
        for x, y in [(x, y) for y in range(16) for x in range(16)
                     if img.getpixel((x, y)) in (tones["m"], tones["n"])]:
            if rnd.random() < 0.3:
                img.putpixel((x, y), tones["l"])
    return img


def hoes(chosen):
    rows = []
    for variant in HOE_VARIANTS:
        row = []
        for m in HOE_MATERIALS:
            img = hoe(m, variant)
            d = os.path.join(DOCS, "hoes", variant)
            os.makedirs(d, exist_ok=True)
            img.save(os.path.join(d, f"{m}_hoe.png"))
            if variant == chosen:
                save(img, f"item/{m}_hoe")
            row.append(img)
        rows.append(row)
    os.makedirs(DOCS, exist_ok=True)
    # слева подписи вариантов, сверху — материалы
    grid = sheet(rows)
    out = Image.new("RGBA", (grid.width + 40, grid.height + 24), (60, 60, 60, 255))
    out.paste(grid, (40, 24))
    draw = ImageDraw.Draw(out)
    for r, variant in enumerate(HOE_VARIANTS):
        draw.text((12, 24 + 6 + r * 102 + 44), variant + (" *" if variant == chosen else ""), fill=(255, 255, 255, 255))
    for c, m in enumerate(HOE_MATERIALS):
        draw.text((40 + 6 + c * 102 + 30, 8), m, fill=(255, 255, 255, 255))
    out.save(os.path.join(DOCS, "hoes.png"))


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

# Пучки джута: 40 вариантов (K–O — по B, P–T — по C, M1–M10 — смесь B, D, K и N в цветах волокна, V1–V10 — по M10, W1–W10 — они же с прядями пучками,
# X1–X10 — тонкие, как солома и пшеница: straw_bundle; Y1–Y10 — по X2 и X10). l/m/d — волокно (светлое, среднее, тёмное), o — контур волокна, R/r — перевязь,
# b — контур перевязи, g/G/k — зелёные стебли и листья (сырой джут)
JUTE_BUNDLES = {
    # A — косой сноп, перевязан посередине, концы веером
    "A": [
        "................",
        "...........o.o..",
        "..........olomo.",
        ".........olmlmdo",
        "........olmlmdo.",
        ".......olmlmdo..",
        "......olmlmdo...",
        ".....bRrRrb.....",
        "....brRrRb......",
        "...olmlmdo......",
        "..olmlmdo.......",
        ".olmlmdo........",
        "olmlmdo.........",
        "omlodo..........",
        ".o.o............",
        "................",
    ],
    # B — прямой сноп стоймя, перевязь посередине, концы расходятся
    "B": [
        "................",
        "...o..o..o..o...",
        "..olo.lo.ol.ol..",
        "...olmlmlmlmo...",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....bRrRrRrb....",
        "....brRrRrRb....",
        "....olmlmlmo....",
        "....olmlmldo....",
        "...olmlmlmldo...",
        "..olmlmlmlmldo..",
        "..ololmlmlolo...",
        "...o.o.o.o.o....",
        "................",
    ],
    # C — моток: волокно сложено петлёй и перехвачено у сгиба
    "C": [
        "................",
        "......oooo......",
        ".....olmmlo.....",
        "....olo..olo....",
        "....omo..omo....",
        "....olo..olo....",
        "....omo..omo....",
        "....bRrRrRb.....",
        "....brRrRrb.....",
        "....olmlmlo.....",
        "....omlmlmo.....",
        "...olmlomlmo....",
        "...olmo.olmo....",
        "..olmo...olmo...",
        "..ooo.....ooo...",
        "................",
    ],
    # D — свежие стебли джута с листьями, связаны пучком
    "D": [
        "................",
        "..G.....G...G...",
        ".Gg..G.gG..Gg...",
        "..gk.Gg.k..gk...",
        "...k..gkk.kG....",
        "...kG..kk.k.....",
        "....k..kkk......",
        "....kk.kk.......",
        "....bRrRb.......",
        "....bRrRb.......",
        "....kkkkk.......",
        "...kkk.kk.......",
        "...kk..kk.......",
        "..kk...k.k......",
        "..k....k..k.....",
        "................",
    ],
    # E — коса из трёх прядей по диагонали
    "E": [
        "................",
        "............oo..",
        "...........olmo.",
        "..........olmdo.",
        ".........omldo..",
        "........olmdo...",
        ".......odlmo....",
        "......olmdo.....",
        ".....omldo......",
        "....olmdo.......",
        "...odlmo........",
        "..bRrb..........",
        ".bRrb...........",
        "olmo............",
        "lo.o............",
        "................",
    ],
    # F — два снопа крест-накрест, перевязаны в середине
    "F": [
        "................",
        ".o.o........o.o.",
        "olmlo......olmlo",
        ".olmlo....olmlo.",
        "..olmlo..olmlo..",
        "...olmlo.olmlo..",
        "....olmlolmlo...",
        ".....obRrRbo....",
        ".....obrRrbo....",
        "....olmlolmlo...",
        "...olmlo..olmlo.",
        "..olmlo....olmlo",
        ".olmlo......olml",
        "olmlo........olo",
        ".o.o..........o.",
        "................",
    ],
    # G — сноп, сложенный пополам: сгиб наверху, хвосты внизу
    "G": [
        "................",
        ".....oooooo.....",
        "....olmlmlmo....",
        "...olmdoodlmo...",
        "...omdo..odmo...",
        "...olo....olo...",
        "...bRrRrRrRrb...",
        "...brRrRrRrRb...",
        "...olmlmlmlmo...",
        "...olmlmlmldo...",
        "...olmlmlmldo...",
        "..olmlmlmlmldo..",
        "..olmlmlmlmldo..",
        ".olmlolmlolmldo.",
        ".o.o.o.o.o.o.o..",
        "................",
    ],
    # H — скатанный клубок с хвостом нити
    "H": [
        "................",
        "................",
        ".....oooooo.....",
        "....olmlmlmo....",
        "...olmdmlmdmo...",
        "..olmlmdmlmlmo..",
        "..omdmlmdmlmdo..",
        "..olmlmdmlmdmo..",
        "..omdmlmlmdmlo..",
        "..olmlmdmlmdmo..",
        "...odmlmdmlmo...",
        "....odmlmlmo....",
        ".....oooooomo...",
        "...........omo..",
        "............olo.",
        ".............o..",
    ],
    # I — тугой жгут волокна, перевязан в двух местах
    "I": [
        "................",
        ".............oo.",
        "............olo.",
        "...........olmo.",
        "..........bRrb..",
        ".........olmdo..",
        "........olmdo...",
        ".......olmdo....",
        "......olmdo.....",
        ".....olmdo......",
        "....bRrb........",
        "...olmdo........",
        "..olmdo.........",
        ".olmlo..........",
        ".olo............",
        "..o.............",
    ],
    # J — пучок стеблей с волокном: сверху светлое волокно, снизу зелёные стебли, перевязан
    "J": [
        "................",
        "...........o.o..",
        "..........olmlo.",
        ".........olmlmdo",
        "........olmlmdo.",
        ".......olmlmdo..",
        "......olmlmdo...",
        ".....bRrRrb.....",
        "....brRrRb......",
        "...kgkGkgk......",
        "..kgkGkgk.......",
        ".kgkGkgk........",
        "kgkGkgk.........",
        "kGkgk...........",
        ".k.k............",
        "................",
    ],

    # K (по B) — стройный сноп, перевязан в двух местах
    "K": [
        "................",
        "....o..o..o.....",
        "...olo.lo.olo...",
        "....olmlmlmo....",
        ".....olmlmo.....",
        ".....bRrRrb.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....bRrRrb.....",
        ".....olmlmo.....",
        "....olmlmldo....",
        "...olmlmlmldo...",
        "...o.o.o.o.o....",
        "................",
    ],
    # L (по B) — сноп с тонкой талией, концы широким веером сверху и снизу
    "L": [
        "................",
        ".o..o..o..o..o..",
        ".olo.lo.ol.olo..",
        "..olmlmlmlmlo...",
        "...olmlmlmlo....",
        "....olmlmlo.....",
        ".....olmlo......",
        ".....bRrRb......",
        ".....brRrb......",
        ".....olmlo......",
        "....olmlmlo.....",
        "...olmlmlmldo...",
        "..olmlmlmlmldo..",
        ".olmlolmlolmldo.",
        ".o.o.o.o.o.o.o..",
        "................",
    ],
    # M (по B) — аккуратный сноп с ровно срезанными концами
    "M": [
        "................",
        "................",
        "....oooooooo....",
        "....ollmlmlo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmldo....",
        "....bRrRrRrb....",
        "....brRrRrRb....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmldo....",
        "....olmlmldo....",
        "....oooooooo....",
        "................",
        "................",
    ],
    # N (по B) — сноп, перевязан бечёвкой с узлом и свисающими концами
    "N": [
        "................",
        "...o..o..o..o...",
        "..olo.lo.ol.ol..",
        "...olmlmlmlmo...",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....bRrRrRrbb...",
        "....brRrRrRbRb..",
        "....olmlmlmobRb.",
        "....olmlmldo.br.",
        "...olmlmlmldo.b.",
        "..olmlmlmlmldo..",
        "..ololmlmlolo...",
        "...o.o.o.o.o....",
        "................",
    ],
    # O (по B) — короткий толстый сноп
    "O": [
        "................",
        "................",
        "................",
        "..o.o..o..o.o...",
        ".olololo.ololo..",
        "..olmlmlmlmlmo..",
        "...olmlmlmlmo...",
        "...bRrRrRrRrb...",
        "...brRrRrRrRb...",
        "...olmlmlmlmo...",
        "..olmlmlmlmldo..",
        ".olmlmlmlmlmldo.",
        ".olololmlmlolo..",
        "..o.o.o.o.o.o...",
        "................",
        "................",
    ],
    # P (по C) — моток с длинной петлёй и прямыми хвостами
    "P": [
        ".....oooo.......",
        "....olmmlo......",
        "....olo.olo.....",
        "....omo.omo.....",
        "....olo.olo.....",
        "....omo.omo.....",
        "....olo.olo.....",
        "....bRrRrRb.....",
        "....brRrRrb.....",
        "....olmlmlo.....",
        "....omlmlmo.....",
        "....olmlmlo.....",
        "....omlomlo.....",
        "....olmoolmo....",
        "....ooo..ooo....",
        "................",
    ],
    # Q (по C) — моток, лежащий наискось: петля справа вверху
    "Q": [
        "................",
        "..........ooo...",
        ".........olmlo..",
        "........olo.olo.",
        ".......olo..olo.",
        "......olo..olo..",
        ".....olo.olo....",
        "....bRrRbo......",
        "...brRrRb.......",
        "..olmlmo........",
        ".olmlmlo........",
        "olmlolo.........",
        "olo.olo.........",
        "oo..oo..........",
        "................",
        "................",
    ],
    # R (по C) — восьмёрка: петля сверху и снизу, перехват посередине
    "R": [
        "................",
        "......oooo......",
        ".....olmmlo.....",
        "....olo..olo....",
        "....omo..omo....",
        "....olo..olo....",
        ".....olmmlo.....",
        "....bRrRrRb.....",
        "....brRrRrb.....",
        ".....olmmlo.....",
        "....olo..olo....",
        "....omo..omo....",
        "....olo..olo....",
        ".....olmmlo.....",
        "......oooo......",
        "................",
    ],
    # S (по C) — моток, хвосты перехвачены второй перевязью
    "S": [
        "................",
        "......oooo......",
        ".....olmmlo.....",
        "....olo..olo....",
        "....omo..omo....",
        "....olo..olo....",
        "....bRrRrRb.....",
        "....brRrRrb.....",
        "....olmlmlo.....",
        "....omlmlmo.....",
        "....olmlmlo.....",
        "....bRrRrRb.....",
        "...olmlolmlo....",
        "..olmo...olmo...",
        "..ooo.....ooo...",
        "................",
    ],
    # T (по C) — моток с узелком-петлёй для подвеса сверху
    "T": [
        "......bb........",
        ".....b..b.......",
        ".....b..b.......",
        "......bb........",
        ".....oRRo.......",
        "....olmmlo......",
        "...olo..olo.....",
        "...omo..omo.....",
        "...olo..olo.....",
        "...bRrRrRb......",
        "...brRrRrb......",
        "...olmlmlo......",
        "..olmlomlmo.....",
        "..olmo.olmo.....",
        ".olmo...olmo....",
        ".ooo.....ooo....",
    ],

    # M1 — сноп B, две перевязи (K), на нижней узел с концами (N)
    "M1": [
        "................",
        "...o..o..o..o...",
        "..olo.lo.ol.ol..",
        "...olmlmlmlmo...",
        "....olmlmlmo....",
        "....bRrRrRrb....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....bRrRrRrbb...",
        "....olmlmlmobRb.",
        "...olmlmlmldobr.",
        "..olmlmlmlmldob.",
        "..ololmlmlolo...",
        "...o.o.o.o.o....",
        "................",
    ],
    # M2 — стройный сноп K, сверху пряди расходятся, как стебли D
    "M2": [
        "..l.....l...l...",
        ".ld..l.ml..ld...",
        "..md.lm.d..md...",
        "...d..mdd.dl....",
        "....d..dd.d.....",
        ".....olmlmo.....",
        ".....bRrRrb.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....bRrRrb.....",
        ".....olmlmo.....",
        "....olmlmldo....",
        "...olmlmlmldo...",
        "...o.o.o.o.o....",
        "................",
    ],
    # M3 — сноп B с узлом N, верхние концы растрёпаны прядями, как у D
    "M3": [
        "..l...l...l..l..",
        ".lm..lm..ml.lm..",
        "..md..md.d.md...",
        "...olmlmlmlmo...",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....bRrRrRrbb...",
        "....brRrRrRbRb..",
        "....olmlmlmobRb.",
        "....olmlmldo.br.",
        "...olmlmlmldo.b.",
        "..olmlmlmlmldo..",
        "..ololmlmlolo...",
        "...o.o.o.o.o....",
        "................",
    ],
    # M4 — стройный K, одна перевязь с узлом и концами (N), низ веером (B)
    "M4": [
        "................",
        "....o..o..o.....",
        "...olo.lo.olo...",
        "....olmlmlmo....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        ".....olmlmo.br..",
        "....olmlmldo.b..",
        "...olmlmlmldo...",
        "..olmlmlmlmldo..",
        "..o.o.o.o.o.o...",
        "................",
    ],
    # M5 — пучок прядей по форме D, в цвет волокна, перехвачен дважды (K)
    "M5": [
        "..l.....l...l...",
        ".lm..l.ml..lm...",
        "..md.lm.d..md...",
        "...d..mdd.dl....",
        "....d.olmo......",
        "....bRrRrb......",
        "....olmlmo......",
        "....olmlmo......",
        "....bRrRrb......",
        "....olmlmo......",
        "...olmlmldo.....",
        "...lm.lm.md.....",
        "..lm..lm..md....",
        "..d...d....d....",
        "................",
        "................",
    ],
    # M6 — сноп B, узел слева, концы бечёвки свисают (N зеркально)
    "M6": [
        "................",
        "...o..o..o..o...",
        "..ol.ol.ol.olo..",
        "...olmlmlmlmo...",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "...bbRrRrRrb....",
        "..bRbrRrRrRb....",
        ".bRbolmlmlmo....",
        ".rb.olmlmldo....",
        ".b.olmlmlmldo...",
        "..olmlmlmlmldo..",
        "...ololmlmlolo..",
        "....o.o.o.o.o...",
        "................",
    ],
    # M7 — стройный K: сверху пряди D, снизу веер B, две перевязи, на верхней узел N
    "M7": [
        "..l.....l...l...",
        ".lm..l.ml..lm...",
        "..md.lm.d..md...",
        "...d..mdd.d.....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....olmlmobRb..",
        ".....olmlmo.br..",
        ".....olmlmo..b..",
        ".....bRrRrb.....",
        ".....olmlmo.....",
        "....olmlmldo....",
        "...olmlmlmldo...",
        "..olmlmlmlmldo..",
        "..o.o.o.o.o.o...",
        "................",
    ],
    # M8 — широкий сноп B, обе стороны растрёпаны прядями D, одна перевязь
    "M8": [
        ".l...l...l...l..",
        ".lm.lm..ml..ml..",
        "..md.md.d..md...",
        "...olmlmlmlmo...",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....olmlmlmo....",
        "....bRrRrRrb....",
        "....brRrRrRb....",
        "....olmlmlmo....",
        "....olmlmldo....",
        "...olmlmlmldo...",
        "..dm.md.md..md..",
        ".dm..m..m.d..md.",
        ".d...d..d..d..d.",
        "................",
    ],
    # M9 — стройный K, две перевязи, узел с концами на нижней (N)
    "M9": [
        "................",
        "....o..o..o.....",
        "...olo.lo.olo...",
        "....olmlmlmo....",
        ".....olmlmo.....",
        ".....bRrRrb.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....olmlmobRb..",
        "....olmlmldobr..",
        "...olmlmlmldob..",
        "...o.o.o.o.o....",
        "................",
        "................",
    ],
    # M10 — пряди D в цвет волокна, перехват с узлом и свисающими концами (N)
    "M10": [
        "..l.....l...l...",
        ".lm..l.ml..lm...",
        "..md.lm.d..md...",
        "...d..mdd.dl....",
        "...d..mdd.d.....",
        "....d.olmo......",
        "....bRrRrbb.....",
        "....brRrRbRb....",
        "....olmlmobRb...",
        "...olmlmldobr...",
        "...lm.lm.md.b...",
        "..lm..lm..md....",
        "..d...d....d....",
        ".d....d.....d...",
        "................",
        "................",
    ],

    # V1 — M10 по центру и симметрично
    "V1": [
        "..l....l....l...",
        ".lm...lm...ml...",
        "..md..md..md....",
        "...d..md.dm.....",
        "....d.mdmd......",
        ".....olmlo......",
        ".....bRrRbb.....",
        ".....brRrbRb....",
        ".....olmlobRb...",
        "....olmlmldbr...",
        "....lm.lm.md.b..",
        "...lm..lm..md...",
        "...d...d....d...",
        "..d....d.....d..",
        "................",
        "................",
    ],
    # V2 — гуще: больше прядей сверху и снизу
    "V2": [
        ".l..l..l..l..l..",
        ".lm.lm.ml.ml.lm.",
        "..md.md.d.md.m..",
        "...d.mdmdmd.d...",
        "....dmdmdmd.....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        "....olmlmlmdbr..",
        "...lmlmlmlmdm.b.",
        "..lm.lm.lm.md...",
        "..d..d..d..dm...",
        ".d...d...d..d...",
        "................",
        "................",
    ],
    # V3 — длинные концы бечёвки до самого низа
    "V3": [
        "..l.....l...l...",
        ".lm..l.ml..lm...",
        "..md.lm.d..md...",
        "...d..mdd.dl....",
        "...d..mdd.d.....",
        "....d.olmo......",
        "....bRrRrbb.....",
        "....brRrRbRb....",
        "....olmlmobRb...",
        "...olmlmldobr...",
        "...lm.lm.md.bR..",
        "..lm..lm..md.b..",
        "..d...d....d.R..",
        ".d....d.....db..",
        ".............r..",
        "................",
    ],
    # V4 — две перевязи, узел с концами на верхней
    "V4": [
        "..l.....l...l...",
        ".lm..l.ml..lm...",
        "..md.lm.d..md...",
        "...d..mdd.d.....",
        "....d.olmo......",
        "....bRrRrbb.....",
        "....brRrRbRb....",
        "....olmlmobRb...",
        "....olmlmo.br...",
        "....bRrRrb..b...",
        "...olmlmldo.....",
        "...lm.lm.md.....",
        "..lm..lm..md....",
        "..d...d....d....",
        ".d....d.....d...",
        "................",
    ],
    # V5 — пряди закручены волной
    "V5": [
        "...l....l...l...",
        "..lm...lm..ml...",
        ".md...md...md...",
        "..md...md.md....",
        "...d..mdd.d.....",
        "....d.olmo......",
        "....bRrRrbb.....",
        "....brRrRbRb....",
        "....olmlmobRb...",
        "...olmlmldobr...",
        "...md.lm.dm.b...",
        "..md...md..md...",
        "...md..dm...md..",
        "....d...d....d..",
        "................",
        "................",
    ],
    # V6 — наклонён наискось вправо
    "V6": [
        "......l....l..l.",
        ".....lm..l.ml.l.",
        "......md.lm.dml.",
        ".......d.mdd.d..",
        "........dolmo...",
        ".......bRrRbb...",
        "......brRrbRb...",
        ".....olmlmobRb..",
        "....olmlmldobr..",
        "...lm.lm.md..b..",
        "..lm..lm.md.....",
        ".lm..lm..d......",
        ".d...d..d.......",
        "d...d...........",
        "................",
        "................",
    ],
    # V7 — меньше прядей, но толще
    "V7": [
        "..ll.....ll.....",
        "..lmd...lmd.....",
        "...md..lmd......",
        "...lmd.lmd......",
        "....lmdlmd......",
        "....olmlmo......",
        "....bRrRrbb.....",
        "....brRrRbRb....",
        "....olmlmobRb...",
        "...olmlmldobr...",
        "...lmd.lmd..b...",
        "..lmd...lmd.....",
        "..lmd....lmd....",
        "..ld......ld....",
        "................",
        "................",
    ],
    # V8 — узел и концы слева
    "V8": [
        "...l...l.....l..",
        "...ml.lm.l..ml..",
        "...md..d.ml.dm..",
        "....ld.ddm..d...",
        ".....d.ddm..d...",
        "......omlo.d....",
        ".....bbRrRrb....",
        "....bRbRrRrb....",
        "...bRbomlmlo....",
        "...rbodlmlmlo...",
        "...b.dm.ml.ml...",
        "....dm..ml..ml..",
        "....d....d...d..",
        "...d.....d....d.",
        "................",
        "................",
    ],
    # V9 — короткий верх, длинные хвосты внизу
    "V9": [
        "................",
        "................",
        "...l...l...l....",
        "...md.lm..md....",
        "....d.mdd.d.....",
        "....d.olmo......",
        "....bRrRrbb.....",
        "....brRrRbRb....",
        "....olmlmobRb...",
        "...olmlmldobr...",
        "...lm.lm.md.b...",
        "...lm.lm.md.....",
        "..lm..lm..md....",
        "..lm..lm..md....",
        ".d....d.....d...",
        ".d....d.....d...",
    ],
    # V10 — бант: две петли бечёвки вместо свисающих концов
    "V10": [
        "..l.....l...l...",
        ".lm..l.ml..lm...",
        "..md.lm.d..md...",
        "...d..mdd.dl....",
        "...d..mdd.d.....",
        "....d.olmo.bb...",
        "....bRrRrbbRrb..",
        "....brRrRbRbbb..",
        "....olmlmobb....",
        "...olmlmldobb...",
        "...lm.lm.mdbRb..",
        "..lm..lm..mdbb..",
        "..d...d....d....",
        ".d....d.....d...",
        "................",
        "................",
    ],

    # W1 — V1 пучками: три пучка сверху и снизу сходятся к перевязи
    "W1": [
        "................",
        "..lm...lm...ml..",
        "..lmd..lm..dml..",
        "...lmd.lm.dml...",
        "....lmdlmdml....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        "....olmlmldobr..",
        "....lmdlmdml.b..",
        "...lmd.lm.dml...",
        "..lmd..lm..dml..",
        "..ld...ld...dl..",
        "................",
        "................",
    ],
    # W2 — V2 пучками: четыре пучка, гуще
    "W2": [
        ".lm..lm..ml..ml.",
        ".lmd.lmd.dml.dml",
        "..lmd.lmdml.dml.",
        "...lmdlmdmldml..",
        ".....olmlmlo....",
        ".....bRrRrRbb...",
        ".....brRrRrbRb..",
        ".....olmlmlobRb.",
        "....olmlmlmdobr.",
        "...lmdlmdmldml.b",
        "..lmd.lmdml.dml.",
        ".lmd.lmd..dml.dm",
        ".ld..ld....dl..d",
        "................",
        "................",
        "................",
    ],
    # W3 — V3 пучками: концы бечёвки до низа
    "W3": [
        "................",
        "..lm...lm...ml..",
        "..lmd..lm..dml..",
        "...lmd.lm.dml...",
        "....lmdlmdml....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        "....olmlmldobR..",
        "....lmdlmdml.b..",
        "...lmd.lm.dmlR..",
        "..lmd..lm..dmb..",
        "..ld...ld...dR..",
        ".............b..",
        "................",
    ],
    # W4 — V4 пучками: две перевязи, узел на верхней
    "W4": [
        "..lm...lm...ml..",
        "..lmd..lm..dml..",
        "...lmd.lm.dml...",
        "....lmdlmdml....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        ".....olmlmo.br..",
        ".....bRrRrb..b..",
        "....olmlmldo....",
        "....lmdlmdml....",
        "...lmd.lm.dml...",
        "..lmd..lm..dml..",
        "..ld...ld...dl..",
        "................",
    ],
    # W5 — V5 пучками: пучки изогнуты волной
    "W5": [
        "................",
        "...lm..lm...ml..",
        "..lmd..lm..dml..",
        "..lmd.lmd..dml..",
        "...lmdlmd.dml...",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        "....olmlmldobr..",
        "...dmldmllmd.b..",
        "..dml.dml.lmd...",
        "...dml.dml.lmd..",
        "....dl..dl..ld..",
        "................",
        "................",
    ],
    # W6 — V6 пучками: наискось вправо
    "W6": [
        "........lm..lm..",
        ".......lmd.lmd..",
        "......lmd.lmd...",
        ".....lmdlmd.....",
        ".....olmlmo.....",
        "....bRrRrbb.....",
        "...brRrRbRb.....",
        "...olmlmobRb....",
        "..olmlmldobr....",
        ".lmdlmdml..b....",
        "lmd.lmd.........",
        "lm..lm..........",
        "................",
        "................",
        "................",
        "................",
    ],
    # W7 — V7 пучками: два толстых пучка
    "W7": [
        "................",
        "..llm.....mll...",
        "..lmmd...dmml...",
        "...lmmd.dmml....",
        "....lmmdmml.....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        "....olmlmldobr..",
        "....lmmdmml..b..",
        "...lmmd.dmml....",
        "..lmmd...dmml...",
        "..lld.....dll...",
        "................",
        "................",
    ],
    # W8 — V8 пучками: узел и концы слева
    "W8": [
        "................",
        "..lm...ml...ml..",
        "..lmd..ml..dml..",
        "...lmd.ml.dml...",
        "....lmdmldml....",
        ".....olmlmo.....",
        "....bbRrRrb.....",
        "...bRbRrRrb.....",
        "..bRbolmlmo.....",
        "..rbodlmlmlo....",
        "..b.lmdmldml....",
        "...lmd.ml.dml...",
        "..lmd..ml..dml..",
        "..ld...ld...dl..",
        "................",
        "................",
    ],
    # W9 — V9 пучками: короткий верх, длинные хвосты
    "W9": [
        "................",
        "................",
        "...lm..lm..ml...",
        "....lmdlmdml....",
        ".....olmlmo.....",
        ".....bRrRrbb....",
        ".....brRrRbRb...",
        ".....olmlmobRb..",
        "....olmlmldobr..",
        "....lmdlmdml.b..",
        "....lmd.lm.dml..",
        "...lmd..lm.dml..",
        "...lmd..lm..dml.",
        "..lmd...lm..dml.",
        "..ld....ld...dl.",
        "................",
    ],
    # W10 — V10 пучками: с бантом
    "W10": [
        "................",
        "..lm...lm...ml..",
        "..lmd..lm..dml..",
        "...lmd.lm.dml...",
        "....lmdlmdml....",
        ".....olmlmobb...",
        ".....bRrRrbRrb..",
        ".....brRrRbbbb..",
        ".....olmlmobb...",
        "....olmlmldobb..",
        "....lmdlmdmbRb..",
        "...lmd.lm.dbbl..",
        "..lmd..lm..dml..",
        "..ld...ld...dl..",
        "................",
        "................",
    ],
}
JUTE_BUNDLE_COLORS = {"l": "#e6cf92", "m": "#c9ab66", "d": "#97793f", "o": "#5e4524", "R": "#a07a3e", "r": "#6a4e26",
                      "b": "#3e2c16", "g": "#66a03a", "G": "#7fb64a", "k": "#436f26"}


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

# X1–X10 — тонкие связки, как солома и ванильная пшеница
STRAW_LIKE = {
    "X1": dict(width=3, ties=(0,)),
    "X2": dict(width=3, ties=(-3,), fan=(0, 8), spread=2, fray=2, seed=2),
    "X3": dict(width=2, ties=(0,), tail="r", length=(-13, 13)),
    "X4": dict(width=2, ties=(-5,), fan=(0, 11), spread=2, fray=3, seed=4, length=(-13, 13)),
    "X5": dict(width=3, ties=(-5, 4)),
    "X6": dict(width=3, ties=(0,), fan=(5, 5), spread=2, fray=2, seed=6),
    "X7": dict(width=3, ties=(-6,), fan=(0, 6), spread=1, fray=1, seed=7, length=(-12, 13)),
    "X8": dict(width=2, ties=(-5, 4), length=(-13, 13)),
    "X9": dict(width=2, ties=(-1,), fray=3, seed=9, length=(-13, 13)),
    "X10": dict(width=3, ties=(0,), tail="r", fan=(5, 5), spread=2, fray=1, seed=10),
}
JUTE_BUNDLES.update({name: straw_bundle(**params) for name, params in STRAW_LIKE.items()})
# Y1–Y10 — по X2 (веер сверху) и X10 (веер с обеих сторон и концы бечёвки)
FANNED = {
    "Y1": dict(width=2, ties=(-3,), fan=(0, 8), spread=2, fray=2, seed=2, length=(-13, 12)),
    "Y2": dict(width=3, ties=(-3,), tail="r", fan=(0, 8), spread=2, fray=2, seed=2),
    "Y3": dict(width=3, ties=(-4,), fan=(0, 10), spread=2, fray=2, seed=13),
    "Y4": dict(width=3, ties=(-3,), fan=(3, 8), spread=2, fray=2, seed=14),
    "Y5": dict(width=2, ties=(0,), tail="r", fan=(5, 5), spread=2, fray=1, seed=10, length=(-13, 13)),
    "Y6": dict(width=3, ties=(0,), fan=(6, 6), spread=2, fray=1, seed=16),
    "Y7": dict(width=3, ties=(-3,), tail="r", fan=(3, 7), spread=2, fray=1, seed=17),
    "Y8": dict(width=3, ties=(-6, -1), fan=(0, 7), spread=2, fray=2, seed=18),
    "Y9": dict(width=3, ties=(0,), tail="r", fan=(5, 5), spread=2, fray=3, seed=19),
    "Y10": dict(width=2, ties=(-2,), tail="r", fan=(4, 8), spread=2, fray=2, seed=20, length=(-13, 13)),
}
JUTE_BUNDLES.update({name: straw_bundle(**params) for name, params in FANNED.items()})

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

ROPE = [
    "................",
    "................",
    ".....lmlmlm.....",
    "...lmdddddmlm...",
    "..lmd.....dmlm..",
    "..md.......dml..",
    ".lm.........ml..",
    ".md...lml...dm..",
    ".lm..lmdml..ml..",
    ".md..md.dm..dm..",
    ".lm..lm.lm..ml..",
    "..ml..mlm..lmd..",
    "..dml.....lmd...",
    "...dmlmlmlmd....",
    ".....ddddd.lm...",
    "............md..",
]
ROPE_COLORS = {"l": "#d8bf86", "m": "#a8894e", "d": "#6e5530"}


def berry_item(kind):
    return paint(blank(), ITEM_BERRIES[kind], {k: hx(v) for k, v in ITEM_BERRY_COLORS[kind].items()})


def jute_bundle(variant):
    return paint(blank(), JUTE_BUNDLES[variant], {k: hx(v) for k, v in JUTE_BUNDLE_COLORS.items()})


def jute_bundles(chosen):
    """Лист всех вариантов пучка джута (docs/textures/jute_bundles.png), по 10 в ряд, * — в игре."""
    per_row = 10
    rows = (len(JUTE_BUNDLES) + per_row - 1) // per_row
    out = Image.new("RGBA", (per_row * 90, rows * 110), (60, 60, 60, 255))
    draw = ImageDraw.Draw(out)
    for i, variant in enumerate(JUTE_BUNDLES):
        x, y = i % per_row * 90, i // per_row * 110
        big = jute_bundle(variant).resize((80, 80), Image.NEAREST)
        out.paste(big, (x + 5, y + 5), big)
        draw.text((x + 34, y + 92), variant + (" *" if variant == chosen else ""), fill=(255, 255, 255, 255))
    out.save(os.path.join(DOCS, "jute_bundles.png"))


def items():
    for kind in ITEM_BERRIES:
        save(berry_item(kind), f"item/{kind}")
    save(jute_bundle(JUTE_CHOSEN), "item/jute")
    save(paint(blank(), JUTE_SEEDS, {k: hx(v) for k, v in JUTE_SEEDS_COLORS.items()}), "item/jute_seeds")
    save(paint(blank(), ROPE, {k: hx(v) for k, v in ROPE_COLORS.items()}), "item/rope")


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
    chosen = "D"
    if "--hoe" in sys.argv:
        chosen = sys.argv[sys.argv.index("--hoe") + 1].upper()
        if chosen not in HOE_VARIANTS:
            sys.exit(f"Нет варианта тяпки {chosen}, есть: {', '.join(HOE_VARIANTS)}")
    # пучок джута — 70 вариантов (docs/textures/jute_bundles.png), другой: --jute B
    JUTE_CHOSEN = "A"
    if "--jute" in sys.argv:
        JUTE_CHOSEN = sys.argv[sys.argv.index("--jute") + 1].upper()
        if JUTE_CHOSEN not in JUTE_BUNDLES:
            sys.exit(f"Нет варианта пучка джута {JUTE_CHOSEN}, есть: {', '.join(JUTE_BUNDLES)}")
    jute_bundles(JUTE_CHOSEN)
    hoes(chosen)
    farmlands()
    plants()
    items()
    preview()
    print("ok")

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

# Пучки джута: 10 вариантов. l/m/d — волокно (светлое, среднее, тёмное), o — контур волокна, R/r — перевязь,
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
}
JUTE_BUNDLE_COLORS = {"l": "#e6cf92", "m": "#c9ab66", "d": "#97793f", "o": "#5e4524", "R": "#a07a3e", "r": "#6a4e26",
                      "b": "#3e2c16", "g": "#66a03a", "G": "#7fb64a", "k": "#436f26"}

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
    """Лист всех вариантов пучка джута (docs/textures/jute_bundles.png), * — в игре."""
    out = Image.new("RGBA", (len(JUTE_BUNDLES) * 90, 110), (60, 60, 60, 255))
    draw = ImageDraw.Draw(out)
    for i, variant in enumerate(JUTE_BUNDLES):
        big = jute_bundle(variant).resize((80, 80), Image.NEAREST)
        out.paste(big, (i * 90 + 5, 5), big)
        draw.text((i * 90 + 38, 92), variant + (" *" if variant == chosen else ""), fill=(255, 255, 255, 255))
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
    # пучок джута — 10 вариантов (docs/textures/jute_bundles.png), другой: --jute B
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

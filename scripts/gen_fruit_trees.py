"""
Фруктовые деревья на древесине амадзу (LEAF_VARIANTS в gen_forests.py, ModWood.FRUIT_TREES): плоды (ModItems — еда),
листва с плодами, саженцы, плоды в луте листвы, переводы и превью docs/textures/fruit_trees.png.

Запуск из корня проекта:  python scripts/gen_fruit_trees.py   (нужен Pillow; ванильное яблоко — из jar клиента в кэше
Gradle). Запускать после gen_wood.py и gen_forests.py: тот копирует лут листвы с амадзу и стирает в нём плоды.

С листвы (не ножницами) падает плод (CHANCE). Одинокие фруктовые деревья растут на равнинах (PLAINS_TREES в
gen_forests.py). Текстуры: листва — листва амадзу с плодами, саженец — саженец амадзу с цветками или завязями,
яблоко — ванильное чуть краснее, остальные плоды рисуются здесь же (shape: круги, тень от света сверху слева).
"""
import colorsys
import json
import math
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(HERE, "..", "docs", "textures")
BASE = "amadzu"
CHANCE = 0.08


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


# плод: ru, en, гамма плода от тёмной к светлой (контур, тень, цвет, свет, блик), где плоды на листве (левый верхний
# пиксель), вид на листве ("big" — 2×2, "pair" — две ягоды по диагонали), цвет цветков на саженце
FRUITS = {
    "apple": ("Яблоко", "Apple", [hx(c) for c in ("6e0c10", "a8161a", "d22a24", "e8584a", "f6a090")],
              [(3, 3), (10, 1), (6, 10), (12, 9)], "big", hx("f4dce0")),
    "pear": ("Груша", "Pear", [hx(c) for c in ("5a5414", "8c8424", "bcb43a", "dcd668", "f2eeaa")],
             [(2, 5), (9, 2), (5, 11), (12, 8)], "big", hx("f6f4ec")),
    "plum": ("Слива", "Plum", [hx(c) for c in ("2a1036", "4a2060", "6c3486", "9256aa", "c49cd6")],
             [(4, 2), (11, 4), (2, 10), (9, 11)], "big", hx("f6f4ec")),
    "cherry": ("Вишня", "Cherry", [hx(c) for c in ("4a0610", "7c0c1a", "a81424", "d2343c", "f08a8a")],
               [(3, 2), (10, 3), (5, 9), (12, 11), (8, 6)], "pair", hx("f8d4dc")),
    "apricot": ("Абрикос", "Apricot", [hx(c) for c in ("8a3c10", "c0601c", "e88c34", "f6b25a", "fde0a0")],
                [(3, 4), (10, 2), (6, 11), (12, 8)], "big", hx("f8dcd4")),
}
LEAF = "#"  # пиксели плода на листве
STEM, STEM_DARK = hx("6a4628"), hx("3e2818")
LEAF_GREEN, LEAF_DARK = hx("4e8a30"), hx("2e5a1c")


def path(rel):
    return os.path.join(ROOT, *rel.split("/"))


def read(rel, default=None):
    try:
        with open(path(rel), encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return default


def write(rel, data):
    p = path(rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hsv_map(img, fn):
    out = img.convert("RGBA").copy()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = out.getpixel((x, y))
            if a:
                h, s, v = fn(*colorsys.rgb_to_hsv(r / 255, g / 255, b / 255))
                r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


# ---------------------------------------------------------------- плоды-предметы

def blob(circles):
    """Пиксели фигуры из кругов (cx, cy, r) на сетке 16×16 (центр пикселя — +0,5)."""
    return {(x, y) for x in range(16) for y in range(16)
            if any((x + 0.5 - cx) ** 2 + (y + 0.5 - cy) ** 2 <= r * r for cx, cy, r in circles)}


def shade(img, pixels, colors, light):
    """Заливка фигуры: контур — самый тёмный, внутри — светлее к точке света light, у неё блик."""
    lx, ly = light
    far = max(math.hypot(x + 0.5 - lx, y + 0.5 - ly) for x, y in pixels)
    for x, y in pixels:
        edge = any(q not in pixels for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
        if edge:
            c = colors[0]
        else:
            t = 1 - math.hypot(x + 0.5 - lx, y + 0.5 - ly) / far
            c = colors[1] if t < 0.3 else colors[2] if t < 0.62 else colors[3] if t < 0.86 else colors[4]
        img.putpixel((x, y), c + (255,))


def stem(img, points, leaf=None):
    for i, p in enumerate(points):
        img.putpixel(p, (STEM if i % 2 == 0 else STEM_DARK) + (255,))
    for x, y in leaf or []:
        img.putpixel((x, y), (LEAF_GREEN if (x + y) % 2 == 0 else LEAF_DARK) + (255,))


def pear(colors):
    img = Image.new("RGBA", (16, 16))
    shade(img, blob([(8, 6.5, 2.4), (8, 11, 4.2), (8, 8.6, 3.0)]), colors, (6.2, 8.5))
    stem(img, [(8, 3), (8, 2), (9, 1)], [(9, 2), (10, 2), (10, 1), (11, 1)])
    return img


def plum(colors):
    img = Image.new("RGBA", (16, 16))
    pixels = blob([(8, 9, 4.9), (7.5, 8.6, 4.7)])
    shade(img, pixels, colors, (6, 7))
    for y in range(6, 13):  # бороздка
        if (9, y) in pixels and (10, y) in pixels:
            img.putpixel((9, y), colors[1] + (255,))
    stem(img, [(8, 4), (8, 3)], [(9, 3), (10, 3), (10, 2)])
    return img


def cherry(colors):
    img = Image.new("RGBA", (16, 16))
    left, right = blob([(5, 11.5, 2.8)]), blob([(11, 10.5, 2.8)])
    shade(img, left, colors, (4.2, 10.6))
    shade(img, right, colors, (10.2, 9.6))
    # черешки сходятся вверху
    stem(img, [(5, 8), (6, 7), (6, 6), (7, 5), (8, 4), (9, 3)], None)
    stem(img, [(11, 7), (11, 6), (10, 5), (10, 4), (9, 3)], [(10, 2), (11, 2), (11, 1), (12, 1)])
    return img


def apricot(colors):
    img = Image.new("RGBA", (16, 16))
    pixels = blob([(8, 9.5, 5.0)])
    shade(img, pixels, colors, (6.2, 7.6))
    blush = hx("d4542c")
    for x, y in pixels:  # румянец справа снизу
        if (x - 9) ** 2 + (y - 11) ** 2 <= 5 and img.getpixel((x, y))[:3] in (colors[2], colors[1]):
            img.putpixel((x, y), blush + (255,))
    for y in range(10, 14):  # бороздка снизу
        if (8, y) in pixels and (7, y) in pixels and (9, y) in pixels:
            img.putpixel((8, y), colors[1] + (255,))
    stem(img, [(8, 4)], [(9, 4), (9, 3), (10, 3)])
    return img


DRAW = {"pear": pear, "plum": plum, "cherry": cherry, "apricot": apricot}


# ---------------------------------------------------------------- листва и саженцы

def fruit_leaves(leaves, colors, spots, kind):
    img = leaves.copy()
    for x, y in spots:
        if kind == "pair":  # две ягоды по диагонали
            for dx, dy in ((0, 0), (1, 1)):
                img.putpixel((x + dx, y + dy), colors[2] + (255,))
            img.putpixel((x, y), colors[3] + (255,))
            img.putpixel((x + 1, y), STEM_DARK + (255,))
        else:
            for dx, dy, c in ((0, 0, colors[3]), (1, 0, colors[2]), (0, 1, colors[2]), (1, 1, colors[1])):
                img.putpixel((x + dx, y + dy), c + (255,))
            img.putpixel((x, y), colors[4] + (255,))  # блик
    return img


def fruit_sapling(sapling, flower, n):
    """Саженец амадзу чуть светлее, на кончиках листьев — цветки (по n-й паре зелёных пикселей сверху)."""
    img = hsv_map(sapling, lambda h, s, v: (h, s * 0.92, min(1, v * 1.08)))
    green = sorted(((x, y) for y in range(16) for x in range(16)
                    if img.getpixel((x, y))[3] and img.getpixel((x, y))[1] > img.getpixel((x, y))[0] + 20),
                   key=lambda p: (p[1], p[0]))
    if green:
        for k in (n, len(green) // 2 + n * 3):
            img.putpixel(green[k % len(green)], flower + (255,))
    return img


def loot(fruit):
    rel = f"data/{NS}/loot_table/blocks/{fruit}_leaves.json"
    table = read(rel)
    table["pools"] = [p for p in table["pools"] if f'"{NS}:{fruit}"' not in json.dumps(p)]
    table["pools"].append({"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{fruit}"}],
                           "conditions": [{"condition": "minecraft:inverted", "term": {
                               "condition": "minecraft:match_tool", "predicate": {"items": "minecraft:shears"}}},
                                          {"condition": "minecraft:random_chance", "chance": CHANCE}]})
    write(rel, table)


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for fruit, spec in FRUITS.items():
            data[f"item.{NS}.{fruit}"] = spec[k]
        write(rel, data)


def preview(rows):
    scale, pad = 6, 6
    width = max(len(r) for r in rows)
    sheet = Image.new("RGBA", (pad + width * (16 * scale + pad), pad + len(rows) * (16 * scale + pad)), (60, 60, 60, 255))
    for j, row in enumerate(rows):
        for i, img in enumerate(row):
            sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST),
                                  (pad + i * (16 * scale + pad), pad + j * (16 * scale + pad)))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "fruit_trees.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    vanilla_apple = Image.open(BytesIO(client.read("assets/minecraft/textures/item/apple.png")))
    leaves = Image.open(os.path.join(TEX, "block", f"{BASE}_leaves.png")).convert("RGBA")
    sapling = Image.open(os.path.join(TEX, "block", f"{BASE}_sapling.png")).convert("RGBA")
    items, leaf_images, saplings = [], [], []
    for n, (fruit, (ru, en, colors, spots, kind, flower)) in enumerate(FRUITS.items()):
        if fruit == "apple":
            item = hsv_map(vanilla_apple, lambda h, s, v: (h - 0.01 if h > 0.8 or h < 0.1 else h, min(1, s * 1.1), v))
        else:
            item = DRAW[fruit](colors)
        item.save(os.path.join(TEX, "item", f"{fruit}.png"))
        write(f"assets/{NS}/models/item/{fruit}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{fruit}"}})
        leaf = fruit_leaves(leaves, colors, spots, kind)
        leaf.save(os.path.join(TEX, "block", f"{fruit}_leaves.png"))
        sap = fruit_sapling(sapling, flower, n)
        sap.save(os.path.join(TEX, "block", f"{fruit}_sapling.png"))
        loot(fruit)
        items.append(item)
        leaf_images.append(leaf)
        saplings.append(sap)
    lang()
    preview([items, leaf_images, saplings])
    print("ok")

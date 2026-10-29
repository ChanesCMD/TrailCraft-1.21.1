"""
Фруктовые деревья на древесине амадзу (LEAF_VARIANTS в gen_forests.py, ModWood.FRUIT_TREES): плоды-предметы (ModItems —
еда), плоды на дереве (блок <плод>_fruit, block/FruitBlock: блокстейт, модели стадий, текстура, лут), листва, саженцы,
переводы и превью docs/textures/fruit_trees.png.

Запуск из корня проекта:  python scripts/gen_fruit_trees.py   (нужен Pillow; ванильное яблоко — из jar клиента в кэше
Gradle). Запускать после gen_wood.py и gen_forests.py: тот копирует лут листвы с амадзу.

Деревья растут без плодов; природная листва завязывает под собой плод (block/FruitLeavesBlock), он растёт от цветка
до спелого (4 стадии, модели — кубики на черенке: SHAPES), спелый собирается ПКМ. Одинокие фруктовые деревья растут
на равнинах (PLAINS_TREES в gen_forests.py). Текстуры: листва — листва амадзу чуть другого оттенка (LEAF_TINT), саженец —
саженец амадзу с цветками, плод на дереве — развёртка 16×16 (спелая, полуспелая и зелёная кожица, цветок, черенок),
яблоко-предмет — ванильное чуть краснее, остальные плоды рисуются здесь же (круги, тень от света сверху слева).
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


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


# плод: ru, en, гамма плода от тёмной к светлой (контур, тень, цвет, свет, блик), форма на дереве (SHAPES),
# сколько плодов со спелого (как count у FruitBlock в ModWood), цвет цветков (на саженце и на дереве),
# оттенок листвы (сдвиг тона, насыщенность ×, яркость ×), имя плода на дереве ru, en
FRUITS = {
    "apple": ("Яблоко", "Apple", [hx(c) for c in ("6e0c10", "a8161a", "d22a24", "e8584a", "f6a090")], "round", 1,
              hx("f4dce0"), (0, 1, 1), "Яблоко на ветке", "Apple on Branch"),
    "pear": ("Груша", "Pear", [hx(c) for c in ("5a5414", "8c8424", "bcb43a", "dcd668", "f2eeaa")], "pear", 1,
             hx("f6f4ec"), (0.01, 0.95, 1.04), "Груша на ветке", "Pear on Branch"),
    "plum": ("Слива", "Plum", [hx(c) for c in ("2a1036", "4a2060", "6c3486", "9256aa", "c49cd6")], "round", 1,
             hx("f6f4ec"), (-0.01, 1, 0.94), "Слива на ветке", "Plum on Branch"),
    "cherry": ("Вишня", "Cherry", [hx(c) for c in ("4a0610", "7c0c1a", "a81424", "d2343c", "f08a8a")], "cherry", 2,
               hx("f8d4dc"), (-0.005, 1.05, 0.97), "Вишня на ветке", "Cherries on Branch"),
    "apricot": ("Абрикос", "Apricot", [hx(c) for c in ("8a3c10", "c0601c", "e88c34", "f6b25a", "fde0a0")], "round", 1,
                hx("f8dcd4"), (0.015, 0.95, 1.03), "Абрикос на ветке", "Apricot on Branch"),
}
STEM, STEM_DARK = hx("6a4628"), hx("3e2818")
LEAF_GREEN, LEAF_DARK = hx("4e8a30"), hx("2e5a1c")
UNRIPE = [hx(c) for c in ("2e4a14", "4a6e20", "6a9430", "8cb44a", "b4d47a")]
FLOWER_CENTER = hx("e8c43c")


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

def fruit_leaves(leaves, tint):
    """Листва амадзу чуть другого оттенка — чтобы деревья различались и без плодов."""
    dh, ks, kv = tint
    return hsv_map(leaves, lambda h, s, v: (h + dh, s * ks, min(1, v * kv)))


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


# ---------------------------------------------------------------- плод на дереве

def skin(colors, size=8):
    """Кожица size×size: светлее к левому верхнему углу, у угла блик, по краю снизу и справа темнее."""
    img = Image.new("RGBA", (size, size))
    for y in range(size):
        for x in range(size):
            t = (x + y) / (2 * size - 2)
            c = colors[3] if t < 0.25 else colors[2] if t < 0.6 else colors[1]
            if x == size - 1 or y == size - 1:
                c = colors[1] if t < 0.6 else colors[0]
            img.putpixel((x, y), c + (255,))
    img.putpixel((1, 1), colors[4] + (255,))
    return img


def fruit_texture(colors, flower):
    """Развёртка плода на дереве 16×16: спелая кожица (0, 0), полуспелая (8, 0), зелёная (0, 8), цветок 4×4 (8, 8),
    черенок — столбики (12, 8) и (13, 8)."""
    img = Image.new("RGBA", (16, 16))
    half = [tuple((a + b) // 2 for a, b in zip(c, u)) for c, u in zip(colors, UNRIPE)]
    img.alpha_composite(skin(colors), (0, 0))
    img.alpha_composite(skin(half), (8, 0))
    img.alpha_composite(skin(UNRIPE), (0, 8))
    for y in range(8, 12):
        for x in range(8, 12):
            img.putpixel((x, y), (FLOWER_CENTER if (x, y) in ((9, 9), (10, 10)) else flower) + (255,))
    for y in range(8, 16):
        img.putpixel((12, y), STEM + (255,))
        img.putpixel((13, y), STEM_DARK + (255,))
    for y in range(8, 16):
        for x in (14, 15):
            img.putpixel((x, y), (LEAF_GREEN if (x + y) % 2 else LEAF_DARK) + (255,))
    return img


# Области развёртки: спелая, полуспелая, зелёная кожица, цветок, черенок
UV = {"ripe": (0, 0), "half": (8, 0), "green": (0, 8), "flower": (8, 8), "stem": (12, 8)}


def cube(frm, to, region):
    """Кубик модели: все грани берут кусок развёртки нужного размера от угла области."""
    u, v = UV[region]
    w, h, d = (to[i] - frm[i] for i in range(3))
    size = {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}
    return {"from": frm, "to": to, "faces": {
        face: {"uv": [u, v, u + fw, v + fh], "texture": "#fruit"} for face, (fw, fh) in size.items()}}


def stalk(x, z, bottom):
    return cube([x, bottom, z], [x + 1, 16, z + 1], "stem")


def blossom():
    return [cube([6.5, 13, 6.5], [9.5, 14, 9.5], "flower"), stalk(7.5, 7.5, 14)]


# Формы плодов на дереве по стадиям: 0 — цветок, 1 — завязь, 2 — полуспелый, 3 — спелый (висят от верха блока)
SHAPES = {
    "round": [blossom(),
              [cube([7, 11, 7], [9, 13, 9], "green"), stalk(7.5, 7.5, 13)],
              [cube([6, 8, 6], [10, 12, 10], "half"), stalk(7.5, 7.5, 12)],
              [cube([5, 5, 5], [11, 11, 11], "ripe"), stalk(7.5, 7.5, 11)]],
    "pear": [blossom(),
             [cube([7, 10, 7], [9, 13, 9], "green"), stalk(7.5, 7.5, 13)],
             [cube([6, 7, 6], [10, 10, 10], "half"), cube([7, 10, 7], [9, 12, 9], "half"), stalk(7.5, 7.5, 12)],
             [cube([5, 4, 5], [11, 9, 11], "ripe"), cube([6, 9, 6], [10, 12, 10], "ripe"), stalk(7.5, 7.5, 12)]],
    "cherry": [blossom(),
               [cube([5, 11, 6], [6, 12, 7], "green"), stalk(5, 6, 12), cube([10, 10, 9], [11, 11, 10], "green"),
                stalk(10, 9, 11)],
               [cube([4.5, 8, 5.5], [6.5, 10, 7.5], "half"), stalk(5, 6, 10), cube([9.5, 7, 8.5], [11.5, 9, 10.5], "half"),
                stalk(10, 9, 9)],
               [cube([4, 6, 5], [7, 9, 8], "ripe"), stalk(5, 6, 9), cube([9, 5, 8], [12, 8, 11], "ripe"), stalk(10, 9, 8)]],
}


def fruit_block(fruit, shape, count):
    block = f"{fruit}_fruit"
    for age, elements in enumerate(SHAPES[shape]):
        write(f"assets/{NS}/models/block/{block}_stage{age}.json", {
            "ambientocclusion": False, "render_type": "minecraft:cutout",
            "textures": {"fruit": f"{NS}:block/{block}", "particle": f"{NS}:block/{block}"},
            "elements": elements})
    write(f"assets/{NS}/blockstates/{block}.json", {"variants": {
        f"age={age}": {"model": f"{NS}:block/{block}_stage{age}"} for age in range(4)}})
    entry = {"type": "minecraft:item", "name": f"{NS}:{fruit}"}
    if count > 1:
        entry["functions"] = [{"function": "minecraft:set_count", "count": count}]
    write(f"data/{NS}/loot_table/blocks/{block}.json", {
        "type": "minecraft:block", "random_sequence": f"{NS}:blocks/{block}",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [entry], "conditions": [{
            "condition": "minecraft:block_state_property", "block": f"{NS}:{block}", "properties": {"age": "3"}}]}]})


def loot(fruit):
    """С листвы плоды не падают (растут отдельным блоком): убрать пул плода, если остался."""
    rel = f"data/{NS}/loot_table/blocks/{fruit}_leaves.json"
    table = read(rel)
    table["pools"] = [p for p in table["pools"] if f'"{NS}:{fruit}"' not in json.dumps(p)]
    write(rel, table)


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for fruit, spec in FRUITS.items():
            data[f"item.{NS}.{fruit}"] = spec[k]
            data[f"block.{NS}.{fruit}_fruit"] = spec[7 + k]
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
    items, leaf_images, saplings, fruit_images = [], [], [], []
    for n, (fruit, (ru, en, colors, shape, count, flower, tint, *_)) in enumerate(FRUITS.items()):
        if fruit == "apple":
            item = hsv_map(vanilla_apple, lambda h, s, v: (h - 0.01 if h > 0.8 or h < 0.1 else h, min(1, s * 1.1), v))
        else:
            item = DRAW[fruit](colors)
        item.save(os.path.join(TEX, "item", f"{fruit}.png"))
        write(f"assets/{NS}/models/item/{fruit}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{fruit}"}})
        leaf = fruit_leaves(leaves, tint)
        leaf.save(os.path.join(TEX, "block", f"{fruit}_leaves.png"))
        tex = fruit_texture(colors, flower)
        tex.save(os.path.join(TEX, "block", f"{fruit}_fruit.png"))
        fruit_block(fruit, shape, count)
        sap = fruit_sapling(sapling, flower, n)
        sap.save(os.path.join(TEX, "block", f"{fruit}_sapling.png"))
        loot(fruit)
        items.append(item)
        leaf_images.append(leaf)
        saplings.append(sap)
        fruit_images.append(tex)
    lang()
    preview([items, leaf_images, saplings, fruit_images])
    print("ok")

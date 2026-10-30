"""
Новая порода древесины по образцу другой (WOODS: порода -> донор, палитра): копии всех JSON донора с заменой имени
(блокстейты, модели, лут, рецепты, тэг <порода>_logs, деревья <порода>_tree / _wild_tree и их placed_feature), её id в тех
же тэгах, где донор, переводы и текстуры — текстуры дуба, перекрашенные градиентом по яркости в палитру породы (кора,
древесина, саженец — в цвете листвы). Превью — docs/textures/<порода>.png. Текстуру листвы пишет gen_leaves.py.

Сейчас одна порода — амадзу (по мотивам древесины цуйхуа из Genshin Impact): тёмная бордовая древесина, невысокое
дерево с широкой кроной-шапкой (форма orchard в worldgen/TrailTreeFeature); на ней растут фруктовые деревья
(LEAF_VARIANTS в gen_forests.py, плоды — gen_fruit_trees.py). Сама амадзу в мире не растёт, только из саженца.

Верстак, малая доска, сундук и тэг тонких брёвен пишут gen_workbench.py, gen_storage.py, gen_ores.py (порода — в их
WOODS); блоки — init/ModWood.java. Порядок: этот скрипт, gen_workbench.py, gen_storage.py, gen_ores.py, gen_forests.py,
gen_fruit_trees.py.

Запуск из корня проекта:  python scripts/gen_wood.py   (нужен Pillow)
"""
import colorsys
import json
import os

from PIL import Image

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(HERE, "..", "docs", "textures")

# порода -> (донор JSON, имя по-русски в родительном падеже, по-английски, форма дерева,
#            кора от тёмной к светлой, древесина от тёмной к светлой, листва саженца от тёмной к светлой)
WOODS = {
    "amadzu": ("alder", "амадзу", "Amadzu", "orchard",
               [(42, 24, 26), (76, 46, 46), (118, 80, 74)],
               [(90, 36, 34), (142, 70, 60), (194, 112, 92)],
               [(26, 66, 24), (62, 120, 40), (124, 178, 70)]),
}
# Блоки породы — то, что копируется (верстак, малая доска и сундук — другие скрипты)
PARTS = ["{w}_log", "thin_{w}_log", "stripped_{w}_log", "{w}_wood", "stripped_{w}_wood", "{w}_leaves", "{w}_sapling",
         "{w}_planks", "{w}_stairs", "{w}_slab", "{w}_fence", "{w}_fence_gate", "{w}_door", "{w}_trapdoor", "{w}_button",
         "{w}_pressure_plate"]


def path(rel):
    return os.path.join(ROOT, *rel.split("/"))


def read(rel):
    with open(path(rel), encoding="utf-8") as f:
        return json.load(f)


def write(rel, data):
    p = path(rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def own(name, donor):
    """Файл донора, который копируется: имя — один из блоков породы (или его модель / рецепт)."""
    stems = [p.format(w=donor) for p in PARTS] + [f"{donor}_logs", f"{donor}_tree", f"{donor}_wild_tree",
                                                    f"{donor}_checked", f"{donor}_wild_checked"]
    base = name[:-len(".json")]
    return any(base == s or base.startswith(s + "_") for s in stems) and not any(
        k in base for k in ("chest", "crafting_table", "small_board"))


def clone(wood, donor, shape):
    folders = [f"assets/{NS}/blockstates", f"assets/{NS}/models/block", f"assets/{NS}/models/item",
               f"data/{NS}/loot_table/blocks", f"data/{NS}/recipe", f"data/{NS}/tags/item",
               f"data/{NS}/worldgen/configured_feature", f"data/{NS}/worldgen/placed_feature"]
    for folder in folders:
        for name in sorted(os.listdir(path(folder))):
            if not name.endswith(".json") or not own(name, donor):
                continue
            text = json.dumps(read(f"{folder}/{name}"), ensure_ascii=False).replace(donor, wood)
            data = json.loads(text)
            if folder.endswith("configured_feature"):
                data["config"]["shape"] = shape
            write(f"{folder}/{name.replace(donor, wood)}", data)


def tags(wood, donor):
    """Id породы — во все тэги блоков и предметов, где есть такие же блоки донора (кроме биомов и т. п.)."""
    ids = {f"{NS}:{p.format(w=donor)}": f"{NS}:{p.format(w=wood)}" for p in PARTS}
    for ns in sorted(os.listdir(path("data"))):
        for kind in ("block", "item"):
            folder = path(f"data/{ns}/tags/{kind}")
            if not os.path.isdir(folder):
                continue
            for dirpath, _, files in sorted(os.walk(folder)):
                for name in sorted(files):
                    if donor in name:  # свой тэг донора (<донор>_logs) не трогаем
                        continue
                    rel = os.path.relpath(os.path.join(dirpath, name), path("")).replace(os.sep, "/")
                    data = read(rel)
                    values = data.get("values", [])
                    add = [ids[v] for v in values if isinstance(v, str) and v in ids and ids[v] not in values]
                    if add:
                        data["values"] = values + add
                        write(rel, data)


def lang(wood, donor, ru, en):
    """Переводы блоков породы — как у донора с заменой имени (у ольхи — «… ольхи», "Alder …")."""
    for code, name in (("ru_ru", ru), ("en_us", en)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel)
        donor_name = data[f"block.{NS}.{donor}_log"].split()[-1] if code == "ru_ru" else donor.capitalize()
        keys = {f"block.{NS}.{p.format(w=wood)}" for p in PARTS}
        out = {}
        for k, v in data.items():
            if k in keys:
                continue
            out[k] = v
            if k == f"block.{NS}.{donor}_pressure_plate":
                for p in PARTS:
                    out[f"block.{NS}.{p.format(w=wood)}"] = data[f"block.{NS}.{p.format(w=donor)}"].replace(donor_name, name)
        write(rel, out)


# ---------------------------------------------------------------- текстуры

def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def ramp(stops, t):
    t = max(0.0, min(1.0, t)) * (len(stops) - 1)
    i = min(int(t), len(stops) - 2)
    f = t - i
    return tuple(round(stops[i][k] + (stops[i + 1][k] - stops[i][k]) * f) for k in range(3))


def gradient(img, stops, keep_gray=False, mask=None):
    """Перекраска по яркости: самый тёмный пиксель — первый цвет палитры, самый светлый — последний; keep_gray оставляет
    серые пиксели (железо на двери), mask — какие пиксели перекрашивать."""
    img = img.convert("RGBA")
    px = [(x, y) for y in range(img.height) for x in range(img.width)
          if img.getpixel((x, y))[3] and (mask is None or mask(img.getpixel((x, y))))]
    if not px:
        return img
    ls = [lum(img.getpixel(p)) for p in px]
    lo, hi = min(ls), max(ls)
    out = img.copy()
    for p in px:
        c = img.getpixel(p)
        if keep_gray and colorsys.rgb_to_hsv(*(k / 255 for k in c[:3]))[1] < 0.12:
            continue
        out.putpixel(p, ramp(stops, (lum(c) - lo) / (hi - lo or 1)) + (c[3],))
    return out


def green(c):
    h, s, v = colorsys.rgb_to_hsv(*(k / 255 for k in c[:3]))
    return 0.15 < h < 0.5 and s > 0.15


def textures(wood, bark, timber, leaves):
    """Текстуры дуба в палитре породы: {путь без .png: картинка}."""
    load = lambda n: Image.open(os.path.join(TEX, n + ".png")).convert("RGBA")
    both = [bark[0], bark[1], timber[0], timber[1], timber[2]]  # торец: кора по краю, древесина внутри
    sapling = gradient(load("block/oak_sapling"), bark, mask=lambda c: not green(c))
    return {
        f"block/{wood}_log": gradient(load("block/oak_log"), bark),
        f"block/{wood}_log_top": gradient(load("block/oak_log_top"), both),
        f"block/stripped_{wood}_log": gradient(load("block/stripped_oak_log"), timber),
        f"block/stripped_{wood}_log_top": gradient(load("block/stripped_oak_log_top"), timber),
        f"block/thin_{wood}_log_top": gradient(load("block/thin_oak_log_top"), both),
        f"block/{wood}_planks": gradient(load("block/oak_planks"), timber),
        f"block/{wood}_door_top": gradient(load("block/oak_door_top"), timber, keep_gray=True),
        f"block/{wood}_door_bottom": gradient(load("block/oak_door_bottom"), timber, keep_gray=True),
        f"item/{wood}_door": gradient(load("item/oak_door"), timber, keep_gray=True),
        f"block/{wood}_trapdoor": gradient(load("block/oak_trapdoor"), timber, keep_gray=True),
        f"block/{wood}_sapling": gradient(sapling, leaves, mask=green),
    }


def preview(wood, images):
    scale, pad = 6, 6
    sheet = Image.new("RGBA", (pad + len(images) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, f"{wood}.png"))


if __name__ == "__main__":
    for wood, (donor, ru, en, shape, bark, timber, leaves) in WOODS.items():
        clone(wood, donor, shape)
        tags(wood, donor)
        lang(wood, donor, ru, en)
        images = textures(wood, bark, timber, leaves)
        for rel, img in images.items():
            img.save(os.path.join(TEX, *rel.split("/")) + ".png")
        preview(wood, list(images.values()))
    print("ok")

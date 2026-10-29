"""
Хранилища: бочка с водой (block/WaterBarrelBlock.java) и сундуки по породам древесины (init/ModChests.java) — текстуры,
модели, блокстейты, лут, рецепты, тэги, переводы и превью docs/textures/storage.png.

Запуск из корня проекта:  python scripts/gen_storage.py   (нужен Pillow; ванильные текстуры — из jar клиента в кэше
Gradle, как в gen_create_compat.py)

Сундук — ванильный (одиночный и половины двойного, textures/entity/chest/normal*.png): у дерева средний цвет — как у
досок породы (textures/block/<порода>_planks.png; каналы умножаются, фактура остаётся), металл замка как был. Рецепт — 8 досок своей породы.
Бочка — ванильная бочка, закрытая, в цветах досок дуба: 6 досок и 2 малые доски любой породы; помнит воду, если её
сломать (copy_components). Бочка — в c:barrels/wooden (жидкостный бак и хранилище Create), сундуки — в c:chests/wooden.
"""
import colorsys
import json
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
# породы с досками (варианты листвы — красный клён, яблоня — без своих досок и сундуков), как ModWood.SETS
WOODS = ["oak", "birch", "acacia", "sakura", "eucalyptus", "chestnut", "thuja", "pine", "fir", "spruce", "white_poplar",
         "ebony", "maple", "cedar", "willow", "lilac", "palm", "ironwood", "aspen", "linden", "pussy_willow", "walnut",
         "alder", "amadzu"]
PARTS = {"": "normal", "_left": "normal_left", "_right": "normal_right"}

LANG = {
    "block.trailcraft.water_barrel": ("Бочка", "Barrel"),
    "block.trailcraft.water_barrel.amount": ("Вода: %s / %s mB (%s)", "Water: %s / %s mB (%s)"),
    "block.trailcraft.water_barrel.fresh": ("пресная", "fresh"),
    "block.trailcraft.water_barrel.salty": ("морская", "salty"),
    "block.trailcraft.water_barrel.desc": ("Ведро — налить или набрать, фляга — наполнить, пустой рукой — попить, Shift — "
                                           "сколько воды; под дождём набирается сама",
                                           "Bucket to pour or take, flask to fill, empty hand to drink, Shift to check; "
                                           "fills up in the rain"),
}


def path(rel):
    return os.path.join(ROOT, *rel.split("/"))


def read(rel, default):
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


def tag(kind, tag_id, values):
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write(rel, data)


def is_wood(c):
    h, s, v = colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)
    return 0.02 < h < 0.17 and s > 0.25


def wood_color(wood):
    """Средний цвет досок породы."""
    planks = Image.open(os.path.join(TEX, "block", f"{wood}_planks.png")).convert("RGBA")
    px = [c[:3] for c in planks.get_flattened_data() if c[3]]
    return tuple(sum(c[i] for c in px) / len(px) for i in range(3))


def hsv(c):
    return colorsys.rgb_to_hsv(c[0] / 255, c[1] / 255, c[2] / 255)


def repaint(img, color):
    """Дерево (коричневое) — тот же рисунок, но в цвете досок породы: оттенок досок, насыщенность умножена, яркость
    сдвинута так, чтобы средние совпали с досками (разброс, а с ним фактура, остаётся); замок и нутро — как были."""
    img = img.convert("RGBA")
    wood = [hsv(c) for c in img.get_flattened_data() if c[3] and is_wood(c)]
    mean_s, mean_v = sum(w[1] for w in wood) / len(wood), sum(w[2] for w in wood) / len(wood)
    th, ts, tv = hsv(color)
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3] and is_wood(c):
                _, s_, v = hsv(c)
                # яркость — сдвигом (у светлых пород умножение упиралось в белое и стирало рисунок)
                r, g, b = colorsys.hsv_to_rgb(th, min(1, s_ * ts / mean_s), max(0, min(1, v + tv - mean_v)))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), c[3]))
    return out


def chests(client):
    previews = []
    for wood in WOODS:
        color = wood_color(wood)
        for suffix, vanilla in PARTS.items():
            src = Image.open(BytesIO(client.read(f"assets/minecraft/textures/entity/chest/{vanilla}.png")))
            img = repaint(src, color)
            os.makedirs(os.path.join(TEX, "entity", "chest"), exist_ok=True)
            img.save(os.path.join(TEX, "entity", "chest", f"{wood}{suffix}.png"))
            if not suffix:
                previews.append(img.crop((0, 0, 64, 64)))
        name = f"{wood}_chest"
        particle = f"{NS}:block/{wood}_planks"
        write(f"assets/{NS}/models/block/{name}.json", {"textures": {"particle": particle}})
        write(f"assets/{NS}/blockstates/{name}.json", {"variants": {"": {"model": f"{NS}:block/{name}"}}})
        write(f"assets/{NS}/models/item/{name}.json", {"parent": "minecraft:item/chest", "textures": {"particle": particle}})
        write(f"data/{NS}/loot_table/blocks/{name}.json", {
            "type": "minecraft:block", "random_sequence": f"{NS}:blocks/{name}",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{name}",
                       "functions": [{"function": "minecraft:copy_name", "source": "block_entity"}]}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        write(f"data/{NS}/recipe/chests/{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "misc", "key": {"P": {"item": f"{NS}:{wood}_planks"}},
            "pattern": ["PPP", "P P", "PPP"], "result": {"id": f"{NS}:{name}", "count": 1}})
    names = [f"{NS}:{w}_chest" for w in WOODS]
    for kind in ("block", "item"):
        tag(kind, "c:chests/wooden", names)
        tag(kind, "c:chests", names)
    tag("block", "minecraft:mineable/axe", names)
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for w in WOODS:
            planks = data[f"block.{NS}.{w}_planks"]
            data[f"block.{NS}.{w}_chest"] = ("Сундук из " + planks.split(" ", 1)[1]) if k == 0 \
                else planks.replace("Planks", "Chest")
        write(rel, data)
    return previews


def barrel(client):
    color = wood_color("oak")
    images = []
    for part in ("top", "side", "bottom"):
        src = Image.open(BytesIO(client.read(f"assets/minecraft/textures/block/barrel_{part}.png")))
        img = repaint(src, color)
        img.save(os.path.join(TEX, "block", f"water_barrel_{part}.png"))
        images.append(img)
    write(f"assets/{NS}/models/block/water_barrel.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": f"{NS}:block/water_barrel_top", "side": f"{NS}:block/water_barrel_side",
        "bottom": f"{NS}:block/water_barrel_bottom"}})
    write(f"assets/{NS}/blockstates/water_barrel.json", {"variants": {"": {"model": f"{NS}:block/water_barrel"}}})
    write(f"assets/{NS}/models/item/water_barrel.json", {"parent": f"{NS}:block/water_barrel"})
    write(f"data/{NS}/loot_table/blocks/water_barrel.json", {
        "type": "minecraft:block", "random_sequence": f"{NS}:blocks/water_barrel",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:water_barrel",
                   "functions": [{"function": "minecraft:copy_components", "source": "block_entity",
                                  "include": [f"{NS}:water", f"{NS}:salty"]}]}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(f"data/{NS}/recipe/water_barrel.json", {
        "type": "minecraft:crafting_shaped", "category": "misc",
        "key": {"P": {"tag": "minecraft:planks"}, "B": {"tag": f"{NS}:small_boards"}},
        "pattern": ["PBP", "P P", "PBP"], "result": {"id": f"{NS}:water_barrel", "count": 1}})
    tag("block", "minecraft:mineable/axe", [f"{NS}:water_barrel"])
    return images


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


def preview(chest_images, barrel_images):
    scale, pad, per_row = 2, 6, 8
    cell = 64 * scale
    rows = (len(chest_images) + per_row - 1) // per_row + 1
    sheet = Image.new("RGBA", (pad + per_row * (cell + pad), pad + rows * (cell + pad)), (60, 60, 60, 255))
    for i, img in enumerate(chest_images):
        sheet.alpha_composite(img.resize((cell, cell), Image.NEAREST),
                              (pad + i % per_row * (cell + pad), pad + i // per_row * (cell + pad)))
    for i, img in enumerate(barrel_images):
        sheet.alpha_composite(img.resize((cell // 2, cell // 2), Image.NEAREST),
                              (pad + i * (cell // 2 + pad), pad + (rows - 1) * (cell + pad)))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "storage.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    chest_images = chests(client)
    barrel_images = barrel(client)
    lang()
    preview(chest_images, barrel_images)
    print("ok")

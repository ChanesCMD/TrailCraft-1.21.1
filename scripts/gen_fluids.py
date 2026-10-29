"""
Креозот и танин, пропитанные палки, бумага — текстуры, модели, блокстейты, рецепты Create и переводы; превью —
docs/textures/fluids.png. Сами жидкости, их блоки и вёдра — init/ModFluids.java (цвет жидкости там же, ARGB,
им красится ванильная текстура воды), дубление в бочке — block/entity/WaterBarrelBlockEntity.java.

Цепочки:
- древесный уголь в жёрнове — древесноугольная пыль; смеситель с подогревом: 1000 mB воды, 4 смолы и 10 пыли —
  1000 mB креозота; наливатель: обработанная палка и 100 mB креозота — пропитанная палка (из неё лук);
- смеситель: 1000 mB воды и кора — 1000 mB танина; выскобленные шкуры дубятся в бочке с танином (как в TFC);
- кора в жёрнове — опилки; смеситель: вода и опилки или 2 сахарного тростника — заготовка бумаги; пресс — бумага.

Запуск из корня проекта:  python scripts/gen_fluids.py   (нужен Pillow; ванильные текстуры — из jar клиента в кэше
Gradle, как в gen_create_compat.py)
"""
import colorsys
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar
from gen_ore_textures import dust

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(HERE, "..", "docs", "textures")

# Жидкость: цвета в ведре от тёмного к светлому (как ModFluids.<жидкость>.color), ru, en
FLUIDS = {
    "creosote": ([(22, 14, 8), (42, 28, 16), (62, 42, 24), (86, 60, 34)], "Креозот", "Creosote"),
    "tannin": ([(74, 40, 16), (108, 64, 28), (140, 90, 42), (170, 118, 60)], "Танин", "Tannin"),
}
CHARCOAL_DUST = [(24, 22, 22), (40, 37, 36), (56, 52, 50), (72, 67, 64), (90, 84, 80), (110, 103, 98)]
SAWDUST = [(118, 84, 48), (150, 110, 64), (178, 136, 84), (200, 160, 104), (220, 184, 128), (236, 206, 154)]

LANG = {
    "item.trailcraft.charcoal_dust": ("Древесноугольная пыль", "Charcoal Dust"),
    "item.trailcraft.sawdust": ("Опилки", "Sawdust"),
    "item.trailcraft.paper_blank": ("Заготовка бумаги", "Paper Blank"),
    "item.trailcraft.stick_impregnated": ("Пропитанная палка", "Impregnated Stick"),
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


def item_model(name):
    write(f"assets/{NS}/models/item/{name}.json",
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{name}"}})


def hsv_map(img, fn):
    img = img.convert("RGBA")
    out = Image.new("RGBA", img.size)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = img.getpixel((x, y))
            if a:
                h, s, v = fn(*colorsys.rgb_to_hsv(r / 255, g / 255, b / 255))
                r, g, b = colorsys.hsv_to_rgb(h % 1.0, max(0, min(1, s)), max(0, min(1, v)))
                out.putpixel((x, y), (round(r * 255), round(g * 255), round(b * 255), a))
    return out


def bucket(water_bucket, colors):
    """Ванильное ведро воды: синие пиксели воды — в цвета жидкости по яркости, ведро остаётся."""
    img = water_bucket.convert("RGBA")
    is_water = lambda c: c[3] and c[2] > c[0] + 30
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    water = [img.getpixel((x, y)) for y in range(16) for x in range(16) if is_water(img.getpixel((x, y)))]
    lo, hi = min(map(lum, water)), max(map(lum, water))
    out = img.copy()
    for y in range(16):
        for x in range(16):
            c = img.getpixel((x, y))
            if is_water(c):
                k = (lum(c) - lo) / max(1, hi - lo)
                out.putpixel((x, y), colors[min(len(colors) - 1, int(k * len(colors)))] + (255,))
    return out


def textures(client):
    vanilla = lambda name: Image.open(BytesIO(client.read(f"assets/minecraft/textures/item/{name}.png")))
    stick = Image.open(os.path.join(TEX, "item", "stick_processed.png"))
    items = {f"{fluid}_bucket": bucket(vanilla("water_bucket"), colors) for fluid, (colors, _ru, _en) in FLUIDS.items()}
    items.update({
        "charcoal_dust": dust(CHARCOAL_DUST, seed=5),
        "sawdust": dust(SAWDUST, seed=7),
        # мокрый лист: бумага серее и темнее
        "paper_blank": hsv_map(vanilla("paper"), lambda h, s, v: (0.1, min(1, s + 0.12), v * 0.82)),
        # обработанная палка, темнее и чуть краснее
        "stick_impregnated": hsv_map(stick, lambda h, s, v: (h - 0.01, min(1, s * 1.15), v * 0.62)),
    })
    for name, img in items.items():
        img.save(os.path.join(TEX, "item", f"{name}.png"))
        item_model(name)
    return list(items.values())


def blocks():
    """Блок жидкости в мире: модель только с частицами (рисует его игра, как воду)."""
    for fluid in FLUIDS:
        write(f"assets/{NS}/models/block/{fluid}.json", {"textures": {"particle": "minecraft:block/water_still"}})
        write(f"assets/{NS}/blockstates/{fluid}.json", {"variants": {"": {"model": f"{NS}:block/{fluid}"}}})


def water(amount):
    return {"type": "neoforge:single", "amount": amount, "fluid": "minecraft:water"}


def item(name, n=1):
    return [{"item": name}] * n


def recipes():
    write(f"data/{NS}/recipe/milling/charcoal.json", {
        "type": "create:milling", "ingredients": item(f"{NS}:charcoal"), "processing_time": 150,
        "results": [{"id": f"{NS}:charcoal_dust"}]})
    write(f"data/{NS}/recipe/milling/bark.json", {
        "type": "create:milling", "ingredients": item(f"{NS}:bark"), "processing_time": 100,
        "results": [{"id": f"{NS}:sawdust"}]})
    write(f"data/{NS}/recipe/mixing/creosote.json", {
        "type": "create:mixing", "heat_requirement": "heated",
        "ingredients": [water(1000)] + item(f"{NS}:resin", 4) + item(f"{NS}:charcoal_dust", 10),
        "results": [{"id": f"{NS}:creosote", "amount": 1000}]})
    write(f"data/{NS}/recipe/mixing/tannin.json", {
        "type": "create:mixing", "ingredients": [water(1000)] + item(f"{NS}:bark"),
        "results": [{"id": f"{NS}:tannin", "amount": 1000}]})
    write(f"data/{NS}/recipe/mixing/paper_blank.json", {
        "type": "create:mixing", "ingredients": [water(250)] + item(f"{NS}:sawdust"),
        "results": [{"id": f"{NS}:paper_blank"}]})
    write(f"data/{NS}/recipe/mixing/paper_blank_from_sugar_cane.json", {
        "type": "create:mixing", "ingredients": [water(250)] + item(f"{NS}:sugar_cane", 2),
        "results": [{"id": f"{NS}:paper_blank"}]})
    write(f"data/{NS}/recipe/pressing/paper_blank.json", {
        "type": "create:pressing", "ingredients": item(f"{NS}:paper_blank"), "results": [{"id": "minecraft:paper"}]})
    write(f"data/{NS}/recipe/filling/stick_impregnated.json", {
        "type": "create:filling",
        "ingredients": item(f"{NS}:stick_processed") + [{"type": "neoforge:single", "amount": 100, "fluid": f"{NS}:creosote"}],
        "results": [{"id": f"{NS}:stick_impregnated"}]})


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        for fluid, (_colors, ru, en) in FLUIDS.items():
            name = (ru, en)[k]
            data[f"fluid_type.{NS}.{fluid}"] = name
            data[f"block.{NS}.{fluid}"] = name
            data[f"item.{NS}.{fluid}_bucket"] = (f"Ведро {ru.lower()}а", f"{en} Bucket")[k]
        write(rel, data)


def preview(images):
    scale, pad = 8, 8
    sheet = Image.new("RGBA", (pad + len(images) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(images):
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "fluids.png"))


if __name__ == "__main__":
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    images = textures(client)
    blocks()
    recipes()
    lang()
    preview(images)
    print("ok")

"""Корнистые зёмы: текстура почвы + наложение ванильного hanging roots, потом уменьшенный контраст.

Заодно кладёт корнистые зёмы в тэг #minecraft:dirt, чтобы на них могли расти трава, рогоз и прочие растения,
и переопределяет дерево азалии (ствол — амадзу, земля под ним — эти три блока).

Запуск из корня проекта:  python scripts/gen_rooted_soils.py   (нужен Pillow)
"""
import io
import json
import os
import sys
import zipfile

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
sys.path.insert(0, os.path.join(ROOT, "scripts"))
from gen_create_compat import find_jar  # noqa: E402

NS = "trailcraft"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", NS)
TEX = os.path.join(ASSETS, "textures", "block")
SOILS = {"rooted_zheltozem": "zheltozem", "rooted_burozem": "burozem", "rooted_chernozem": "chernozem"}
ROOT_ALPHA = 0.9        # насколько плотно ложатся корни
CONTRAST = 0.72         # 1.0 — как есть, меньше — мягче (тянем к средней яркости)


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def hanging_roots():
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    img = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/hanging_roots.png"))).convert("RGBA")
    px = img.load()
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a:
                px[x, y] = (r, g, b, round(a * ROOT_ALPHA))
    return img


def soften(img):
    """Уменьшает контраст: тянет цвет каждого пикселя к средней по текстуре."""
    out = img.copy()
    px = out.load()
    pixels = [px[x, y] for y in range(16) for x in range(16) if px[x, y][3]]
    mean = [sum(p[i] for p in pixels) / len(pixels) for i in range(3)]
    for y in range(16):
        for x in range(16):
            r, g, b, a = px[x, y]
            px[x, y] = tuple(max(0, min(255, round(mean[i] + (c - mean[i]) * CONTRAST)))
                             for i, c in enumerate((r, g, b))) + (a,)
    return out


def main():
    roots = hanging_roots()
    for block, soil in SOILS.items():
        base = Image.open(os.path.join(TEX, soil + ".png")).convert("RGBA")
        merged = base.copy()
        merged.alpha_composite(roots)
        soften(merged).save(os.path.join(TEX, block + ".png"))
        write(os.path.join(ASSETS, "blockstates", block + ".json"),
              {"variants": {"": {"model": f"{NS}:block/{block}"}}})
        write(os.path.join(ASSETS, "models", "block", block + ".json"),
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{block}"}})
        write(os.path.join(ASSETS, "models", "item", block + ".json"), {"parent": f"{NS}:block/{block}"})

    lang_ru = {"rooted_zheltozem": "Жёлтозём с корнями", "rooted_burozem": "Бурозём с корнями",
               "rooted_chernozem": "Чернозём с корнями"}
    lang_en = {"rooted_zheltozem": "Rooted Yellow Soil", "rooted_burozem": "Rooted Brown Soil",
               "rooted_chernozem": "Rooted Black Soil"}
    for code, texts in (("ru_ru", lang_ru), ("en_us", lang_en)):
        path = os.path.join(ASSETS, "lang", code + ".json")
        data = json.load(open(path, encoding="utf-8"))
        for block, text in texts.items():
            data[f"block.{NS}.{block}"] = text
        json.dump(data, open(path, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=2)
        open(path, "a", encoding="utf-8", newline="\n").write("\n")

    # на корнистом зёме должны расти трава, рогоз и прочие растения: это обычная земля
    dirt = os.path.join(ROOT, "src", "main", "resources", "data", "minecraft", "tags", "block", "dirt.json")
    tag = json.load(open(dirt, encoding="utf-8"))
    for block in SOILS:
        value = f"{NS}:{block}"
        if value not in tag["values"]:
            tag["values"].append(value)
    tag["replace"] = False
    json.dump(tag, open(dirt, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=2)
    open(dirt, "a", encoding="utf-8", newline="\n").write("\n")

    # дерево азалии: ствол амадзу, земля под ним — корнистые зёмы
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    tree = json.loads(jar.read("data/minecraft/worldgen/configured_feature/azalea_tree.json"))
    tree["config"]["trunk_provider"] = {"type": "minecraft:simple_state_provider",
                                        "state": {"Name": f"{NS}:amadzu_log", "Properties": {"axis": "y"}}}
    tree["config"]["dirt_provider"] = {"type": "minecraft:weighted_state_provider", "entries": [
        {"data": {"Name": f"{NS}:{block}"}, "weight": 1} for block in SOILS]}
    write(os.path.join(ROOT, "src", "main", "resources", "data", "minecraft", "worldgen",
                       "configured_feature", "azalea_tree.json"), tree)
    print("ok")


if __name__ == "__main__":
    main()

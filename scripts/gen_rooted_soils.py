"""РљРѕСЂРЅРёСЃС‚С‹Рµ Р·С‘РјС‹: С‚РµРєСЃС‚СѓСЂС‹, РјРѕРґРµР»Рё, Р±Р»РѕРєСЃС‚РµР№С‚С‹, РїРµСЂРµРІРѕРґС‹ Рё РґРµСЂРµРІРѕ Р°Р·Р°Р»РёРё РЅР° Р°РјР°РґР·Сѓ.

РўРµРєСЃС‚СѓСЂР° = РЅР°С€Р° РїРѕС‡РІР° + РєРѕСЂРЅРё, РІС‹СЂРµР·Р°РЅРЅС‹Рµ РёР· РІР°РЅРёР»СЊРЅРѕРіРѕ rooted_dirt (РїРёРєСЃРµР»Рё, РіРґРµ РѕРЅ РѕС‚Р»РёС‡Р°РµС‚СЃСЏ РѕС‚ РѕР±С‹С‡РЅРѕР№ Р·РµРјР»Рё).
Р”РµСЂРµРІРѕ Р°Р·Р°Р»РёРё РїРµСЂРµРѕРїСЂРµРґРµР»СЏРµС‚ РІР°РЅРёР»СЊРЅСѓСЋ С„РёС‡Сѓ minecraft:azalea_tree: СЃС‚РІРѕР» вЂ” Р±СЂС‘РІРЅР° Р°РјР°РґР·Сѓ, РїРѕРґ РґРµСЂРµРІРѕРј вЂ” РєРѕСЂРЅРёСЃС‚С‹Рµ Р·С‘РјС‹.
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
DATA = os.path.join(ROOT, "src", "main", "resources", "data", NS)
TEX = os.path.join(ASSETS, "textures", "block")
SOILS = {"rooted_zheltozem": "zheltozem", "rooted_burozem": "burozem", "rooted_chernozem": "chernozem"}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def roots():
    """Разница яркости ванильного rooted_dirt и земли: положительная — светлые корни, отрицательная — тени."""
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    rooted = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/rooted_dirt.png"))).convert("RGBA")
    dirt = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/dirt.png"))).convert("RGBA")
    lum = lambda c: 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]
    return {(x, y): lum(rooted.getpixel((x, y))) - lum(dirt.getpixel((x, y))) for y in range(16) for x in range(16)}


def apply_roots(img, delta):
    """Переносит рисунок корней на нашу текстуру: яркость пикселя сдвигается на разницу ванильных текстур."""
    out = img.copy()
    px = out.load()
    for (x, y), d in delta.items():
        r, g, b, a = px[x, y]
        k = 1.0 + d / 190.0
        px[x, y] = (min(255, max(0, round(r * k))), min(255, max(0, round(g * k))),
                    min(255, max(0, round(b * k))), a)
    return out


def main():
    delta = roots()
    print("sredniy sdvig yarkosti:", round(sum(delta.values()) / len(delta), 1))
    for block, soil in SOILS.items():
        soil_img = Image.open(os.path.join(TEX, soil + ".png")).convert("RGBA")
        apply_roots(soil_img, delta).save(os.path.join(TEX, block + ".png"))
        write(os.path.join(ASSETS, "blockstates", block + ".json"),
              {"variants": {"": {"model": f"{NS}:block/{block}"}}})
        write(os.path.join(ASSETS, "models", "block", block + ".json"),
              {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{block}"}})
        write(os.path.join(ASSETS, "models", "item", block + ".json"), {"parent": f"{NS}:block/{block}"})

    lang_ru = {"rooted_zheltozem": "Р–С‘Р»С‚РѕР·С‘Рј СЃ РєРѕСЂРЅСЏРјРё", "rooted_burozem": "Р‘СѓСЂРѕР·С‘Рј СЃ РєРѕСЂРЅСЏРјРё",
               "rooted_chernozem": "Р§РµСЂРЅРѕР·С‘Рј СЃ РєРѕСЂРЅСЏРјРё"}
    lang_en = {"rooted_zheltozem": "Rooted Yellow Soil", "rooted_burozem": "Rooted Brown Soil",
               "rooted_chernozem": "Rooted Black Soil"}
    for code, texts in (("ru_ru", lang_ru), ("en_us", lang_en)):
        path = os.path.join(ASSETS, "lang", code + ".json")
        data = json.load(open(path, encoding="utf-8"))
        for block, text in texts.items():
            data[f"block.{NS}.{block}"] = text
        json.dump(data, open(path, "w", encoding="utf-8", newline="\n"), ensure_ascii=False, indent=2)
        open(path, "a", encoding="utf-8", newline="\n").write("\n")

    # РґРµСЂРµРІРѕ Р°Р·Р°Р»РёРё: СЃС‚РІРѕР» Р°РјР°РґР·Сѓ, Р·РµРјР»СЏ РїРѕРґ РЅРёРј вЂ” РєРѕСЂРЅРёСЃС‚С‹Рµ Р·С‘РјС‹
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


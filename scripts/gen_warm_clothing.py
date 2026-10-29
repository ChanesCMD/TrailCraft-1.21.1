"""Тёплая одежда (item/WarmClothingItem, слоты «Тепло») и переводы температуры (climate/).

Текстуры — ванильные кожаные шлем, нагрудник, поножи, ботинки, перекрашенные по яркости (recolor из gen_create_compat.py):
меховая шапка, тулуп, ватные штаны, валенки. Пишет модели, рецепты, тэг #trailcraft:warm_clothing и переводы
(одежда, подсказки, температура в HUD, диапазон культур). Тепло в градусах — ModItems (WarmClothingItem).

Запуск из корня проекта:  python scripts/gen_warm_clothing.py   (нужен Pillow и кэш Gradle с клиентом Minecraft)
"""
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar, recolor

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
NS = "trailcraft"

# предмет: ванильная текстура, палитра от тёмного к светлому, рецепт, ключ
CLOTHES = {
    "fur_hat": ("leather_helmet", [(52, 33, 20), (84, 56, 34), (118, 84, 54), (152, 116, 80), (186, 154, 118)],
                ["WLW", "L L"], {"W": {"tag": "minecraft:wool"}, "L": {"tag": "c:leathers"}}),
    "sheepskin_coat": ("leather_chestplate", [(74, 50, 30), (118, 84, 50), (160, 122, 80), (198, 164, 120), (232, 214, 186)],
                       ["L L", "LWL", "LWL"], {"W": {"tag": "minecraft:wool"}, "L": {"tag": "c:leathers"}}),
    "quilted_trousers": ("leather_leggings", [(30, 34, 42), (48, 54, 66), (68, 76, 92), (92, 102, 120), (120, 130, 148)],
                         ["CWC", "C C", "C C"], {"W": {"tag": "minecraft:wool"}, "C": {"item": f"{NS}:cloth"}}),
    "felt_boots": ("leather_boots", [(56, 56, 58), (84, 84, 86), (112, 110, 108), (140, 138, 134), (170, 168, 162)],
                   ["W W", "W W"], {"W": {"tag": "minecraft:wool"}}),
}

LANG = {
    "item.trailcraft.fur_hat": ("Меховая шапка", "Fur Hat"),
    "item.trailcraft.sheepskin_coat": ("Тулуп", "Sheepskin Coat"),
    "item.trailcraft.quilted_trousers": ("Ватные штаны", "Quilted Trousers"),
    "item.trailcraft.felt_boots": ("Валенки", "Felt Boots"),
    "tooltip.trailcraft.warm_clothing": ("Тепло: +%s °C", "Warmth: +%s °C"),
    "tooltip.trailcraft.crop_temperature": ("Растёт при %s…%s °C", "Grows at %s…%s °C"),
    "gui.trailcraft.hud.temperature": ("%s °C", "%s °C"),
    "commands.trailcraft.calendar.temperature": ("Температура здесь: %s °C (средняя за сутки %s °C)",
                                                 "Temperature here: %s °C (daily mean %s °C)"),
}


def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read(rel, default):
    try:
        with open(os.path.join(ROOT, *rel.split("/")), encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return default


def main():
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    for item, (vanilla, palette, pattern, key) in CLOTHES.items():
        img = Image.open(BytesIO(client.read(f"assets/minecraft/textures/item/{vanilla}.png"))).convert("RGBA")
        recolor(img, palette).save(os.path.join(ROOT, "assets", NS, "textures", "item", f"{item}.png"))
        write(f"assets/{NS}/models/item/{item}.json",
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{item}"}})
        write(f"data/{NS}/recipe/{item}.json", {
            "type": "minecraft:crafting_shaped", "category": "equipment", "key": key, "pattern": pattern,
            "result": {"id": f"{NS}:{item}", "count": 1}})
    tag = read(f"data/{NS}/tags/item/warm_clothing.json", {"replace": False, "values": []})
    for item in CLOTHES:
        if f"{NS}:{item}" not in tag["values"]:
            tag["values"].append(f"{NS}:{item}")
    write(f"data/{NS}/tags/item/warm_clothing.json", tag)
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)
    print("ok")


if __name__ == "__main__":
    main()

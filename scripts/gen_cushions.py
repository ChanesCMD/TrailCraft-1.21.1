#!/usr/bin/env python3
"""Подушки для сидения (cushion/): модели предметов, рецепты, тег и названия. Запуск из корня проекта.
Текстуры рисуются здесь же (textures/item/cushion/<цвет>_top|side.png): светлая подушка с пуговицей и четырьмя подушечками, цвет — множитель цвета красителя.
Рецепт: 3 ковра одного цвета в ряд (в 1.21.1 нет шерстяных плит из 26.3; TODO: заменить на плиты, если их добавят),
плюс перекраска: любая подушка + краситель."""
import json
import os
import re

ASSETS = "src/main/resources/assets/trailcraft"
DATA = "src/main/resources/data/trailcraft"
COLORS = {  # цвет: (английское название, русское название)
    "white": ("White", "Белая"), "orange": ("Orange", "Оранжевая"), "magenta": ("Magenta", "Пурпурная"),
    "light_blue": ("Light Blue", "Голубая"), "yellow": ("Yellow", "Жёлтая"), "lime": ("Lime", "Лаймовая"),
    "pink": ("Pink", "Розовая"), "gray": ("Gray", "Серая"), "light_gray": ("Light Gray", "Светло-серая"),
    "cyan": ("Cyan", "Бирюзовая"), "purple": ("Purple", "Фиолетовая"), "blue": ("Blue", "Синяя"),
    "brown": ("Brown", "Коричневая"), "green": ("Green", "Зелёная"), "red": ("Red", "Красная"),
    "black": ("Black", "Чёрная"),
}


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="") as f:
        f.write(json.dumps(data, indent=2, ensure_ascii=False).replace("\n", "\r\n") + "\r\n")


# цвета красителей (DyeColor.getTextureDiffuseColor): подушка красится множителем по светлой текстуре
DYE_RGB = {
    "white": 0xF9FFFE, "orange": 0xF9801D, "magenta": 0xC74EBD, "light_blue": 0x3AB3DA, "yellow": 0xFED83D,
    "lime": 0x80C71F, "pink": 0xF38BAA, "gray": 0x474F52, "light_gray": 0x9D9D97, "cyan": 0x169C9C,
    "purple": 0x8932B8, "blue": 0x3C44AA, "brown": 0x835432, "green": 0x5E7C16, "red": 0xB02E26, "black": 0x1D1D21,
}
HEIGHT_PX = 4  # подушка плоская: 16x16 пикселей сверху, 4 в высоту (CushionEntity.HEIGHT)


def top_shades():
    """Светлота (0..1) верха 16x16: четыре мягкие подушечки по диагоналям, складки по осям и пуговица в центре."""
    grid = []
    for y in range(16):
        row = []
        for x in range(16):
            u, v = x if x < 8 else 15 - x, y if y < 8 else 15 - y  # расстояние до края своей четверти: 0..7
            cu, cv = abs(u - 4.2), abs(v - 4.2)
            shade = 0.97 - 0.10 * ((cu * cu + cv * cv) / 18.0)  # подушечка: светлее в середине, темнее к краям
            if u >= 6 or v >= 6:
                shade -= 0.07 * (max(u, v) - 5)  # склон к складке по оси
            if u == 7 or v == 7:
                shade = 0.80 if (u == 7) != (v == 7) else 0.76  # складки креста
            if u >= 6 and v >= 6:  # пуговица 4x4 в центре: светлая, с тёмным ободком
                shade = 0.99 if u == 7 and v == 7 else 0.86
            row.append(max(0.0, min(1.0, shade)))
        grid.append(row)
    return grid


def side_shades():
    """Бок 16x16 (используются верхние HEIGHT_PX рядов): шерстяной шум, светлая кромка сверху, темнее к низу."""
    import random
    rng = random.Random(7)
    grid = []
    for y in range(16):
        base = 0.93 if y == 0 else 0.86 - 0.025 * y
        grid.append([max(0.0, min(1.0, base + rng.uniform(-0.035, 0.035))) for _ in range(16)])
    return grid


def textures():
    from PIL import Image
    for color, rgb in DYE_RGB.items():
        r, g, b = (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255
        for name, grid in (("top", top_shades()), ("side", side_shades())):
            img = Image.new("RGBA", (16, 16))
            for y in range(16):
                for x in range(16):
                    k = grid[y][x]
                    img.putpixel((x, y), (round(r * k), round(g * k), round(b * k), 255))
            path = f"{ASSETS}/textures/item/cushion/{color}_{name}.png"
            os.makedirs(os.path.dirname(path), exist_ok=True)
            img.save(path)


def models():
    faces = {"up": {"texture": "#top", "uv": [0, 0, 16, 16]}}
    for d in ("north", "south", "west", "east"):
        faces[d] = {"texture": "#side", "uv": [0, 0, 16, HEIGHT_PX]}
    faces["down"] = {"texture": "#side", "uv": [0, 0, 16, 16]}
    write(f"{ASSETS}/models/item/cushion_base.json", {
        "textures": {"particle": "#side"},
        # один сплошной плоский блок во всю клетку, без верхнего слоя: соседние подушки сходятся без зазоров
        "elements": [{"from": [0, 0, 0], "to": [16, HEIGHT_PX, 16], "faces": faces}],
        "display": {
            "gui": {"rotation": [30, 45, 0], "translation": [0, 2, 0], "scale": [0.9, 0.9, 0.9]},
            "ground": {"translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
            "fixed": {"scale": [1, 1, 1]},
            "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.4, 0.4, 0.4]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        }})
    for color in COLORS:
        write(f"{ASSETS}/models/item/{color}_cushion.json", {
            "parent": "trailcraft:item/cushion_base",
            "textures": {"top": f"trailcraft:item/cushion/{color}_top", "side": f"trailcraft:item/cushion/{color}_side"}})


def recipes():
    for color in COLORS:
        write(f"{DATA}/recipe/{color}_cushion.json", {
            "type": "minecraft:crafting_shaped", "category": "misc",
            "key": {"#": {"item": f"minecraft:{color}_carpet"}}, "pattern": ["###"],
            "result": {"id": f"trailcraft:{color}_cushion", "count": 1}})
        write(f"{DATA}/recipe/{color}_cushion_dye.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"tag": "trailcraft:cushions"}, {"item": f"minecraft:{color}_dye"}],
            "result": {"id": f"trailcraft:{color}_cushion", "count": 1}})
    write(f"{DATA}/tags/item/cushions.json", {"values": [f"trailcraft:{c}_cushion" for c in COLORS]})


def lang():
    for code, idx, entity in (("en_us", 0, "Cushion"), ("ru_ru", 1, "Подушка")):
        path = f"{ASSETS}/lang/{code}.json"
        text = open(path, encoding="utf-8", newline="").read()
        text = re.sub(r'\r?\n\s*"(item\.trailcraft\.\w+_cushion|entity\.trailcraft\.cushion)": "[^"]*",?', "", text)
        entries = [f'  "entity.trailcraft.cushion": "{entity}"']
        for color, names in COLORS.items():
            word = "Cushion" if idx == 0 else "подушка"
            entries.append(f'  "item.trailcraft.{color}_cushion": "{names[idx]} {word.lower() if idx else word}"')
        body = text.rstrip()
        assert body.endswith("}")
        body = body[:-1].rstrip()
        if not body.endswith(","):
            body += ","
        open(path, "w", encoding="utf-8", newline="").write(body + "\r\n" + ",\r\n".join(entries) + "\r\n}\r\n")


if __name__ == "__main__":
    textures()
    models()
    recipes()
    lang()
    print("ok")

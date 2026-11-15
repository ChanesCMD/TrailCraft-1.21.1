"""Вода менее прозрачная: копии ванильных текстур воды с поднятой непрозрачностью.

Ванильная вода имеет альфу 180 из 255 (около 0,71) — сквозь неё слишком хорошо видно дно.
Скрипт берёт ванильные water_still и water_flow и поднимает альфу до ALPHA, цвет не трогает.
Файлы кладутся в assets/minecraft/textures/block, поэтому перекрывают ванильные.

Запуск из корня проекта:  python scripts/gen_water.py   (нужен Pillow)
"""
import io
import os
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_create_compat import find_jar  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
DST = os.path.join(ROOT, "src", "main", "resources", "assets", "minecraft", "textures", "block")
ALPHA = 230         # было 180 у ванили; больше — вода плотнее
NAMES = ("water_still", "water_flow")


def main():
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    os.makedirs(DST, exist_ok=True)
    for name in NAMES:
        im = Image.open(io.BytesIO(jar.read(f"assets/minecraft/textures/block/{name}.png"))).convert("RGBA")
        px = im.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = px[x, y]
                px[x, y] = (r, g, b, ALPHA if a else 0)
        im.save(os.path.join(DST, name + ".png"))
    print("ok")


if __name__ == "__main__":
    main()

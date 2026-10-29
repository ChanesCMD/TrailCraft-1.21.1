"""
Текстуры пород, которые правятся из исходника в scripts/templates/: диорит — трещины не доходят до краёв блока
(иначе на стене из диорита трещины сливаются в сетку): тёмные пиксели трещин по краю (EDGE пикселей) заменяются
цветом соседнего светлого пикселя внутри, а сами трещины светлее (смешаны со средним цветом камня на CRACK_SOFTEN);
андезит — ванильный андезит, смешанный с нашим (ANDESITE_OURS — доля нашего).

Запуск из корня проекта:  python scripts/gen_stone_textures.py   (нужен Pillow и jar клиента в кэше Gradle)
"""
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar

HERE = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trailcraft", "textures", "block")
EDGE = 2
CRACK = 130  # яркость пикселей трещин — не выше
CRACK_SOFTEN = 0.5  # насколько трещины диорита приближены к цвету камня (0 — как в исходнике)
ANDESITE_OURS = 0.25


def lum(c):
    return sum(c[:3]) // 3


def diorite():
    img = Image.open(os.path.join(HERE, "templates", "diorite.png")).convert("RGBA")
    out = img.copy()
    n = img.width
    for y in range(n):
        for x in range(n):
            if min(x, y, n - 1 - x, n - 1 - y) >= EDGE or lum(img.getpixel((x, y))) > CRACK:
                continue
            # ближайший светлый пиксель по направлению к центру
            for step in range(1, n):
                qx = x + step * (1 if x < n // 2 else -1) * (min(x, n - 1 - x) < EDGE)
                qy = y + step * (1 if y < n // 2 else -1) * (min(y, n - 1 - y) < EDGE)
                c = img.getpixel((qx % n, qy % n))
                if lum(c) > CRACK:
                    out.putpixel((x, y), c)
                    break
    stone = [c for c in img.get_flattened_data() if lum(c) > CRACK]
    mean = tuple(sum(c[i] for c in stone) // len(stone) for i in range(3))
    for y in range(n):
        for x in range(n):
            c = out.getpixel((x, y))
            if lum(c) <= CRACK:
                out.putpixel((x, y), tuple(round(c[i] + (mean[i] - c[i]) * CRACK_SOFTEN) for i in range(3)) + (c[3],))
    out.save(os.path.join(TEX, "diorite.png"))


def andesite(client):
    vanilla = Image.open(BytesIO(client.read("assets/minecraft/textures/block/andesite.png"))).convert("RGBA")
    ours = Image.open(os.path.join(HERE, "templates", "andesite.png")).convert("RGBA")
    Image.blend(vanilla, ours, ANDESITE_OURS).save(os.path.join(TEX, "andesite.png"))


# Ванильные гранит, диорит, андезит (самый глубокий слой пород) — «древние», чтобы не путать с нашими (id прежние)
ANCIENT = {"granite": ("Древний гранит", "Ancient Granite"), "diorite": ("Древний диорит", "Ancient Diorite"),
           "andesite": ("Древний андезит", "Ancient Andesite")}


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        p = os.path.join(HERE, "..", "src", "main", "resources", "assets", "trailcraft", "lang", f"{code}.json")
        with open(p, encoding="utf-8") as f:
            data = json.load(f)
        for rock, names in ANCIENT.items():
            data[f"block.minecraft.{rock}"] = names[k]
        with open(p, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")


if __name__ == "__main__":
    lang()
    diorite()
    andesite(zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar")))
    print("ok")

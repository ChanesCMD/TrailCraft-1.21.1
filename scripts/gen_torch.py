"""Наш факел: блок (слабее ванильного), предмет, модель, текстура, переводы и палка в костре."""
import json
import os

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")
TEX = os.path.join(ASSETS, "textures", "block")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def texture():
    """Палка со светотенью как у обработанной палки, верх — джутовый, заметно светлее палки."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # stick_processed даёт светлый верхний край и тёмный нижний: переносим это на вертикальную палку
    light_top, light_bottom = (212, 176, 122), (143, 106, 58)
    dark_top, dark_bottom = (107, 76, 40), (69, 48, 26)

    def lerp(a, b, t):
        return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))

    rows = list(range(9, 16))
    for i, y in enumerate(rows):
        t = i / (len(rows) - 1)
        px[7, y] = lerp(light_top, light_bottom, t) + (255,)     # светлая грань
        px[8, y] = lerp(dark_top, dark_bottom, t) + (255,)       # тёмная грань
    # верхушка: яркий джутовый уголёк, чтобы контрастировал с палкой
    ember = {6: (236, 214, 152), 7: (206, 174, 106), 8: (172, 138, 74)}
    for y, tone in ember.items():
        for x in (7, 8):
            px[x, y] = tone + (255,)
    px[6, 6] = (246, 228, 172, 255)
    img.save(os.path.join(TEX, "torch.png"))


def main():
    texture()
    write(os.path.join(ASSETS, "blockstates", "torch.json"),
          {"variants": {"": {"model": "trailcraft:block/torch"}}})
    write(os.path.join(ASSETS, "models", "block", "torch.json"),
          {"parent": "minecraft:block/torch",
           "render_type": "minecraft:cutout",
           "textures": {"particle": "trailcraft:block/torch", "torch": "trailcraft:block/torch"}})
    write(os.path.join(ASSETS, "models", "item", "torch.json"),
          {"parent": "minecraft:item/generated",
           "textures": {"layer0": "trailcraft:block/torch"}})
    for code, text in (("ru_ru", "Факел"), ("en_us", "Torch")):
        path = os.path.join(ASSETS, "lang", code + ".json")
        data = json.load(open(path, encoding="utf-8"))
        data["block.trailcraft.torch"] = text
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

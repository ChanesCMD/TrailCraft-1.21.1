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
    """Как у ванильного: уголёк в строках 6-8, палка ниже; остальное прозрачное."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    stick = [(92, 66, 40), (78, 55, 33), (104, 76, 46)]
    # палка: с 9-й строки до низа, 2 пикселя шириной (как в ванильной текстуре)
    for y in range(9, 16):
        for x in (7, 8):
            px[x, y] = stick[(x + y) % 3] + (255,)
    # тлеющий уголёк вместо пламени — на тех же строках, где у ванили огонь
    ember = {6: (232, 166, 84), 7: (206, 128, 58), 8: (170, 92, 38)}
    for y, tone in ember.items():
        for x in (7, 8):
            px[x, y] = tone + (255,)
    px[7, 6] = (246, 200, 120, 255)
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

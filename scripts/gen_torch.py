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
    """Палка — оттенки обработанной палки, верх — оттенки джута, чуть темнее; разметка как у ванильного."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    # stick_processed: (107,76,40), (143,106,58), (184,148,90), (212,176,122), (69,48,26)
    stick = [(107, 76, 40), (143, 106, 58), (69, 48, 26), (107, 76, 40)]
    for y in range(9, 16):
        for x in (7, 8):
            px[x, y] = stick[(x * 2 + y) % len(stick)] + (255,)
    # jute: (151,121,63), (201,171,102), (230,207,146) — притемнённые на 15 %
    ember = {6: (207, 186, 131), 7: (181, 154, 92), 8: (141, 113, 59)}
    for y, tone in ember.items():
        for x in (7, 8):
            px[x, y] = tone + (255,)
    px[7, 6] = (230, 207, 146, 255)
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

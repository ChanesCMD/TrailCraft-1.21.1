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
    """Палка с тлеющим угольком вместо яркого пламени."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    stick = [(92, 66, 40), (78, 55, 33), (104, 76, 46)]
    for y in range(6, 16):
        for x in (7, 8):
            px[x, y] = stick[(x + y) % 3] + (255,)
    px[6, 15] = stick[1] + (255,) if False else px[6, 15]
    # уголёк: тёмно-оранжевый, к верху светлее
    ember = [(120, 58, 24), (150, 74, 28), (176, 96, 36), (204, 126, 52), (226, 158, 74)]
    for i, y in enumerate(range(5, 0, -1)):
        tone = ember[min(len(ember) - 1, i)]
        for x in (7, 8):
            px[x, y] = tone + (255,)
    px[7, 0] = (240, 190, 110, 235)
    px[8, 0] = (220, 150, 80, 200)
    img.save(os.path.join(TEX, "torch.png"))


def main():
    texture()
    write(os.path.join(ASSETS, "blockstates", "torch.json"),
          {"variants": {"": {"model": "trailcraft:block/torch"}}})
    write(os.path.join(ASSETS, "models", "block", "torch.json"),
          {"parent": "minecraft:block/torch",
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

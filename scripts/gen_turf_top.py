"""Верх дёрна (turf_top): серый рисунок в стиле ванильной травы, красится цветом травы биома."""
import io
import os
import random
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gen_create_compat import find_jar  # noqa: E402

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures", "block")
SEED = 20261115


def main():
    jar = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    vanilla = Image.open(io.BytesIO(jar.read("assets/minecraft/textures/block/grass_block_top.png"))).convert("RGBA")
    rnd = random.Random(SEED)
    out = Image.new("RGBA", (16, 16))
    px = out.load()
    for y in range(16):
        for x in range(16):
            v = vanilla.getpixel((x, y))[0]
            # небольшая своя вариация, чтобы рисунок не был копией ванильного
            v = max(96, min(208, v + rnd.randint(-6, 6)))
            px[x, y] = (v, v, v, 255)
    out.save(os.path.join(TEX, "turf_top.png"))
    out.resize((160, 160), Image.NEAREST).save(os.path.join(ROOT, "build", "tmp", "turf_top_new.png"))
    print("ok")


if __name__ == "__main__":
    main()

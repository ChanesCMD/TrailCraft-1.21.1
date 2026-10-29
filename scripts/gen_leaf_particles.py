"""
Падающая листва (client/LeafParticles): лепестки сакуры и листья ивы — 12 кадров ванильного лепестка вишни
(textures/particle/cherry_0..11), перекрашенные по яркости в цвета листвы породы (розовые пиксели листвы сакуры,
зелёные — ивы); particles/<частица>.json. Превью — docs/textures/leaf_particles.png.

Запуск из корня проекта:  python scripts/gen_leaf_particles.py   (нужен Pillow и jar клиента в кэше Gradle)
"""
import colorsys
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.join(HERE, "..", "src", "main", "resources", "assets", NS)
DOCS = os.path.join(HERE, "..", "docs", "textures")
FRAMES = 12


def hue(c):
    return colorsys.rgb_to_hsv(*(k / 255 for k in c[:3]))


# частица -> листва, какие её пиксели берутся
LEAVES = {
    "sakura_petal": ("sakura_leaves", lambda c: c[0] > c[1] + 20 and hue(c)[1] > 0.2),
    "willow_leaf": ("willow_leaves", lambda c: c[1] >= c[0] and hue(c)[1] > 0.2),
}


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor(img, palette):
    px = [c for c in img.get_flattened_data() if c[3]]
    lo, hi = min(map(lum, px)), max(map(lum, px))
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            c = img.getpixel((x, y))
            if c[3]:
                k = (lum(c) - lo) / max(1, hi - lo)
                out.putpixel((x, y), palette[min(len(palette) - 1, int(k * len(palette)))] + (c[3],))
    return out


def main():
    client = zipfile.ZipFile(find_jar("minecraft_1.21.1_client.jar"))
    previews = []
    for particle, (leaves, pick) in LEAVES.items():
        tex = Image.open(os.path.join(ASSETS, "textures", "block", f"{leaves}.png")).convert("RGBA")
        palette = sorted({c[:3] for c in tex.get_flattened_data() if c[3] and pick(c)}, key=lum)
        names = []
        for i in range(FRAMES):
            src = Image.open(BytesIO(client.read(f"assets/minecraft/textures/particle/cherry_{i}.png"))).convert("RGBA")
            img = recolor(src, palette)
            p = os.path.join(ASSETS, "textures", "particle", f"{particle}_{i}.png")
            os.makedirs(os.path.dirname(p), exist_ok=True)
            img.save(p)
            names.append(f"{NS}:{particle}_{i}")
            previews.append(img)
        with open(os.path.join(ASSETS, "particles", f"{particle}.json"), "w", encoding="utf-8", newline="\n") as f:
            json.dump({"textures": names}, f, ensure_ascii=False, indent=2)
            f.write("\n")
    scale, pad = 8, 4
    w = previews[0].width * scale
    sheet = Image.new("RGBA", (pad + FRAMES * (w + pad), pad + 2 * (w + pad)), (60, 60, 60, 255))
    for i, img in enumerate(previews):
        sheet.alpha_composite(img.resize((w, w), Image.NEAREST), (pad + i % FRAMES * (w + pad), pad + i // FRAMES * (w + pad)))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "leaf_particles.png"))


if __name__ == "__main__":
    main()
    print("ok")

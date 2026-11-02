"""Мелкие камешки на жёлтозёме: точки, которые видно при повороте блока."""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", "trailcraft", "textures", "block", "zheltozem.png")

img = Image.open(TEX).convert("RGBA")
for x, y, c in (
    (3, 4, (205, 195, 168, 255)),
    (10, 6, (150, 138, 110, 255)),
    (6, 11, (215, 205, 180, 255)),
    (12, 12, (145, 132, 105, 255)),
    (2, 13, (195, 183, 155, 255)),
    (8, 2, (155, 143, 115, 255)),
):
    img.putpixel((x, y), c)
img.save(TEX)
print("ok")

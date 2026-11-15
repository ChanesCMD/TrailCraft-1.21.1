"""Взять участок референса 1:1 как текстуру облаков."""
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
REF = os.path.join(os.path.dirname(os.path.abspath(__file__)), "templates", "cloud_reference.png")
OUT = os.path.join(ROOT, "assets", "minecraft", "textures", "environment", "clouds.png")

src = Image.open(REF).convert("RGB")
# Нативный участок без сжатия всего скриншота: сохраняет толщину и длину пластов.
crop = src.crop((416, 32, 672, 288))
gray = crop.convert("L")
mask = gray.point(lambda p: 255 if p > 140 else 0)
out = crop.convert("RGBA")
out.putalpha(mask)
out.save(OUT)
print("ok", crop.size)

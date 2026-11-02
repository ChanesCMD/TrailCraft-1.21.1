"""Текстуры зубил (медь, бронза, железо, сталь) — для головок, форм и предметов."""
import json
import os
from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")
TEX = os.path.join(ASSETS, "textures", "item")
MODELS = os.path.join(ASSETS, "models", "item")

PATTERN = [
    "................",
    "................",
    "..........hh....",
    ".........hmm....",
    "........hmm.....",
    ".......hmm......",
    "......hmm.......",
    ".....hmm........",
    "....hmm.........",
    "...hmm..........",
    "..hmm...........",
    "..hm............",
    "..12............",
    ".123............",
    ".34.............",
    "................",
]
HANDLE = {"1": (184, 148, 90), "2": (143, 106, 58), "3": (107, 76, 40), "4": (69, 48, 26)}
HEADS = {
    "copper": {"h": (240, 170, 130), "m": (182, 98, 60)},
    "bronze": {"h": (240, 200, 110), "m": (178, 122, 38)},
    "iron": {"h": (242, 249, 255), "m": (183, 189, 200)},
    "steel": {"h": (220, 230, 240), "m": (140, 152, 168)},
}

NAMES = {
    "copper": ("Медное зубило", "Copper Chisel"),
    "bronze": ("Бронзовое зубило", "Bronze Chisel"),
    "iron": ("Железное зубило", "Iron Chisel"),
    "steel": ("Стальное зубило", "Steel Chisel"),
}
for material, head in HEADS.items():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(PATTERN):
        for x, ch in enumerate(row):
            if ch in HANDLE:
                img.putpixel((x, y), HANDLE[ch] + (255,))
            elif ch in head:
                img.putpixel((x, y), head[ch] + (255,))
    img.save(os.path.join(TEX, f"{material}_chisel.png"))
    with open(os.path.join(MODELS, f"{material}_chisel.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": f"trailcraft:item/{material}_chisel"}}, f, indent=2)
        f.write("\n")

for code, idx in (("ru_ru", 0), ("en_us", 1)):
    path = os.path.join(ASSETS, "lang", f"{code}.json")
    with open(path, encoding="utf-8") as f:
        data = json.load(f)
    for material, names in NAMES.items():
        data[f"item.trailcraft.{material}_chisel"] = names[idx]
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")
print("ok")

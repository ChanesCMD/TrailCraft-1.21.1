"""Текстуры новых диких растений: просо, крапива, подорожник, полынь, водосбор и рогоз."""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
TEX = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft", "textures", "block")


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def draw(rows, colors):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch in colors:
                img.putpixel((x, y), colors[ch])
    return img


GREEN = {"g": hx("#4f7a30"), "d": hx("#3a5c22"), "l": hx("#6a9a40")}
YELLOW = {"y": hx("#d8b63e"), "Y": hx("#f0d25a")}
PURPLE = {"p": hx("#5b4a9a"), "P": hx("#8a72c8"), "w": hx("#e8e0ff")}

CATTAIL_BOTTOM = [
    "................",
    "................",
    ".......g........",
    "..g....g....g...",
    "..g...gg...g....",
    "...g..g.g..g....",
    "...gg.g.g.gg....",
    "....ggggggg.....",
    ".....ggggg......",
    "......ggg.......",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
    ".......g........",
]
CATTAIL_TOP = [
    "................",
    "................",
    ".....ggggg......",
    ".....bbbbb......",
    "....bbbbbbb.....",
    "....bbbbbbb.....",
    "....bbbbbbb.....",
    ".....bbbbb......",
    "......bbb.......",
    ".......g........",
    ".......g........",
    "..g....g....g...",
    "..g...gg...g....",
    "...g..g.g..g....",
    "....ggggggg.....",
    ".....ggggg......",
]
MILLET = [
    "................",
    "....y...y.......",
    "...yyy.yyy......",
    "...yyy.yyy......",
    "....y...y.......",
    "....g...g.......",
    "...g.g.g.g......",
    "..g..ggg..g.....",
    "..g...g...g.....",
    "...g..g..g......",
    "....g.g.g.......",
    ".....ggg........",
    "......g.........",
    "......g.........",
    "......g.........",
    "................",
]
NETTLE = [
    "................",
    "................",
    "......g.........",
    ".....ggg........",
    "....ggggg.......",
    "...gg.g.gg......",
    "..gg..g..gg.....",
    "..g..ggg..g.....",
    "...g.ggg.g......",
    "....ggggg.......",
    ".....ggg........",
    "......g.........",
    ".....ggg........",
    "....g.g.g.......",
    ".....g.g........",
    "................",
]
PLANTAIN = [
    "................",
    ".......f........",
    "......fff.......",
    "......fff.......",
    ".......f........",
    ".......g........",
    ".......g........",
    ".......g........",
    "....g..g..g.....",
    "...ggg.g.ggg....",
    "..gggggggggg....",
    "..gg.gggg.gg....",
    "...g..gg..g.....",
    "......gg........",
    "................",
    "................",
]
WORMWOOD = [
    "................",
    "....s..s...s....",
    "...sss.ss.sss...",
    "....s..s...s....",
    "...s..sss..s....",
    "..s.s.s.s.s.s...",
    "...sss.s.sss....",
    "....s..s..s.....",
    ".....sssss......",
    "......sss.......",
    ".......s........",
    ".......s........",
    ".......s........",
    ".......s........",
    "................",
    "................",
]
COLUMBINE = [
    "................",
    ".....p..........",
    "....pPp.........",
    "...p.P.p........",
    "....pPp.........",
    ".....p..........",
    ".....g..........",
    "..p..g..........",
    ".pPp.g...p......",
    "..p..g..pPp.....",
    ".....g...p......",
    ".....g..........",
    "....ggg.........",
    ".....g..........",
    ".....g..........",
    "................",
]

W = os.path.join(TEX)


def save(name, rows, colors):
    draw(rows, colors).save(os.path.join(W, name + ".png"))


save("cattail_bottom", CATTAIL_BOTTOM, GREEN)
save("cattail_top", CATTAIL_TOP, {**GREEN, "b": hx("#7a4a1e")})
save("millet", MILLET, {**GREEN, **YELLOW})
save("nettle", NETTLE, GREEN)
save("plantain", PLANTAIN, {**GREEN, "f": hx("#c8c0a0")})
save("wormwood", WORMWOOD, {"s": hx("#9aa77a")})
save("columbine", COLUMBINE, {**GREEN, **PURPLE})
print("ok: 7 textures")

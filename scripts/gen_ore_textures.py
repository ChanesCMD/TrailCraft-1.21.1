"""
Текстуры руд в ванильном стиле: куски руды raw_* (форма и светотень — как у ванильных кусков золота, железа, меди,
плюс пятнистая форма из образца автора), самородки (предмет и камешек на земле) и шихта.

Запуск из корня проекта:  python scripts/gen_ore_textures.py   (нужен Pillow)
Цвета каждой руды — RAMPS: основная гамма от тёмного к светлому и, если есть, вкрапления (ACCENTS).
У сфалерита сохранена прежняя расцветка: серый с янтарными вкраплениями.
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", "trailcraft",
                    "textures")

# Формы 16×16: '.' — пусто, цифра 0–9 — яркость основного цвета, буква a–j — яркость вкраплений.
SHAPES = {
    "gold": [
        "................",
        "................",
        "......2222222...",
        "....2234486332..",
        "...234646644332.",
        "..2896466446440.",
        ".29998998864330.",
        ".29988886434320.",
        ".26864644333220.",
        ".036464433346840",
        ".033644324689430",
        "..04333224433320",
        "..03432203443220",
        "...03320.032220.",
        "....000...0000..",
        "................",
    ],
    "iron": [
        "................",
        "..11111.........",
        ".158865111......",
        ".189898685111...",
        "16999898988651..",
        "165699886865651.",
        "064465443345451.",
        "053454443113441.",
        "053354431111130.",
        "043331111896440.",
        "0443311166685430",
        ".043330046653110",
        "..0000..04443110",
        ".........044110.",
        "..........0000..",
        "................",
    ],
    "copper": [
        "................",
        "................",
        "...ccc...c1111..",
        "..ceddccc478861.",
        ".cggegde4789870.",
        ".cijigg44667610.",
        "1d4gedd4311360..",
        "187e4d43113330..",
        "188764631131000.",
        "0786763113867330",
        ".07663131897632a",
        ".a231311078832da",
        ".aed210006631dda",
        "..aaa0...0611aa.",
        "..........aaa...",
        "................",
    ],
    "spotted": [
        "................",
        ".......000......",
        "...e0.0570000...",
        "..ehj0575hjhfc..",
        ".ej9h754h9hffec.",
        ".cfhe47922ffeec.",
        "..c457922575eec.",
        ".04775hj575002c.",
        ".07952jhf0002c..",
        ".c0025ff02257c..",
        ".c0200202577hfc.",
        "..cc000c55hffea.",
        "....c0cchjjheea.",
        ".....cccafeeea..",
        ".........aaaa...",
        "................",
    ],
}

# Самородок-предмет: небольшой комок (цифры — яркость)
NUGGET = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "......2443......",
    ".....258953.....",
    "....25899741....",
    "...136887542.23.",
    "...24675431.2562",
    "...13453310.2441",
    "....012210..0110",
    ".....0000.......",
    "................",
    "................",
    "................",
]


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def ramp(*colors):
    return [hx(c) for c in colors]


# Основная гамма каждой руды (от тёмного к светлому) и форма куска
RAMPS = {
    "native_gold": ("gold", ramp("503008", "845610", "ac7816", "d4a022", "f0c842", "fff09c")),
    "malachite": ("gold", ramp("0a281c", "113e2b", "216c46", "2e8654", "4aa868", "7ecc92")),
    "limonite": ("gold", ramp("281a0e", "46301a", "684828", "8a6234", "aa8048", "ccaa70")),
    "bauxite": ("gold", ramp("3c1a14", "622e24", "7e3e2e", "985640", "b2765a", "d4aa88")),
    "galena": ("gold", ramp("1a1c22", "33363f", "4e525d", "6c717e", "a0a6b2", "d6dce8")),
    "magnetite": ("iron", ramp("08080a", "151518", "202025", "303137", "50535c", "767a86")),
    "cassiterite": ("iron", ramp("120c0a", "261b15", "38271d", "543a28", "7a5a40", "c4aa92")),
    "nepheline": ("iron", ramp("4e222a", "803e42", "ae605c", "c4746c", "eeb49e", "ffe0cc")),
    "pentlandite": ("iron", ramp("30281a", "5a4e30", "786a40", "988a56", "b8ac74", "dcd4a4")),
    "lepidolite": ("iron", ramp("34263e", "54425e", "6c5876", "867290", "a896b2", "cec2d6", "f2e8f8")),
    "native_copper": ("copper", ramp("38180e", "602a16", "8c4022", "b86034", "de8852", "f4ba8c")),
    "hematite": ("copper", ramp("1e1010", "341c1c", "4e2622", "6c342c", "924a3a", "c47c68")),
    "tetrahedrite": ("copper", ramp("101114", "34373e", "4e525c", "707682", "a8b0be")),
    "sphalerite": ("spotted", ramp("16161a", "2e3136", "40444a", "5c6168", "868c94", "c4cad2")),
    "native_bismuth": ("spotted", ramp("28222c", "344564", "4a6590", "6296d6", "a8c8f0")),
}
# Вкрапления (буквы в форме); у руд без них буквы берут основную гамму
ACCENTS = {
    "native_copper": ramp("326c50", "468a64", "6eaa82"),
    "hematite": ramp("8c2824", "aa3c30"),
    "tetrahedrite": ramp("7a6440", "ba9660"),
    "sphalerite": ramp("5c3e1c", "805822", "a87a2e", "d2a64e"),
    "native_bismuth": ramp("5d375f", "884f88", "ce70c8", "eabe5c"),
}

NUGGET_ORES = ["native_copper", "malachite", "tetrahedrite", "cassiterite", "sphalerite"]

# Шихта: перекраска образца tin_bronze_mix (медные крупинки и серые крупинки) — гаммы для «медной» и «серой» части
METALS = {
    "copper": ramp("5a2a14", "8c4022", "b86034", "de8852", "f4ba8c"),
    "tin": ramp("4a4c50", "6e7278", "9a9ea4", "c4c8cc", "e4e8ec"),
    "zinc": ramp("3e4652", "5e6a78", "8492a0", "aab6c2", "cfd8e0"),
}
MIXES = {
    "copper_mix": ("copper", "copper"),
    "tin_mix": ("tin", "tin"),
    "zinc_mix": ("zinc", "zinc"),
    "brass_mix": ("copper", "zinc"),
}


def pick(colors, level, top=9):
    return colors[round(level * (len(colors) - 1) / top)]


def paint(rows, base, accent=None):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch == ".":
                continue
            if ch.isdigit():
                c = pick(base, int(ch))
            else:
                level = "abcdefghij".index(ch)
                c = pick(accent, level) if accent else pick(base, level)
            img.putpixel((x, y), c + (255,))
    return img


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def recolor_by_rank(img, colors):
    """Каждый цвет образца заменяется цветом гаммы того же ранга по яркости."""
    present = sorted({p[:3] for p in img.getdata() if p[3]}, key=lum)
    out = img.copy()
    for y in range(img.height):
        for x in range(img.width):
            p = img.getpixel((x, y))
            if p[3]:
                rank = present.index(p[:3]) / max(1, len(present) - 1)
                out.putpixel((x, y), pick(colors, rank * 9) + (p[3],))
    return out


def save(img, rel):
    img.save(os.path.join(ROOT, *rel.split("/")) + ".png")


def raws():
    for ore, (shape, base) in RAMPS.items():
        save(paint(SHAPES[shape], base, ACCENTS.get(ore)), f"item/raw_{ore}")


def nuggets():
    pebble = Image.open(os.path.join(ROOT, "block", "flint_pebble.png")).convert("RGBA")
    for ore in NUGGET_ORES:
        base = RAMPS[ore][1]
        save(paint(NUGGET, base), f"item/{ore}_nugget")
        save(recolor_by_rank(pebble, base), f"block/{ore}_nugget")


def mixes():
    sample = Image.open(os.path.join(ROOT, "item", "tin_bronze_mix.png")).convert("RGBA")
    outline = sample.getpixel((0, 0))
    for name, (warm, cool) in MIXES.items():
        out = sample.copy()
        for y in range(16):
            for x in range(16):
                p = sample.getpixel((x, y))
                if not p[3] or p == outline or lum(p) < 45:
                    continue
                coppery = p[0] > p[2] + 30
                colors = METALS[warm if coppery else cool]
                # Яркость крупинки сохраняется: 60..200 растягивается на всю гамму
                level = max(0.0, min(9.0, (lum(p) - 60) / 140 * 9))
                out.putpixel((x, y), pick(colors, level) + (255,))
        save(out, f"item/{name}")


if __name__ == "__main__":
    raws()
    nuggets()
    mixes()
    print("ok")

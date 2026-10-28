"""
Костёр TrailCraft (block/TrailCampfireBlock.java): текстуры, модели, блокстейт, модели предметов (костёр, зола),
переводы и превью текстур docs/textures/campfire.png.

Запуск из корня проекта:  python scripts/gen_campfire.py   (нужен Pillow)

Модель смотрит на юг, блокстейт поворачивает её по facing (y = facing.toYRot()). Снизу слой золы (1 пиксель), на нём
0–4 тонких бревна 4×4 длиной в блок, как у ванильного костра: два нижних вдоль z (x 1–5 и 11–15, высота 1–5), два
верхних поперёк (z 11–15 — третье, дальнее от того, кто сложил; z 1–5 — четвёртое; высота 4–8). Над пересечениями
брёвен — слоты еды (TrailCampfireBlock.SLOTS); меняя брёвна здесь, менять и там.
Горящий костёр: обугленные брёвна с тлеющими трещинами, тлеющая зола и ванильный огонь костра
(minecraft:block/campfire_fire). Тарелочка под едой — campfire_plate (6×6 в середине текстуры, рисует
client/TrailCampfireRenderer). Кора брёвен — из коры дуба (textures/block/oak_log.png), торец — как у тонкого дуба.
Зола-предмет — кучка пыли по шаблону scripts/templates/dust.png (dust() из gen_ore_textures.py).
"""
import json
import os
import random

from PIL import Image

from gen_ore_textures import dust, ramp

NS = "trailcraft"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")
TEX = os.path.join(ROOT, "assets", NS, "textures")
DOCS = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "docs", "textures")
MAX_LOGS = 4
# facing → поворот модели (как Direction.toYRot)
FACINGS = {"south": 0, "west": 90, "north": 180, "east": 270}

LANG = {
    "block.trailcraft.campfire": ("Костёр", "Campfire"),
    "item.trailcraft.ash": ("Зола", "Ash"),
}


def hx(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) + (255,)


def shade(c, k):
    return tuple(min(255, round(v * k)) for v in c[:3]) + (c[3],)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def save(img, rel):
    p = os.path.join(TEX, *rel.split("/")) + ".png"
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)


# ---------------------------------------------------------------- текстуры
# Развёртка бревна (16×16): строки 0–3 — кора (бок), 4–7 — кора другого места ствола (низ и второй бок),
# x 0–3 строки 8–11 — торец. Остальное прозрачно.
SIDE_A, SIDE_B, END = (0, 0, 16, 4), (0, 4, 16, 8), (0, 8, 4, 12)
# Торец тонкого бревна: B/b — кора, L/M — светлая и средняя древесина, D — сердцевина
END_ROWS = ["BbbB", "bLMb", "bMDb", "BbbB"]
END_COLORS = {"B": "#3d2e19", "b": "#614b2b", "L": "#b4925a", "M": "#a3814d", "D": "#8a6a3b"}
# Обугленный торец тлеет изнутри
END_LIT_COLORS = {"B": "#140f0c", "b": "#2a201b", "L": "#ffc34a", "M": "#ff8c2a", "D": "#c4401a"}
# Уголь коры по яркости (от тёмного к светлому) и тлеющие трещины (от края к середине)
CHAR = [hx(c) for c in ("#140f0c", "#1f1814", "#2b211b", "#382b22", "#46362a")]
EMBER = [hx(c) for c in ("#7a200c", "#c4401a", "#ff8c2a", "#ffc34a")]


def bark_strips():
    """Две полосы коры 16×4 из коры дуба: волокна вдоль бревна, верхняя строка светлее, нижняя темнее."""
    oak = Image.open(os.path.join(TEX, "block", "oak_log.png")).convert("RGBA").rotate(90)
    strips = []
    for top in (0, 8):
        strip = Image.new("RGBA", (16, 4))
        for y, k in enumerate((1.08, 1.0, 0.94, 0.84)):
            for x in range(16):
                strip.putpixel((x, y), shade(oak.getpixel((x, top + y)), k))
        strips.append(strip)
    return strips


def end(colors):
    img = Image.new("RGBA", (4, 4))
    for y, row in enumerate(END_ROWS):
        for x, ch in enumerate(row):
            img.putpixel((x, y), hx(colors[ch]))
    return img


def log_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    a, b = bark_strips()
    img.paste(a, SIDE_A[:2])
    img.paste(b, SIDE_B[:2])
    img.paste(end(END_COLORS), END[:2])
    return img


def char(strip, seed):
    """Обугленная кора: яркость коры — в гамму угля, по волокнам — тлеющие трещины в 2–4 пикселя."""
    rnd = random.Random(seed)
    levels = sorted({lum(strip.getpixel((x, y))) for y in range(4) for x in range(16)})
    out = Image.new("RGBA", strip.size)
    for y in range(4):
        for x in range(16):
            rank = levels.index(lum(strip.getpixel((x, y)))) / max(1, len(levels) - 1)
            out.putpixel((x, y), CHAR[min(len(CHAR) - 1, int(rank * len(CHAR)))])
    for _ in range(4):
        y, length = rnd.randrange(4), rnd.randint(2, 4)
        x0 = rnd.randrange(16 - length + 1)
        hot = rnd.choice((1, 2, 2, 3))  # самая горячая точка трещины
        for i in range(length):
            edge = i in (0, length - 1)
            out.putpixel((x0 + i, y), EMBER[max(0, hot - 1) if edge else hot])
    return out


def log_lit_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    a, b = bark_strips()
    img.paste(char(a, 11), SIDE_A[:2])
    img.paste(char(b, 12), SIDE_B[:2])
    img.paste(end(END_LIT_COLORS), END[:2])
    return img


ASH = [hx(c) for c in ("#4e4a46", "#625e59", "#76716b", "#89847d", "#9a948c", "#aaa49b")]
COAL = [hx(c) for c in ("#141210", "#22201c", "#302c28")]


def ash_texture(lit):
    """Зола 16×16: пепел пятнами светлее и темнее, угольки; тлеющая — темнее, с жаром кучками. Нижняя строка —
    бок слоя золы."""
    rnd = random.Random(7 if lit else 5)
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            level = rnd.choice((2, 3, 3, 3, 4, 4, 5))
            c = ASH[max(0, level - 2)] if lit else ASH[level]
            img.putpixel((x, y), c)
    for _ in range(9):  # угольки по 1–2 пикселя
        x, y = rnd.randrange(16), rnd.randrange(16)
        img.putpixel((x, y), COAL[rnd.randrange(len(COAL))])
        if rnd.random() < 0.5:
            img.putpixel(((x + 1) % 16, y), COAL[0])
    if lit:
        for _ in range(8):  # жар: красное по краю, оранжевое и жёлтое в середине
            cx, cy = rnd.randrange(16), rnd.randrange(16)
            for dx, dy, level in ((0, 0, 2), (1, 0, 1), (-1, 0, 0), (0, 1, 0)):
                if level == 2 and rnd.random() < 0.3:
                    level = 3
                img.putpixel(((cx + dx) % 16, (cy + dy) % 16), EMBER[level])
    return img


# Глиняная тарелочка 6×6 (сверху): o — контур, r — бортик, h — блик бортика, b — дно, d — тень на дне
PLATE = [
    ".oooo.",
    "ohhhro",
    "ohbbro",
    "orbdro",
    "orrrro",
    ".oooo.",
]
PLATE_COLORS = {"o": "#5c2e18", "r": "#b8693e", "h": "#d4895a", "b": "#a65a34", "d": "#8e4a2a"}


def plate_texture():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(PLATE):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((5 + x, 5 + y), hx(PLATE_COLORS[ch]))
    return img


# Зола-предмет: кучка пыли — пепел с крупинками угля
ASH_DUST = ramp("2a2826", "4a4744", "6e6a66", "8f8a84", "b2ada6", "d2cec8")
COAL_DUST = ramp("101010", "1c1a18", "262422", "302d2a", "3a3633", "46423e")


def textures():
    save(log_texture(), "block/campfire_log")
    save(log_lit_texture(), "block/campfire_log_lit")
    save(ash_texture(False), "block/campfire_ash")
    save(ash_texture(True), "block/campfire_ash_lit")
    save(plate_texture(), "block/campfire_plate")
    save(dust(ASH_DUST, [COAL_DUST], 0.15, 9), "item/ash")


# ---------------------------------------------------------------- модели
def uv(rect):
    return list(rect)


def log_along_z(x0, y0):
    """Нижнее бревно вдоль z: бока — кора, верх и низ — кора (повёрнута вдоль бревна), торцы — на краях блока."""
    return {"from": [x0, y0, 0], "to": [x0 + 4, y0 + 4, 16], "faces": {
        "north": {"uv": uv(END), "texture": "#log", "cullface": "north"},
        "south": {"uv": uv(END), "texture": "#log", "cullface": "south"},
        "east": {"uv": uv(SIDE_A), "texture": "#log"},
        "west": {"uv": uv(SIDE_B), "texture": "#log"},
        "up": {"uv": uv(SIDE_A), "rotation": 90, "texture": "#log"},
        "down": {"uv": uv(SIDE_B), "rotation": 90, "texture": "#log", "cullface": "down"},
    }}


def log_along_x(z0, y0):
    return {"from": [0, y0, z0], "to": [16, y0 + 4, z0 + 4], "faces": {
        "east": {"uv": uv(END), "texture": "#log", "cullface": "east"},
        "west": {"uv": uv(END), "texture": "#log", "cullface": "west"},
        "north": {"uv": uv(SIDE_B), "texture": "#log"},
        "south": {"uv": uv(SIDE_A), "texture": "#log"},
        "up": {"uv": uv(SIDE_A), "texture": "#log"},
        "down": {"uv": uv(SIDE_B), "texture": "#log"},
    }}


# Порядок, в котором брёвна кладут и (с конца) выгорают: два нижних, потом верхние — дальнее, ближнее
LOGS = [log_along_z(1, 1), log_along_z(11, 1), log_along_x(11, 4), log_along_x(1, 4)]
ASH_ELEMENT = {"from": [1, 0, 1], "to": [15, 1, 15], "faces": {
    "up": {"uv": [1, 1, 15, 15], "texture": "#ash"},
    "down": {"uv": [1, 1, 15, 15], "texture": "#ash", "cullface": "down"},
    "north": {"uv": [1, 15, 15, 16], "texture": "#ash"},
    "south": {"uv": [1, 15, 15, 16], "texture": "#ash"},
    "east": {"uv": [1, 15, 15, 16], "texture": "#ash"},
    "west": {"uv": [1, 15, 15, 16], "texture": "#ash"},
}}
# Ванильный огонь костра — две плоскости крест-накрест
FIRE = [
    {"from": [0.8, 1, 8], "to": [15.2, 17, 8], "shade": False,
     "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]},
     "faces": {"north": {"uv": [16, 0, 0, 16], "texture": "#fire"}, "south": {"uv": [0, 0, 16, 16], "texture": "#fire"}}},
    {"from": [8, 1, 0.8], "to": [8, 17, 15.2], "shade": False,
     "rotation": {"angle": 45, "axis": "y", "origin": [8, 8, 8]},
     "faces": {"west": {"uv": [16, 0, 0, 16], "texture": "#fire"}, "east": {"uv": [0, 0, 16, 16], "texture": "#fire"}}},
]


def model_name(logs, lit):
    return f"campfire_{logs}" + ("_lit" if lit else "")


def models():
    for logs in range(MAX_LOGS + 1):
        for lit in (False, True):
            textures_ = {"particle": f"{NS}:block/campfire_log",
                         "log": f"{NS}:block/campfire_log" + ("_lit" if lit else ""),
                         "ash": f"{NS}:block/campfire_ash" + ("_lit" if lit else "")}
            elements = [ASH_ELEMENT] + LOGS[:logs]
            if lit and logs > 0:
                textures_["fire"] = "minecraft:block/campfire_fire"
                elements = elements + FIRE
            write(f"assets/{NS}/models/block/{model_name(logs, lit)}.json", {
                "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                "textures": textures_, "elements": elements})
    variants = {}
    for facing, y in FACINGS.items():
        for lit in (False, True):
            for logs in range(MAX_LOGS + 1):
                v = {"model": f"{NS}:block/{model_name(logs, lit)}"}
                if y:
                    v["y"] = y
                variants[f"facing={facing},lit={str(lit).lower()},logs={logs}"] = v
    write(f"assets/{NS}/blockstates/campfire.json", {"variants": variants})
    write(f"assets/{NS}/models/item/campfire.json", {"parent": f"{NS}:block/{model_name(MAX_LOGS, False)}"})
    write(f"assets/{NS}/models/item/ash.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/ash"}})


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        with open(os.path.join(ROOT, *rel.split("/")), encoding="utf-8") as f:
            data = json.load(f)
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


def preview():
    """Лист текстур: бревно, горящее бревно, зола, тлеющая зола, тарелочка, зола-предмет."""
    names = ["block/campfire_log", "block/campfire_log_lit", "block/campfire_ash", "block/campfire_ash_lit",
             "block/campfire_plate", "item/ash"]
    scale, pad = 8, 8
    sheet = Image.new("RGBA", (pad + len(names) * (16 * scale + pad), 16 * scale + 2 * pad), (60, 60, 60, 255))
    for i, name in enumerate(names):
        img = Image.open(os.path.join(TEX, *name.split("/")) + ".png").convert("RGBA")
        sheet.alpha_composite(img.resize((16 * scale, 16 * scale), Image.NEAREST), (pad + i * (16 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "campfire.png"))


if __name__ == "__main__":
    textures()
    models()
    lang()
    preview()
    print("ok")

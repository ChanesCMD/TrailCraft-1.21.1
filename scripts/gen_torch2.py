"""Факел: гладкая палка, без лишнего пикселя; своя модель с поджатыми плоскостями; луна с прозрачным фоном."""
import json
import os
import sys
from collections import deque

from PIL import Image

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")
TEX = os.path.join(ASSETS, "textures", "block")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def torch_texture():
    """Палка без полос: слева светлая грань, справа тёмная, плавный переход; сверху джутовый уголёк."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    light_top, light_bottom = (212, 176, 122), (143, 106, 58)
    dark_top, dark_bottom = (107, 76, 40), (69, 48, 26)

    def lerp(a, b, t):
        return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))

    rows = list(range(9, 16))
    for i, y in enumerate(rows):
        t = i / (len(rows) - 1)
        px[7, y] = lerp(light_top, light_bottom, t) + (255,)
        px[8, y] = lerp(dark_top, dark_bottom, t) + (255,)
    for y, tone in {6: (236, 214, 152), 7: (206, 174, 106), 8: (172, 138, 74)}.items():
        for x in (7, 8):
            px[x, y] = tone + (255,)
    img.save(os.path.join(TEX, "torch.png"))


def torch_model():
    """Своя модель: плоскости креста поджаты на 0,5 пикселя, чтобы не совпадать с гранями соседних блоков."""
    write(os.path.join(ASSETS, "models", "block", "torch.json"), {
        "ambientocclusion": False,
        "render_type": "minecraft:cutout",
        "textures": {"particle": "trailcraft:block/torch"},
        "elements": [
            {"from": [7, 0, 7], "to": [9, 10, 9], "shade": False,
             "faces": {"down": {"uv": [7, 13, 9, 15], "texture": "#torch"},
                       "up": {"uv": [7, 6, 9, 8], "texture": "#torch"}}},
            {"from": [7, 0, 0.5], "to": [9, 16, 15.5], "shade": False,
             "faces": {"west": {"uv": [0, 0, 16, 16], "texture": "#torch"},
                       "east": {"uv": [0, 0, 16, 16], "texture": "#torch"}}},
            {"from": [0.5, 0, 7], "to": [15.5, 16, 9], "shade": False,
             "faces": {"north": {"uv": [0, 0, 16, 16], "texture": "#torch"},
                       "south": {"uv": [0, 0, 16, 16], "texture": "#torch"}}},
        ],
    })


def moon_transparent(src: str):
    """Возвращает присланную лунную текстуру, но фон делает прозрачным (заливка от углов)."""
    img = Image.open(src).convert("RGBA")
    w, h = img.size
    px = img.load()
    bg = px[0, 0][:3]

    def close(c):
        return abs(c[0] - bg[0]) + abs(c[1] - bg[1]) + abs(c[2] - bg[2]) < 60

    seen = set()
    queue = deque()
    for x in range(w):
        queue.append((x, 0))
        queue.append((x, h - 1))
    for y in range(h):
        queue.append((0, y))
        queue.append((w - 1, y))
    while queue:
        x, y = queue.popleft()
        if (x, y) in seen or not (0 <= x < w and 0 <= y < h):
            continue
        seen.add((x, y))
        r, g, b, a = px[x, y]
        if not close((r, g, b)):
            continue
        px[x, y] = (r, g, b, 0)
        queue.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    out = os.path.join(ASSETS, "textures", "environment", "moon_phases_06.png")
    img.save(out)
    print("луна:", img.size)


def main():
    torch_texture()
    torch_model()
    src = sys.argv[1] if len(sys.argv) > 1 else None
    if src:
        moon_transparent(src)
    else:
        print("файл луны не указан — оставляю прежний")


if __name__ == "__main__":
    main()

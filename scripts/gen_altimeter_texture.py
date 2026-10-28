"""Текстура латунного высотомера 16x16 с вертикальной шкалой (нужен Pillow).

Запуск из корня проекта:  python scripts/gen_altimeter_texture.py — пишет textures/item/brass_altimeter.png.
"""
import os

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ITEM = os.path.join(ROOT, "src/main/resources/assets/trailcraft/textures/item/brass_altimeter.png")

PALETTE = {
    # латунь от тёмной к светлой
    "0": (66, 42, 14), "1": (122, 84, 28), "2": (170, 126, 44), "3": (210, 168, 70), "4": (240, 214, 124),
    # шкала: тень, фон, риски
    "a": (196, 186, 156), "b": (232, 224, 200), "t": (70, 62, 50),
    # указатель / столбик
    "r": (176, 40, 32), "R": (214, 72, 56),
    # стекло (пузырёк уровня)
    "g": (120, 160, 120), "G": (190, 225, 180),
}

# Прямоугольный корпус с ушком, окно со шкалой, красная черта уровня
ALTIMETER = [
    "......0330......",
    ".....03..10.....",
    "......0110......",
    "....00000000....",
    "....04443320....",
    "....03aaaa10....",
    "....03ttbb10....",
    "....03tbbb10....",
    "....03tbbb10....",
    "....03ttrr10....",
    "....03tbbb10....",
    "....03tbbb10....",
    "....03ttbb10....",
    "....03tbbb10....",
    "....02111110....",
    "....00000000....",
]


def render(rows):
    assert len(rows) == 16 and all(len(r) == 16 for r in rows), rows
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                img.putpixel((x, y), PALETTE[ch] + (255,))
    return img


def main():
    render(ALTIMETER).save(ITEM)
    print("ok ->", ITEM)


if __name__ == "__main__":
    main()

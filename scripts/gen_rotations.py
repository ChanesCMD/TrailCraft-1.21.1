"""
Случайный поворот текстур, как у ванильных блоков: дёрн — как трава (поворот вокруг вертикали), почвы — как земля
(поворот вокруг вертикали), песок и гравий — тоже поворот вокруг вертикали, породы — как камень (модель и её зеркальная копия, повёрнутые на 0° и 180°).
Пишет блокстейты (и зеркальные модели пород <порода>_mirrored).

Запуск из корня проекта:  python scripts/gen_rotations.py
"""
import json
import os

NS = "trailcraft"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", NS)
SOILS = ["zheltozem", "burozem", "chernozem", "loam", "clay_white", "clay_blue", "clay_red", "clay_gray", "peat"]
SANDS = ["sand_gray", "sand_red", "sand_white", "sand_yellow"]
GRAVELS = [f"{r}_gravel" for r in ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite",
                                     "gray_granite", "limestone", "marble", "sandstone", "shale"]]
ROCKS = ["andesite", "basalt", "chalk", "diorite", "dolomite", "gabbro", "granite", "gray_granite", "marble",
         "sandstone", "shale"]


def write(rel, data):
    p = os.path.join(ROOT, *rel.split("/"))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def turned(model):
    """Как ванильные земля и трава: четыре поворота вокруг вертикали."""
    return {"variants": {"": [{"model": model}] + [{"model": model, "y": y} for y in (90, 180, 270)]}}


def main():
    for soil in SOILS:
        write(f"blockstates/{soil}.json", turned(f"{NS}:block/{soil}"))
        write(f"blockstates/{soil}_grass.json", turned(f"{NS}:block/{soil}_grass"))
    for block in SANDS + GRAVELS:
        write(f"blockstates/{block}.json", turned(f"{NS}:block/{block}"))
    for rock in ROCKS:
        write(f"models/block/{rock}_mirrored.json",
              {"parent": "minecraft:block/cube_mirrored_all", "textures": {"all": f"{NS}:block/{rock}"}})
        model, mirrored = f"{NS}:block/{rock}", f"{NS}:block/{rock}_mirrored"
        write(f"blockstates/{rock}.json", {"variants": {"": [
            {"model": model}, {"model": mirrored}, {"model": model, "y": 180}, {"model": mirrored, "y": 180}]}})
    print("ok")


if __name__ == "__main__":
    main()

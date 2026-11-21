"""Факел на стену: блок wall_torch, модель, блокстейт и render_type cutout (иначе чёрные квадраты)."""
import json
import os

ROOT = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "trailcraft")


def write(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def main():
    # стоячий факел: cutout обязателен, иначе прозрачные пиксели рисуются чёрным
    write(os.path.join(ASSETS, "models", "block", "torch.json"), {
        "parent": "minecraft:block/torch",
        "render_type": "minecraft:cutout",
        "textures": {"particle": "trailcraft:block/torch", "torch": "trailcraft:block/torch"},
    })
    # настенный
    write(os.path.join(ASSETS, "models", "block", "wall_torch.json"), {
        "parent": "minecraft:block/template_torch_wall",
        "render_type": "minecraft:cutout",
        "textures": {"torch": "trailcraft:block/torch"},
    })
    variants = {}
    for facing in ("north", "east", "south", "west"):
        y = {"north": 270, "east": 0, "south": 90, "west": 180}[facing]
        variants[f"facing={facing}"] = {"model": "trailcraft:block/wall_torch", "y": y}
    write(os.path.join(ASSETS, "blockstates", "wall_torch.json"), {"variants": variants})
    write(os.path.join(ASSETS, "models", "item", "wall_torch.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": "trailcraft:block/torch"}})
    for code, text in (("ru_ru", "Настенный факел"), ("en_us", "Wall Torch")):
        path = os.path.join(ASSETS, "lang", code + ".json")
        data = json.load(open(path, encoding="utf-8"))
        data["block.trailcraft.wall_torch"] = text
        with open(path, "w", encoding="utf-8", newline="\n") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
            f.write("\n")
    print("ok")


if __name__ == "__main__":
    main()

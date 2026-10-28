"""
Опоры (block/SupportBlock.java, item/SupportItem.java, обвалы — collapse/Collapse.java) и молоток штейгера
(item/SteigerHammerItem.java): модели, блокстейты, модели предметов, лут, рецепты, тэги (#trailcraft:can_collapse — что
обваливается, #trailcraft:collapsible_soils — какие почвы осыпаются, кирка/топор), текстура молотка и переводы.

Запуск из корня проекта:  python scripts/gen_supports.py   (нужен Pillow)

Стойка — брус 6×6 во всю высоту блока, перекладина — брус 6×6 у верха блока (y 10–16); плечи north/east/south/west
тянутся от середины к краю блока, к соседней опоре (одна модель плеча на север, блокстейт поворачивает). Меняя формы
здесь, менять и SupportBlock (POST, CORE, ARM_SHAPES). Текстуры опор пока ванильные: окорённый дуб и железный блок.
Молоток штейгера (каменный молоток + кремень) — черенок каменного молотка, металлическая головка с бойком и клювом
поперёк черенка.
"""
import json
import os

from PIL import Image

NS = "trailcraft"
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources")

# материал → (текстура бока, текстура торца, рецепт: ингредиент, сколько выходит)
MATERIALS = {
    "wooden": (f"{NS}:block/stripped_oak_log", f"{NS}:block/stripped_oak_log_top", {"tag": f"{NS}:thin_logs"}, 4),
    "iron": ("minecraft:block/iron_block", "minecraft:block/iron_block", {"item": "minecraft:iron_ingot"}, 2),
}
SOILS = ["zheltozem", "burozem", "chernozem", "loam", "clay_white", "clay_blue", "clay_red", "clay_gray", "peat"]
ARM_Y = {"north": 0, "east": 90, "south": 180, "west": 270}

LANG = {
    "block.trailcraft.wooden_support": ("Деревянная опора", "Wooden Support"),
    "block.trailcraft.wooden_support_beam": ("Деревянная перекладина", "Wooden Support Beam"),
    "block.trailcraft.iron_support": ("Железная опора", "Iron Support"),
    "block.trailcraft.iron_support_beam": ("Железная перекладина", "Iron Support Beam"),
    "item.trailcraft.support.frame": ("В раме держит породу и почву %s×%s×%s вокруг себя",
                                      "In a frame holds rock and soil %s×%s×%s around it"),
    "item.trailcraft.support.lone": ("Одна стойка — только %s×%s×%s", "A lone post holds only %s×%s×%s"),
    "item.trailcraft.support.column": ("Ставится столбом до %s стоек; до соседней стойки (не дальше %s блоков) "
                                       "сама ставится перекладина — получается рама",
                                       "Places a column of up to %s posts and a beam to a post up to %s blocks away, "
                                       "making a frame"),
    "item.trailcraft.support.beam": ("ПКМ по боку опоры — перекладина",
                                     "Right-click the side of a support to place a beam"),
    "item.trailcraft.steiger_hammer": ("Молоток штейгера", "Steiger's Hammer"),
    "item.trailcraft.steiger_hammer.desc": ("В руке: под нависающей породой зелёные точки — держит опора, красные — "
                                            "может обвалиться", "In hand: green dots under overhanging rock are "
                                            "supported, red ones may collapse"),
    "item.trailcraft.steiger_hammer.desc2": ("Посмотрите на опору — видно, что она держит",
                                             "Look at a support to see what it holds"),
    "item.trailcraft.steiger_hammer.framed": ("Опора в раме: держит %s×%s×%s", "Framed support: holds %s×%s×%s"),
    "item.trailcraft.steiger_hammer.lone": ("Опора без рамы: держит только %s×%s×%s — поставьте вторую стойку и перекладину",
                                            "Lone support: holds only %s×%s×%s — add a second post and a beam"),
}


def path(rel):
    return os.path.join(ROOT, *rel.split("/"))


def read(rel, default):
    try:
        with open(path(rel), encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        return default


def write(rel, data):
    p = path(rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def tag(kind, tag_id, values):
    ns, name = tag_id.split(":")
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    data = read(rel, {"replace": False, "values": []})
    for v in values:
        if v not in data["values"]:
            data["values"].append(v)
    write(rel, data)


def faces(side_uv, end_uv, skip=(), rotate=()):
    out = {}
    for f in ("north", "south", "east", "west", "up", "down"):
        if f in skip:
            continue
        face = {"uv": end_uv if f in ("up", "down") else side_uv, "texture": "#end" if f in ("up", "down") else "#side"}
        if f in rotate:
            face["rotation"] = 90
        out[f] = face
    return out


def templates():
    """Общие модели с текстурами #side и #end; у материалов — только текстуры."""
    write(f"assets/{NS}/models/block/support_post.json", {
        "parent": "minecraft:block/block", "textures": {"particle": "#side"},
        "elements": [{"from": [5, 0, 5], "to": [11, 16, 11], "faces": faces([5, 0, 11, 16], [5, 5, 11, 11])}]})
    write(f"assets/{NS}/models/block/support_core.json", {
        "parent": "minecraft:block/block", "textures": {"particle": "#side"},
        "elements": [{"from": [5, 10, 5], "to": [11, 16, 11], "faces": faces([5, 0, 11, 6], [5, 5, 11, 11])}]})
    # плечо к северу: торец у края блока не виден — к нему примыкает соседняя крепь; волокна вдоль плеча
    arm = faces([0, 5, 5, 11], [5, 0, 11, 5], skip=("north", "south"), rotate=("east", "west"))
    for f in ("up", "down"):
        arm[f]["texture"] = "#side"
    write(f"assets/{NS}/models/block/support_arm.json", {
        "parent": "minecraft:block/block", "textures": {"particle": "#side"},
        "elements": [{"from": [5, 10, 0], "to": [11, 16, 5], "faces": arm}]})


def multipart(base, arm):
    parts = [{"apply": {"model": base}}]
    for d, y in ARM_Y.items():
        apply = {"model": arm}
        if y:
            apply["y"] = y
        parts.append({"when": {d: "true"}, "apply": apply})
    return {"multipart": parts}


def loot(block, item):
    write(f"data/{NS}/loot_table/blocks/{block}.json", {
        "type": "minecraft:block",
        "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{item}"}],
                   "conditions": [{"condition": "minecraft:survives_explosion"}]}],
        "random_sequence": f"{NS}:blocks/{block}"})


def materials():
    for mat, (side, end, ingredient, count) in MATERIALS.items():
        name = f"{mat}_support"
        textures = {"side": side, "end": end, "particle": side}
        for part in ("post", "core", "arm"):
            write(f"assets/{NS}/models/block/{name}_{part}.json", {"parent": f"{NS}:block/support_{part}", "textures": textures})
        write(f"assets/{NS}/blockstates/{name}.json", multipart(f"{NS}:block/{name}_post", f"{NS}:block/{name}_arm"))
        write(f"assets/{NS}/blockstates/{name}_beam.json", multipart(f"{NS}:block/{name}_core", f"{NS}:block/{name}_arm"))
        write(f"assets/{NS}/models/item/{name}.json", {"parent": f"{NS}:block/{name}_post"})
        loot(name, name)
        loot(f"{name}_beam", name)
        write(f"data/{NS}/recipe/{name}.json", {
            "type": "minecraft:crafting_shaped", "category": "building",
            "key": {"M": ingredient}, "pattern": ["M", "M"],
            "result": {"id": f"{NS}:{name}", "count": count}})
    tag("block", "minecraft:mineable/axe", [f"{NS}:wooden_support", f"{NS}:wooden_support_beam"])
    tag("block", "minecraft:mineable/pickaxe", [f"{NS}:iron_support", f"{NS}:iron_support_beam"])


def steiger_hammer():
    """Черенок каменного молотка и поперёк него металлическая головка: боёк вверху слева, клюв внизу справа."""
    tex = os.path.join(ROOT, "assets", NS, "textures", "item")
    src = Image.open(os.path.join(tex, "stone_hammer.png")).convert("RGBA")
    img = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            c = src.getpixel((x, y))
            if c[3] and c[0] - c[2] > 25:  # только черенок
                img.putpixel((x, y), c)
    outline, dark, mid, light = (40, 40, 46, 255), (92, 94, 102, 255), (140, 143, 150, 255), (196, 199, 205, 255)
    head = {}
    for i in range(9):
        x, y = 6 + i, 1 + i
        head[(x, y)], head[(x, y + 1)] = mid, dark
        if i < 6:
            head[(x - 1, y + 1)] = light if i < 3 else mid
    for p in [(4, 1), (5, 0), (6, 0), (4, 2), (4, 3), (5, 3)]:
        head[p] = mid
    for p in [(5, 1), (5, 2), (6, 1)]:
        head[p] = light
    for p in [(14, 10), (13, 10), (14, 11)]:  # клюв сужается
        head.pop(p, None)
    for p, c in head.items():
        img.putpixel(p, c)
    filled = {(x, y) for x in range(16) for y in range(16) if img.getpixel((x, y))[3]}
    for x, y in list(head):
        for q in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)):
            if 0 <= q[0] < 16 and 0 <= q[1] < 16 and q not in filled:
                img.putpixel(q, outline)
                filled.add(q)
    img.save(os.path.join(tex, "steiger_hammer.png"))
    write(f"assets/{NS}/models/item/steiger_hammer.json",
          {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{NS}:item/steiger_hammer"}})
    write(f"data/{NS}/recipe/steiger_hammer.json", {
        "type": "minecraft:crafting_shapeless", "category": "equipment",
        "ingredients": [{"item": f"{NS}:stone_hammer"}, {"item": f"{NS}:flint"}],
        "result": {"id": f"{NS}:steiger_hammer", "count": 1}})


def tags():
    tag("block", f"{NS}:can_collapse", [f"#{NS}:rocks", f"#{NS}:ores"])
    tag("block", f"{NS}:collapsible_soils",
        [f"{NS}:{s}" for s in SOILS] + [f"{NS}:silt"] + [f"{NS}:{s}_grass" for s in SOILS])


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        data = read(rel, {})
        for key, texts in LANG.items():
            data[key] = texts[k]
        write(rel, data)


if __name__ == "__main__":
    templates()
    materials()
    steiger_hammer()
    tags()
    lang()
    print("ok")

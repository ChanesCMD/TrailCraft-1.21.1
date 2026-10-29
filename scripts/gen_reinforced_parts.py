"""
Укреплённые жидкостные детали Create (init/ModReinforced): помпа, умная труба, вентиль, ручка вентиля, шкив шланга,
носик, слив, интерфейс жидкостей, бак, и укреплённые трубы — обычная, с окошком, в корпусе (PIPES). Лава их не прожигает
(pipe/LavaPipes).

Пишет: блокстейты и модели — копии блокстейтов и моделей Create (с родителями из Create), где медные текстуры заменены
своими (textures/block/reinforced/...: медь перекрашена по яркости в тёмную гамму прочного листа, латунь, стекло и
прочее остаются), модели предметов, лут, рецепты (деталь Create и прочный лист; бак — 2 листа), тэг кирки, переводы,
превью docs/textures/reinforced_parts.png. Ручка вентиля — модели чёрной ручки Create как есть (её рисует рендер
Create по цвету блока). Ещё — наши текстуры для моделей парового двигателя и свистка Create: на укреплённом баке их
подменяет client/model/ReinforcedAttachmentModel (attachments()).

Запуск из корня проекта:  python scripts/gen_reinforced_parts.py   (нужен Pillow и jar Create в кэше Gradle)
"""
import colorsys
import json
import os
import zipfile
from io import BytesIO

from PIL import Image

from gen_create_compat import find_jar

NS = "trailcraft"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "src", "main", "resources")
DOCS = os.path.join(HERE, "..", "docs", "textures")

# наш id -> id детали Create, ru, en, сколько прочных листов в рецепте
PARTS = {
    "reinforced_mechanical_pump": ("mechanical_pump", "Укреплённая механическая помпа", "Reinforced Mechanical Pump", 1),
    "reinforced_smart_fluid_pipe": ("smart_fluid_pipe", "Укреплённая умная жидкостная труба", "Reinforced Smart Fluid Pipe", 1),
    "reinforced_fluid_valve": ("fluid_valve", "Укреплённый жидкостный вентиль", "Reinforced Fluid Valve", 1),
    "reinforced_valve_handle": ("black_valve_handle", "Укреплённая ручка вентиля", "Reinforced Valve Handle", 1),
    "reinforced_hose_pulley": ("hose_pulley", "Укреплённый шкив шланга", "Reinforced Hose Pulley", 1),
    "reinforced_spout": ("spout", "Укреплённый носик", "Reinforced Spout", 1),
    "reinforced_item_drain": ("item_drain", "Укреплённый слив", "Reinforced Item Drain", 1),
    "reinforced_portable_fluid_interface": ("portable_fluid_interface", "Укреплённый переносной интерфейс жидкостей",
                                            "Reinforced Portable Fluid Interface", 1),
    "reinforced_fluid_tank": ("fluid_tank", "Укреплённый жидкостный бак", "Reinforced Fluid Tank", 2),
}
# Укреплённые трубы (pipe/ReinforcedPipeBlock и её виды): наш id -> труба Create, ru, en, есть ли свой предмет. Рецепт и
# лут обычной — scripts/gen_reinforced_pipe.py; стеклянная и в корпусе роняют её
PIPES = {
    "reinforced_fluid_pipe": ("fluid_pipe", "Укреплённая труба", "Reinforced Fluid Pipe", True),
    "reinforced_glass_fluid_pipe": ("glass_fluid_pipe", "Укреплённая стеклянная труба", "Reinforced Glass Fluid Pipe", False),
    "reinforced_encased_fluid_pipe": ("encased_fluid_pipe", "Укреплённая труба в корпусе", "Reinforced Encased Fluid Pipe",
                                      False),
}
# Части труб, которые Create дорисовывает сам (ободки, стыки, сливы, корпус — PipeAttachmentModel): им нужны только наши
# текстуры, спрайты подменяет client/model/ReinforcedSprites
PIPE_PARTIALS = ("assets/create/models/block/fluid_pipe/", "assets/create/models/block/encased_fluid_pipe/")
# Ручку вентиля рисует рендер Create — её модели не перекрашиваются
AS_IS = {"reinforced_valve_handle"}
COPPER_RECIPE = {"reinforced_valve_handle": "create:copper_valve_handle"}  # ручка из медной: чёрная — та же медная
# Тёмная гамма прочного листа (от тёмного к светлому)
STURDY = [(22, 20, 26), (34, 32, 40), (48, 46, 56), (64, 62, 74), (84, 82, 96), (108, 106, 122), (136, 134, 150)]

jar = zipfile.ZipFile(find_jar("create-1.21.1-*-slim.jar"))
done_models, done_textures = {}, {}


def path(rel):
    return os.path.join(ROOT, *rel.split("/"))


def write(rel, data):
    p = path(rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
        f.write("\n")


def read_json(name):
    return json.loads(jar.read(name))


def copper(c, brass=False):
    """Медь (и, у свистка, латунь — иначе часть его теней становилась серой, часть оставалась жёлтой)."""
    h, s, v = colorsys.rgb_to_hsv(*(k / 255 for k in c[:3]))
    return c[3] and s > (0.1 if brass else 0.3) and (h < (0.2 if brass else 0.11) or h > 0.95)


def lum(c):
    return 0.299 * c[0] + 0.587 * c[1] + 0.114 * c[2]


def texture(ref, brass=False):
    """create:block/x -> trailcraft:block/reinforced/x, если в текстуре есть медь; иначе та же текстура Create."""
    if ref in done_textures:
        return done_textures[ref]
    ns, rel = ref.split(":") if ":" in ref else ("minecraft", ref)
    out = ref
    if ns == "create":
        name = f"assets/create/textures/{rel}.png"
        if name in jar.namelist():
            img = Image.open(BytesIO(jar.read(name))).convert("RGBA")
            px = [img.getpixel((x, y)) for y in range(img.height) for x in range(img.width)]
            cu = [c for c in px if copper(c, brass)]
            if cu:
                lo, hi = min(map(lum, cu)), max(map(lum, cu))
                res = img.copy()
                for y in range(img.height):
                    for x in range(img.width):
                        c = img.getpixel((x, y))
                        if copper(c, brass):
                            k = (lum(c) - lo) / max(1, hi - lo)
                            res.putpixel((x, y), STURDY[min(len(STURDY) - 1, int(k * len(STURDY)))] + (c[3],))
                new_rel = rel.replace("block/", "block/reinforced/", 1)
                p = path(f"assets/{NS}/textures/{new_rel}.png")
                os.makedirs(os.path.dirname(p), exist_ok=True)
                res.save(p)
                # анимированные текстуры — с тем же описанием кадров
                if f"{name}.mcmeta" in jar.namelist():
                    with open(p + ".mcmeta", "wb") as f:
                        f.write(jar.read(f"{name}.mcmeta"))
                out = f"{NS}:{new_rel}"
    done_textures[ref] = out
    return out


def model(ref):
    """Копия модели Create (и её родителей из Create) с нашими текстурами; возвращает новый id."""
    if not ref.startswith("create:"):
        return ref
    if ref in done_models:
        return done_models[ref]
    rel = ref.split(":")[1]
    new = f"{NS}:{rel.replace('block/', 'block/reinforced/', 1)}" if rel.startswith("block/") else f"{NS}:reinforced/{rel}"
    done_models[ref] = new
    data = read_json(f"assets/create/models/{rel}.json")
    if "parent" in data:
        data["parent"] = model(data["parent"] if ":" in data["parent"] else data["parent"])
    for k, v in data.get("textures", {}).items():
        if v in ("block/copper_block", "minecraft:block/copper_block"):
            v = "create:block/pipes"  # частицы медного блока у труб — частицы нашей трубы
        if not v.startswith("#"):
            data["textures"][k] = texture(v)
    if "elements" in data:
        data["render_type"] = "minecraft:cutout_mipped"
    write(f"assets/{NS}/models/{new.split(':')[1]}.json", data)
    return new


def remap_models(node, fn):
    if isinstance(node, dict):
        return {k: (fn(v) if k == "model" else remap_models(v, fn)) for k, v in node.items()}
    if isinstance(node, list):
        return [remap_models(v, fn) for v in node]
    return node


def assets():
    for ours, (theirs, *_rest) in PARTS.items():
        state = read_json(f"assets/create/blockstates/{theirs}.json")
        item = read_json(f"assets/create/models/item/{theirs}.json")
        if ours in AS_IS:
            write(f"assets/{NS}/blockstates/{ours}.json", state)
            write(f"assets/{NS}/models/item/{ours}.json", item)
            continue
        write(f"assets/{NS}/blockstates/{ours}.json", remap_models(state, model))
        item["parent"] = model(item["parent"])
        write(f"assets/{NS}/models/item/{ours}.json", item)


def attachments():
    """Перекрашенные текстуры моделей парового двигателя и свистка (сами модели остаются Create)."""
    def walk(ref, brass):
        if not ref.startswith("create:"):
            return
        m = read_json(f"assets/create/models/{ref.split(':')[1]}.json")
        for v in m.get("textures", {}).values():
            if not v.startswith("#"):
                texture(v, brass)
        if "parent" in m:
            walk(m["parent"], brass)
    for block, brass in (("steam_engine", False), ("steam_whistle", True)):
        remap_models(read_json(f"assets/create/blockstates/{block}.json"), lambda ref: walk(ref, brass) or ref)


def pipes():
    for ours, (theirs, _ru, _en, has_item) in PIPES.items():
        write(f"assets/{NS}/blockstates/{ours}.json", remap_models(read_json(f"assets/create/blockstates/{theirs}.json"), model))
        if has_item:
            item = read_json(f"assets/create/models/item/{theirs}.json")
            item["parent"] = model(item["parent"])
            write(f"assets/{NS}/models/item/{ours}.json", item)
        else:
            write(f"data/{NS}/loot_table/blocks/{ours}.json", {
                "type": "minecraft:block", "random_sequence": f"{NS}:blocks/{ours}",
                "pools": [{"rolls": 1.0, "bonus_rolls": 0.0,
                           "entries": [{"type": "minecraft:item", "name": f"{NS}:reinforced_fluid_pipe"}],
                           "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for name in sorted(jar.namelist()):
        if name.startswith(PIPE_PARTIALS) and name.endswith(".json"):
            for v in json.loads(jar.read(name)).get("textures", {}).values():
                if not v.startswith("#"):
                    texture(v)


def data():
    for ours, (theirs, _ru, _en, sheets) in PARTS.items():
        write(f"data/{NS}/loot_table/blocks/{ours}.json", {
            "type": "minecraft:block", "random_sequence": f"{NS}:blocks/{ours}",
            "pools": [{"rolls": 1.0, "bonus_rolls": 0.0, "entries": [{"type": "minecraft:item", "name": f"{NS}:{ours}"}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        base = COPPER_RECIPE.get(ours, f"create:{theirs}")
        write(f"data/{NS}/recipe/reinforced/{ours}.json", {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"item": base}] + [{"item": "create:sturdy_sheet"}] * sheets,
            "result": {"id": f"{NS}:{ours}", "count": 1}})
    rel = "data/minecraft/tags/block/mineable/pickaxe.json"
    with open(path(rel), encoding="utf-8") as f:
        tag = json.load(f)
    for ours in list(PARTS) + list(PIPES):
        if f"{NS}:{ours}" not in tag["values"]:
            tag["values"].append(f"{NS}:{ours}")
    write(rel, tag)


def lang():
    for code, k in (("ru_ru", 0), ("en_us", 1)):
        rel = f"assets/{NS}/lang/{code}.json"
        with open(path(rel), encoding="utf-8") as f:
            d = json.load(f)
        for ours, (_theirs, ru, en, _sheets) in PARTS.items():
            d[f"block.{NS}.{ours}"] = (ru, en)[k]
        for ours, (_theirs, ru, en, _item) in PIPES.items():
            d[f"block.{NS}.{ours}"] = (ru, en)[k]
        write(rel, d)


def preview():
    imgs = [Image.open(path(f"assets/{NS}/textures/{v.split(':')[1]}.png")).convert("RGBA")
            for v in sorted(set(done_textures.values())) if v.startswith(NS)]
    imgs = [i.crop((0, 0, min(i.width, 32), min(i.height, 32))) for i in imgs]
    scale, pad = 4, 4
    sheet = Image.new("RGBA", (pad + len(imgs) * (32 * scale + pad), 32 * scale + 2 * pad), (60, 60, 60, 255))
    for i, img in enumerate(imgs):
        sheet.alpha_composite(img.resize((img.width * scale, img.height * scale), Image.NEAREST),
                              (pad + i * (32 * scale + pad), pad))
    os.makedirs(DOCS, exist_ok=True)
    sheet.save(os.path.join(DOCS, "reinforced_parts.png"))


if __name__ == "__main__":
    assets()
    pipes()
    attachments()
    data()
    lang()
    preview()
    print("ok")

"""jsn-Athletic の絵・モデル・レシピ・名前"""
import json, os
from PIL import Image, ImageDraw

R = os.path.join(os.path.dirname(__file__), "..", "src/main/resources")
A = f"{R}/assets/jsn_athletic"; D = f"{R}/data/jsn_athletic"
def hx(s): s = s.lstrip("#"); return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4)) + (255,)
def save(img, path): os.makedirs(os.path.dirname(path), exist_ok=True); img.save(path)
def wj(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f: json.dump(obj, f, ensure_ascii=False, indent=2); f.write("\n")
def tex(name, fn):
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); fn(ImageDraw.Draw(im)); save(im, f"{A}/textures/block/{name}.png")

# 仕掛けの台
def mover_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#5a6070")); d.rectangle([0, 0, 15, 15], outline=hx("#30343c"))
    d.rectangle([3, 3, 12, 12], fill=hx("#40444c")); d.rectangle([6, 6, 9, 9], fill=hx("#f0c030"))
    for x in (2, 13): d.point([(x, 2), (x, 13)], fill=hx("#a0a8b8"))
def mover_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#5a6070")); d.ellipse([2, 2, 13, 13], fill=hx("#40444c"), outline=hx("#f0c030"))
    d.line([7, 3, 7, 12], fill=hx("#a0a8b8")); d.line([3, 7, 12, 7], fill=hx("#a0a8b8"))
tex("mover_side", mover_side); tex("mover_top", mover_top)
# 回るバーの台
def bar_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#6a3030")); d.rectangle([0, 0, 15, 15], outline=hx("#3a1818"))
    for i in range(0, 16, 4): d.polygon([(i, 15), (i + 2, 15), (i + 6, 9), (i + 4, 9)], fill=hx("#f0c030"))
def bar_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#6a3030")); d.ellipse([3, 3, 12, 12], fill=hx("#3a1818")); d.line([1, 7, 14, 8], fill=hx("#e04040"), width=2)
tex("spin_bar_side", bar_side); tex("spin_bar_top", bar_top)
# 落ちる床（ひびが増えていく）
import random
for st in range(3):
    def crumble(d, st=st):
        d.rectangle([0, 0, 15, 15], fill=hx("#c8b080")); d.rectangle([0, 0, 15, 15], outline=hx("#a08850"))
        rnd = random.Random(5)
        for i in range(6): d.point((rnd.randint(1, 14), rnd.randint(1, 14)), fill=hx("#b09868"))
        cracks = [[(3, 2), (6, 6), (5, 10), (8, 14)], [(12, 1), (10, 6), (13, 10)], [(1, 9), (5, 8), (9, 9), (14, 6)]]
        for c in cracks[:st + (1 if st > 0 else 0)][:st * 2]:
            d.line(c, fill=hx("#5a4020"), width=1)
        if st == 0:
            d.line([(4, 4), (11, 4)], fill=hx("#d8c090")); d.line([(4, 11), (11, 11)], fill=hx("#d8c090"))
    tex(f"crumble_{st}", crumble)
# コースの床
def plate(name, base, mark):
    def fn(d):
        d.rectangle([0, 0, 15, 15], fill=hx(base))
        for x in range(0, 16, 4):
            for y in range(0, 16, 4):
                if (x // 4 + y // 4) % 2 == 0: d.rectangle([x, y, x + 3, y + 3], fill=hx(mark))
    tex(name, fn)
plate("start_top", "#40c060", "#80f0a0"); plate("checkpoint_top", "#3080e0", "#80c0ff"); plate("goal_top", "#202020", "#f0f0f0")
tex("plate_side", lambda d: d.rectangle([0, 0, 15, 15], fill=hx("#808080")))

def cube(name, side, top, bottom=None):
    wj(f"{A}/models/block/{name}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {"side": f"jsn_athletic:block/{side}",
        "top": f"jsn_athletic:block/{top}", "bottom": f"jsn_athletic:block/{bottom or top}"}})
cube("mover", "mover_side", "mover_top"); cube("spin_bar", "spin_bar_side", "spin_bar_top")
for b in ("mover", "spin_bar"):
    wj(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"jsn_athletic:block/{b}"}}})
    wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_athletic:block/{b}"})
for st in range(3): wj(f"{A}/models/block/crumble_block_{st}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"jsn_athletic:block/crumble_{st}"}})
wj(f"{A}/models/block/crumble_block_gone.json", {"textures": {"particle": "jsn_athletic:block/crumble_2"}, "elements": []})
wj(f"{A}/blockstates/crumble_block.json", {"variants": {f"stage={s}": {"model": f"jsn_athletic:block/crumble_block_{s if s < 3 else 'gone'}"} for s in range(4)}})
wj(f"{A}/models/item/crumble_block.json", {"parent": "jsn_athletic:block/crumble_block_0"})
for b, t in (("start_line", "start_top"), ("checkpoint", "checkpoint_top"), ("goal_line", "goal_top")):
    wj(f"{A}/models/block/{b}.json", {"parent": "minecraft:block/block", "textures": {"top": f"jsn_athletic:block/{t}", "side": "jsn_athletic:block/plate_side",
        "particle": f"jsn_athletic:block/{t}"}, "elements": [{"from": [0, 0, 0], "to": [16, 2, 16], "faces": {
        "up": {"texture": "#top"}, "down": {"texture": "#side"}, **{f: {"texture": "#top", "uv": [0, 14, 16, 16]} for f in ("north", "south", "east", "west")}}}]})
    wj(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"jsn_athletic:block/{b}"}}})
    wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_athletic:block/{b}"})

ALL = ["mover", "spin_bar", "crumble_block", "start_line", "checkpoint", "goal_line"]
for b in ALL:
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_athletic:{b}"}],
                                                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
# スタートは名前（金床でつけたもの）も残す
wj(f"{D}/loot_table/blocks/start_line.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "jsn_athletic:start_line"}],
                                                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
wj(f"{R}/data/minecraft/tags/block/mineable/pickaxe.json", {"replace": False, "values": [f"jsn_athletic:{b}" for b in ("mover", "spin_bar", "start_line", "checkpoint", "goal_line")]})
wj(f"{R}/data/minecraft/tags/block/mineable/shovel.json", {"replace": False, "values": ["jsn_athletic:crumble_block"]})
def shaped(name, pat, key, n=1):
    wj(f"{D}/recipe/{name}.json", {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": pat,
        "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in key.items()}, "result": {"id": f"jsn_athletic:{name}", "count": n}})
shaped("mover", ["IGI", "RPR", "III"], {"I": "minecraft:iron_ingot", "G": "minecraft:gold_ingot", "R": "minecraft:redstone", "P": "minecraft:piston"})
shaped("spin_bar", ["BBB", "RPR", "III"], {"I": "minecraft:iron_ingot", "B": "minecraft:blaze_rod", "R": "minecraft:redstone", "P": "minecraft:piston"})
shaped("crumble_block", ["SG", "GS"], {"S": "minecraft:sand", "G": "minecraft:gravel"}, 8)
shaped("start_line", ["LLL", "SSS"], {"L": "minecraft:lime_dye", "S": "minecraft:smooth_stone_slab"}, 3)
shaped("checkpoint", ["LLL", "SSS"], {"L": "minecraft:light_blue_dye", "S": "minecraft:smooth_stone_slab"}, 3)
shaped("goal_line", ["WBW", "SSS"], {"W": "minecraft:white_dye", "B": "minecraft:black_dye", "S": "minecraft:smooth_stone_slab"}, 3)

ja = {"block.jsn_athletic.mover": "仕掛けの台（回る足場・動く床）", "block.jsn_athletic.spin_bar": "回るバー", "block.jsn_athletic.crumble_block": "落ちる床",
      "block.jsn_athletic.start_line": "スタート", "block.jsn_athletic.checkpoint": "チェックポイント", "block.jsn_athletic.goal_line": "ゴール",
      "entity.jsn_athletic.platform": "足場", "key.categories.jsn_athletic": "jsn-Athletic", "key.jsn_athletic.retry": "チェックポイントにもどる（長押しでやめる）"}
en = {"block.jsn_athletic.mover": "Mover Base", "block.jsn_athletic.spin_bar": "Spinning Bar", "block.jsn_athletic.crumble_block": "Crumbling Floor",
      "block.jsn_athletic.start_line": "Start Line", "block.jsn_athletic.checkpoint": "Checkpoint", "block.jsn_athletic.goal_line": "Goal Line",
      "entity.jsn_athletic.platform": "Platform", "key.categories.jsn_athletic": "jsn-Athletic", "key.jsn_athletic.retry": "Back to checkpoint (hold to quit)"}
wj(f"{A}/lang/ja_jp.json", ja); wj(f"{A}/lang/en_us.json", en)
print("ok")

# ================= ジャンプ台・ベルトコンベア・消えたり出たりする床・ふりこ・角材 =================
def jump_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#f08020")); d.rectangle([0, 0, 15, 15], outline=hx("#a04010"))
    d.polygon([(7, 2), (12, 8), (9, 8), (9, 13), (6, 13), (6, 8), (3, 8)], fill=hx("#fff0a0"))
def jump_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#606870"))
    for y in (2, 6, 10): d.line([1, y, 14, y + 2], fill=hx("#c0c8d0"))
tex("jump_top", jump_top); tex("jump_side", jump_side)
wj(f"{A}/models/block/jump_pad.json", {"parent": "minecraft:block/block", "textures": {"top": "jsn_athletic:block/jump_top", "side": "jsn_athletic:block/jump_side",
    "particle": "jsn_athletic:block/jump_top"}, "elements": [{"from": [0, 0, 0], "to": [16, 6, 16], "faces": {
    "up": {"texture": "#top"}, "down": {"texture": "#side"}, **{f: {"texture": "#side", "uv": [0, 10, 16, 16]} for f in ("north", "south", "east", "west")}}}]})
wj(f"{A}/blockstates/jump_pad.json", {"variants": {f"power={p}": {"model": "jsn_athletic:block/jump_pad"} for p in range(1, 6)}})
wj(f"{A}/models/item/jump_pad.json", {"parent": "jsn_athletic:block/jump_pad"})

# コンベア（上の面は矢印が流れる動く絵）
frames = 8
im = Image.new("RGBA", (16, 16 * frames), (0, 0, 0, 0)); d = ImageDraw.Draw(im)
for f in range(frames):
    oy = f * 16
    d.rectangle([0, oy, 15, oy + 15], fill=hx("#303438"))
    d.rectangle([0, oy, 1, oy + 15], fill=hx("#f0c030")); d.rectangle([14, oy, 15, oy + 15], fill=hx("#f0c030"))
    for k in range(-1, 3):
        y = (k * 8 - f * 1) % 16
        pts = [(4, y + 4), (8, y), (12, y + 4), (10, y + 4), (8, y + 2), (6, y + 4)]
        d.polygon([(x, oy + max(0, min(15, yy))) for x, yy in pts], fill=hx("#d0d4d8"))
save(im, f"{A}/textures/block/conveyor_top.png")
with open(f"{A}/textures/block/conveyor_top.png.mcmeta", "w") as fh: json.dump({"animation": {"frametime": 2}}, fh)
tex("conveyor_side", lambda d: (d.rectangle([0, 0, 15, 15], fill=hx("#505860")), d.rectangle([0, 4, 15, 5], fill=hx("#f0c030")),
                                [d.ellipse([x, 7, x + 3, 10], fill=hx("#202428")) for x in (1, 6, 11)]))
wj(f"{A}/models/block/conveyor.json", {"parent": "minecraft:block/block", "textures": {"top": "jsn_athletic:block/conveyor_top", "side": "jsn_athletic:block/conveyor_side",
    "particle": "jsn_athletic:block/conveyor_side"}, "elements": [{"from": [0, 0, 0], "to": [16, 12, 16], "faces": {
    "up": {"texture": "#top"}, "down": {"texture": "#side"}, **{f: {"texture": "#side", "uv": [0, 4, 16, 16]} for f in ("north", "south", "east", "west")}}}]})
wj(f"{A}/blockstates/conveyor.json", {"variants": {f"facing={f},speed={s}": ({"model": "jsn_athletic:block/conveyor"} | ({"y": y} if y else {}))
                                                  for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for s in (1, 2, 3)}})
wj(f"{A}/models/item/conveyor.json", {"parent": "jsn_athletic:block/conveyor"})

# 消えたり出たりする床（4色）
BLINK = ["#e04040", "#f0d030", "#40c060", "#4080f0"]
for g, c in enumerate(BLINK):
    def on(d, c=c):
        d.rectangle([0, 0, 15, 15], fill=hx(c)); d.rectangle([0, 0, 15, 15], outline=(255, 255, 255, 255)); d.rectangle([3, 3, 12, 12], outline=(255, 255, 255, 140))
    def warn(d, c=c):
        d.rectangle([0, 0, 15, 15], fill=hx(c)[:3] + (150,))
        for x in range(-16, 16, 4): d.line([x, 15, x + 15, 0], fill=(255, 255, 255, 220), width=1)
        d.rectangle([0, 0, 15, 15], outline=(255, 255, 255, 255))
    def ghost(d, c=c):
        d.rectangle([0, 0, 15, 15], outline=hx(c)[:3] + (120,)); d.rectangle([1, 1, 14, 14], fill=hx(c)[:3] + (28,))
    tex(f"blink_{g}", on); tex(f"blink_{g}_warn", warn); tex(f"blink_{g}_ghost", ghost)
    wj(f"{A}/models/block/blink_{g}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"jsn_athletic:block/blink_{g}"}})
    wj(f"{A}/models/block/blink_{g}_warn.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": f"jsn_athletic:block/blink_{g}_warn"}})
    wj(f"{A}/models/block/blink_{g}_ghost.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent", "textures": {"all": f"jsn_athletic:block/blink_{g}_ghost"}})
wj(f"{A}/blockstates/blink_block.json", {"variants": {f"group={g},state={s}": {"model": f"jsn_athletic:block/blink_{g}" + ["", "_warn", "_ghost"][s]}
                                                      for g in range(4) for s in range(3)}})
wj(f"{A}/models/item/blink_block.json", {"parent": "jsn_athletic:block/blink_0"})

# ふりこ・角材の台
def pend_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#4a4e56")); d.rectangle([0, 0, 15, 15], outline=hx("#2a2c30"))
    d.line([7, 3, 7, 10], fill=hx("#c0c4c8")); d.ellipse([5, 9, 10, 14], fill=hx("#202224"))
def pend_bottom(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#4a4e56")); d.ellipse([5, 5, 10, 10], fill=hx("#c0c4c8"))
tex("pendulum_side", pend_side); tex("pendulum_bottom", pend_bottom)
cube("pendulum", "pendulum_side", "pendulum_bottom", "pendulum_bottom")
def beam_side(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#8a5a2a")); d.rectangle([2, 6, 13, 9], fill=hx("#f0c030"))
    d.polygon([(9, 4), (13, 7), (9, 11)], fill=hx("#f0c030"))
def beam_top(d):
    d.rectangle([0, 0, 15, 15], fill=hx("#6a4018")); d.ellipse([3, 3, 12, 12], outline=hx("#f0c030")); d.point((7, 7), fill=hx("#f0c030"))
tex("rolling_beam_side", beam_side); tex("rolling_beam_top", beam_top)
cube("rolling_beam", "rolling_beam_side", "rolling_beam_top")
for b in ("pendulum", "rolling_beam"):
    wj(f"{A}/blockstates/{b}.json", {"variants": {"": {"model": f"jsn_athletic:block/{b}"}}})
    wj(f"{A}/models/item/{b}.json", {"parent": f"jsn_athletic:block/{b}"})

NEW = ["jump_pad", "conveyor", "blink_block", "pendulum", "rolling_beam"]
for b in NEW:
    wj(f"{D}/loot_table/blocks/{b}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": f"jsn_athletic:{b}"}],
                                                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
pk = f"{R}/data/minecraft/tags/block/mineable/pickaxe.json"; dd = json.load(open(pk))
for b in ("jump_pad", "conveyor", "blink_block", "pendulum"): dd["values"].append(f"jsn_athletic:{b}")
wj(pk, dd)
wj(f"{R}/data/minecraft/tags/block/mineable/axe.json", {"replace": False, "values": ["jsn_athletic:rolling_beam"]})
shaped("jump_pad", ["SSS", "IPI"], {"S": "minecraft:slime_ball", "I": "minecraft:iron_ingot", "P": "minecraft:piston"}, 2)
shaped("conveyor", ["LLL", "IRI"], {"L": "minecraft:leather", "I": "minecraft:iron_ingot", "R": "minecraft:redstone"}, 4)
shaped("blink_block", ["GAG", "ARA", "GAG"], {"G": "minecraft:glass", "A": "minecraft:amethyst_shard", "R": "minecraft:redstone"}, 4)
shaped("pendulum", ["III", " C ", " B "], {"I": "minecraft:iron_ingot", "C": "minecraft:chain", "B": "minecraft:iron_block"})
shaped("rolling_beam", ["LLL", "RPR", "LLL"], {"L": "#minecraft:logs", "R": "minecraft:redstone", "P": "minecraft:piston"})
ja.update({"block.jsn_athletic.jump_pad": "ジャンプ台", "block.jsn_athletic.conveyor": "ベルトコンベア", "block.jsn_athletic.blink_block": "消えたり出たりする床",
           "block.jsn_athletic.pendulum": "ふりこの鉄球", "block.jsn_athletic.rolling_beam": "回転する角材"})
en.update({"block.jsn_athletic.jump_pad": "Jump Pad", "block.jsn_athletic.conveyor": "Conveyor Belt", "block.jsn_athletic.blink_block": "Blinking Floor",
           "block.jsn_athletic.pendulum": "Pendulum Ball", "block.jsn_athletic.rolling_beam": "Rolling Beam"})
wj(f"{A}/lang/ja_jp.json", ja); wj(f"{A}/lang/en_us.json", en)
print("ok2")

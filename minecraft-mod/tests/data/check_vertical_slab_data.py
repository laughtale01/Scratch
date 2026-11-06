#!/usr/bin/env python3
"""縦スラブの名前が、ModBlocks・ModItems・loot table・タグでそろっているかを確かめる

タグは "required": false で書いているので、名前を打ち間違えても Minecraft はエラーを出さずに飛ばす。
（ツルハシのタグで飛ばされると、その縦スラブは壊しても何も落ちなくなる）
そのため、ここで名前を突き合わせて、打ち間違いを見つける。

使い方（minecraft-mod で）: python3 -I tests/data/check_vertical_slab_data.py
そろっていれば「そろっています」と出して 0 で終わる。食い違いがあれば、その中身を出して 1 で終わる。
"""
import json
import os
import re
import sys

MOD = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
JAVA = os.path.join(MOD, 'src', 'main', 'java', 'com', 'github', 'minecraftedu', 'init')
DATA = os.path.join(MOD, 'src', 'main', 'resources', 'data')
LOOT = os.path.join(DATA, 'minecraftedu', 'loot_tables', 'blocks')
TAGS = os.path.join(DATA, 'minecraft', 'tags', 'blocks')
NS = 'minecraftedu:'

# バニラ 1.20.1 の needs_*_tool に入っている元のブロック（Blocks の名前）。ここに無いものは木のツルハシで落ちる
VANILLA_NEEDS = {
    'needs_stone_tool': {
        'IRON_BLOCK', 'COPPER_BLOCK', 'WAXED_COPPER_BLOCK', 'LAPIS_BLOCK',
        'CUT_COPPER', 'EXPOSED_CUT_COPPER', 'WEATHERED_CUT_COPPER', 'OXIDIZED_CUT_COPPER',
        'WAXED_CUT_COPPER', 'WAXED_EXPOSED_CUT_COPPER', 'WAXED_WEATHERED_CUT_COPPER', 'WAXED_OXIDIZED_CUT_COPPER',
    },
    'needs_iron_tool': {'GOLD_BLOCK', 'DIAMOND_BLOCK', 'EMERALD_BLOCK'},
    'needs_diamond_tool': {'NETHERITE_BLOCK'},
}

errors = []


def fail(msg):
    errors.append(msg)


def read(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


def registrations(text, register):
    """register("名前", ...) を 1 つずつ取り出す。返すのは (定数の名前, 登録の名前, 中身) の並び"""
    pattern = (r'public static final RegistryObject<\w+> (\w+) = ' + re.escape(register)
               + r'\.register\(\s*"([^"]+)"(.*?)\n    \);')
    return re.findall(pattern, text, re.S)


# ---- ModBlocks: 名前・元のブロック・道具が要るか ----
blocks = {}       # 登録の名前 → (元のブロック, 道具が要るか)
block_const = {}  # 定数の名前 → 登録の名前
for const, name, body in registrations(read(os.path.join(JAVA, 'ModBlocks.java')), 'BLOCKS'):
    src = re.search(r'Properties\.copy\(Blocks\.(\w+)\)', body)
    if name in blocks:
        fail('ModBlocks: %s を 2 回登録している' % name)
    blocks[name] = (src.group(1) if src else None, 'requiresCorrectToolForDrops' in body)
    block_const[const] = name
if not blocks:
    fail('ModBlocks: 登録が 1 つも見つからない（読み取りの決まりがずれた？）')
for name, (src, _) in blocks.items():
    if src is None:
        fail('ModBlocks: %s の元のブロック（Properties.copy）が見つからない' % name)
wood = {n for n, (src, _) in blocks.items() if src and src.endswith('_PLANKS')}
stone = set(blocks) - wood
for n in sorted(wood):
    if blocks[n][1]:
        fail('ModBlocks: 木の %s に requiresCorrectToolForDrops がある（手で壊すと落ちなくなる）' % n)
for n in sorted(stone):
    if not blocks[n][1]:
        fail('ModBlocks: %s に requiresCorrectToolForDrops が無い（元のブロックと違って手でも落ちる）' % n)

# ---- ModItems: 同じ名前で、同じ名前のブロックを指しているか ----
items = set()
for const, name, body in registrations(read(os.path.join(JAVA, 'ModItems.java')), 'ITEMS'):
    items.add(name)
    ref = re.search(r'ModBlocks\.(\w+)\.get\(\)', body)
    if not ref:
        fail('ModItems: %s が指すブロックが見つからない' % name)
    elif block_const.get(ref.group(1)) != name:
        fail('ModItems: %s が別の名前のブロック（ModBlocks.%s）を指している' % (name, ref.group(1)))
for n in sorted(set(blocks) - items):
    fail('ModItems: %s のアイテムが無い' % n)
for n in sorted(items - set(blocks)):
    fail('ModItems: %s は ModBlocks に無い' % n)

# ---- loot table: ブロックごとに 1 つ、自分のアイテムを落とすか ----
loot = {f[:-len('.json')] for f in os.listdir(LOOT) if f.endswith('.json')}
for n in sorted(set(blocks) - loot):
    fail('loot table: %s.json が無い（壊しても何も落ちない）' % n)
for n in sorted(loot - set(blocks)):
    fail('loot table: %s.json のブロックが ModBlocks に無い' % n)
for n in sorted(loot & set(blocks)):
    table = json.loads(read(os.path.join(LOOT, n + '.json')))
    names = [e.get('name') for p in table.get('pools', []) for e in p.get('entries', [])]
    if table.get('type') != 'minecraft:block':
        fail('loot table: %s.json の type が minecraft:block でない' % n)
    if names != [NS + n]:
        fail('loot table: %s.json が落とすものが %s（%s%s のはず）' % (n, names, NS, n))
    if table.get('random_sequence') != NS + 'blocks/' + n:
        fail('loot table: %s.json の random_sequence が %s' % (n, table.get('random_sequence')))


# ---- タグ: どのブロックも 1 つの道具に入り、要る道具の強さが元のブロックと同じか ----
def tag(path):
    t = json.loads(read(os.path.join(TAGS, path + '.json')))
    if t.get('replace') is not False:
        fail('タグ %s: replace が false でない（バニラのタグを消してしまう）' % path)
    ids = []
    for v in t.get('values', []):
        i = v['id'] if isinstance(v, dict) else v
        if not (isinstance(v, dict) and v.get('required') is False):
            fail('タグ %s: %s が "required": false の形でない' % (path, i))
        if not i.startswith(NS) or i[len(NS):] not in blocks:
            fail('タグ %s: %s は ModBlocks に無い（打ち間違い？）' % (path, i))
        ids.append(i[len(NS):])
    if len(ids) != len(set(ids)):
        fail('タグ %s: 同じ名前が 2 回ある' % path)
    return set(ids)


axe, pickaxe = tag('mineable/axe'), tag('mineable/pickaxe')
if axe != wood:
    fail('タグ mineable/axe: 足りない %s・余分 %s' % (sorted(wood - axe), sorted(axe - wood)))
if pickaxe != stone:
    fail('タグ mineable/pickaxe: 足りない %s・余分 %s' % (sorted(stone - pickaxe), sorted(pickaxe - stone)))
for t, vanilla in VANILLA_NEEDS.items():
    want = {n for n in blocks if blocks[n][0] in vanilla}
    got = tag(t)
    if got != want:
        fail('タグ %s: 足りない %s・余分 %s' % (t, sorted(want - got), sorted(got - want)))

if errors:
    print('縦スラブの名前に食い違いがあります（%d 件）' % len(errors))
    for e in errors:
        print('  - ' + e)
    sys.exit(1)
print('縦スラブ %d 種の名前が、ModBlocks・ModItems・loot table・タグでそろっています' % len(blocks))

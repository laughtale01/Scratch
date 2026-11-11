#!/usr/bin/env python3
"""MOD のブロック（縦スラブと、金などの階段・ハーフブロック）の名前が、
ModBlocks・ModItems・loot table・タグ・名前（lang）・見た目のファイルでそろっているかを確かめる

タグは "required": false で書いているので、名前を打ち間違えても Minecraft はエラーを出さずに飛ばす。
（ツルハシのタグで飛ばされると、そのブロックは壊しても何も落ちなくなる）
そのため、ここで名前を突き合わせて、打ち間違いを見つける。
階段・ハーフブロックの見た目とデータのファイルは tools/make_stairs_slab_data.py で作るので、
その台本が作る中身とファイルが同じか（台本を流し忘れていないか・手で書きかえていないか）も確かめる。
さらに、台本に石英の階段・ハーフを作らせて、Minecraft 1.20.1 の元のデータ（vanilla_quartz_stairs_slab_1_20_1.json）と
比べ、台本の決まりそのもの（作るファイルの組・向き・回し方・2 まい重ね・落ちるもの・形（親・絵の名前）・名前の頭）が
Minecraft と同じかも確かめる。絵は、台本がどの面にも元のブロックの絵 1 まいを使う決まりなので、丸ごとは比べず、
フォルダと名前の頭（minecraft:block/quartz_block）が Minecraft の絵の名前と合うかを見る
（台本と出力を一緒にまちがえると、上の比べでは見つからないため）。
（ファイル名は、縦スラブだけだったころのまま）

使い方（minecraft-mod で）: python3 -I tests/data/check_vertical_slab_data.py
そろっていれば「そろっています」と出して 0 で終わる。食い違いがあれば、その中身を出して 1 で終わる。
"""
import importlib.util
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
kinds = {}        # 登録の名前 → 作りの種類（VerticalSlabBlock・StairBlock・SlabBlock）
for const, name, body in registrations(read(os.path.join(JAVA, 'ModBlocks.java')), 'BLOCKS'):
    src = re.search(r'Properties\.copy\(Blocks\.(\w+)\)', body)
    if name in blocks:
        fail('ModBlocks: %s を 2 回登録している' % name)
    blocks[name] = (src.group(1) if src else None, 'requiresCorrectToolForDrops' in body)
    block_const[const] = name
    kind = re.search(r'new (\w+)\(', body)
    kinds[name] = kind.group(1) if kind else None
    if kinds[name] not in ('VerticalSlabBlock', 'StairBlock', 'SlabBlock'):
        fail('ModBlocks: %s の作り（new …）が %s（この確かめが知らない種類）' % (name, kinds[name]))
    # 階段は「元のブロックの状態」も、Properties.copy と同じブロックを指す（違うと見た目と性質がずれる）
    if kinds[name] == 'StairBlock':
        base = re.search(r'Blocks\.(\w+)\.defaultBlockState\(\)', body)
        if not base or not src or base.group(1) != src.group(1):
            fail('ModBlocks: 階段 %s の元のブロック（defaultBlockState）と Properties.copy がそろっていない' % name)
    want_suffix = {'VerticalSlabBlock': '_slab', 'StairBlock': '_stairs', 'SlabBlock': '_slab'}.get(kinds[name])
    if want_suffix and not name.endswith(want_suffix):
        fail('ModBlocks: %s は %s なのに名前が %s で終わらない（gui.js の置き方の見分けがずれる）' % (name, kinds[name], want_suffix))
    if kinds[name] == 'VerticalSlabBlock' and 'vertical_' not in name:
        fail('ModBlocks: 縦スラブ %s の名前に vertical_ が無い（gui.js が MOD のブロックと見分けられない）' % name)
    if kinds[name] in ('StairBlock', 'SlabBlock') and 'vertical_' in name:
        fail('ModBlocks: %s は縦スラブではないのに名前に vertical_ がある（gui.js が縦スラブとして送る）' % name)
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

# 階段・ハーフのタグ（ブロックとアイテム）: 階段とハーフブロック（横向き）がちょうど入っているか
# （縦スラブは Minecraft のハーフブロックとは置き方がちがうので入れない）
ITEM_TAGS = os.path.join(DATA, 'minecraft', 'tags', 'items')
for tag_name, kind in (('stairs', 'StairBlock'), ('slabs', 'SlabBlock')):
    want = {n for n, k in kinds.items() if k == kind}
    for folder, label in ((TAGS, 'blocks'), (ITEM_TAGS, 'items')):
        path = os.path.join(folder, tag_name + '.json')
        if not os.path.exists(path):
            if want:
                fail('タグ %s/%s.json が無い（%s を入れる）' % (label, tag_name, sorted(want)))
            continue
        got = tag(os.path.relpath(path, TAGS)[:-len('.json')])
        if got != want:
            fail('タグ %s/%s: 足りない %s・余分 %s' % (label, tag_name, sorted(want - got), sorted(got - want)))

# ---- 名前（lang）: どのブロックにも日本語と英語の名前があり、余分な名前が無いか ----
LANG = os.path.join(MOD, 'src', 'main', 'resources', 'assets', 'minecraftedu', 'lang')
for f in ('ja_jp.json', 'en_us.json'):
    names = json.loads(read(os.path.join(LANG, f)))
    keys = {k[len('block.minecraftedu.'):] for k in names if k.startswith('block.minecraftedu.')}
    for n in sorted(set(blocks) - keys):
        fail('名前 %s: %s の名前が無い（ゲームの中で block.minecraftedu.… と出る）' % (f, n))
    for n in sorted(keys - set(blocks)):
        fail('名前 %s: %s は ModBlocks に無い' % (f, n))
    for k, v in names.items():
        if not isinstance(v, str) or not v.strip():
            fail('名前 %s: %s が空' % (f, k))

# ---- 見た目: どのブロックにもブロックの状態の表と持ち物の形があるか ----
ASSETS = os.path.join(MOD, 'src', 'main', 'resources', 'assets', 'minecraftedu')
for n in sorted(blocks):
    for sub in ('blockstates', os.path.join('models', 'item')):
        if not os.path.exists(os.path.join(ASSETS, sub, n + '.json')):
            fail('見た目: %s/%s.json が無い（ゲームの中で紫と黒のブロックになる）' % (sub, n))

# ---- 階段・ハーフブロックのファイル: tools/make_stairs_slab_data.py が作る中身と同じか ----
GEN = os.path.join(MOD, 'tools', 'make_stairs_slab_data.py')
# 読み込んでも tools/__pycache__ に .pyc を書かない（-I では止まらない。.pyc にはこの PC のフォルダの場所が入る）
sys.dont_write_bytecode = True
spec = importlib.util.spec_from_file_location('make_stairs_slab_data', GEN)
gen = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gen)
gen_files = gen.all_files()
for path, content in sorted(gen_files.items()):
    rel = os.path.relpath(path, MOD)
    if not os.path.exists(path):
        fail('台本のファイル: %s が無い（python3 -I tools/make_stairs_slab_data.py を流す）' % rel)
    elif read(path) != gen.file_text(content):
        fail('台本のファイル: %s が台本の中身とちがう（手で書きかえた？ 台本を直して流し直す）' % rel)
# 台本の決まりそのものが Minecraft と同じか: 台本に石英の階段・ハーフを作らせ、
# Minecraft 1.20.1 から写した元のデータ（vanilla_quartz_stairs_slab_1_20_1.json）と比べる
# （上の比べは「台本の出力」と「ファイル」なので、台本と出力を一緒にまちがえると見つからないため）
VANILLA = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'vanilla_quartz_stairs_slab_1_20_1.json')
# 元のデータに入っているはずの名前（階段・ハーフブロックと、その形 5 つ）
VANILLA_SHAPES = {'quartz_stairs', 'quartz_slab'}
VANILLA_MODELS = {'quartz_stairs', 'quartz_stairs_inner', 'quartz_stairs_outer', 'quartz_slab', 'quartz_slab_top'}
# 台本に石英の階段・ハーフを作らせる（台本の中のまちがいは、元のデータのせいにしないよう、元のデータを読む前に作る）
made = {}
made.update(gen.stairs_files('quartz_stairs', 'quartz_block'))
made.update(gen.slab_files('quartz_slab', 'quartz_block'))
made = {os.path.relpath(p, MOD).replace(os.sep, '/'): c for p, c in made.items()}
# 元のデータを読み、形を確かめる（読めない・形がくずれているときだけ「比べられない」と出す）
want = None
try:
    vanilla = json.loads(read(VANILLA))
    # 元のデータの「石英の階段・ハーフ自身を指す名前」だけを MOD の名前（minecraftedu:）に読みかえる。
    # 2 まい重ねの形（minecraft:block/quartz_block）や親の形（minecraft:block/stairs）・絵は minecraft: のまま比べる
    # （台本の出力のほうを全部 minecraft: に読みかえると、minecraftedu: と minecraft: の取りちがえが見つからないため）
    want = json.loads(re.sub(r'"minecraft:((?:block/|blocks/)?quartz_(?:stairs|slab)\w*)"', r'"minecraftedu:\1"',
                             json.dumps(vanilla, ensure_ascii=False)))
    # 元のデータがそろっているか（空や 1 つ欠けたままだと、比べる物が減って黙って通ってしまうため）
    for key, names in (('blockstates', VANILLA_SHAPES), ('loot_tables', VANILLA_SHAPES),
                       ('item_models', VANILLA_SHAPES), ('models', VANILLA_MODELS)):
        if not isinstance(want.get(key), dict):
            raise TypeError('%s が無いか、形がちがう' % key)
        if set(want[key]) != names:
            raise ValueError('%s の名前が %s（%s のはず）' % (key, sorted(want[key]), sorted(names)))
    for n, model in want['models'].items():
        if not isinstance(model, dict) or not isinstance(model.get('textures'), dict):
            raise TypeError('形 %s が無いか、形がちがう' % n)
        if not model['textures'] or not all(isinstance(v, str) for v in model['textures'].values()):
            raise TypeError('形 %s の絵が空か、文字でない' % n)
except (OSError, ValueError, KeyError, TypeError, AttributeError) as e:
    fail('台本の決まり: Minecraft 1.20.1 の元のデータ（%s）と比べられない: %s: %s'
         % (os.path.basename(VANILLA), type(e).__name__, e))
    want = None


def same(a, b):
    """JSON として同じか（Python の == は True と 1・2.0 と 2 を同じと見るので、JSON の文字にして比べる）"""
    return json.dumps(a, sort_keys=True, ensure_ascii=False) == json.dumps(b, sort_keys=True, ensure_ascii=False)


if want is not None:
    RES_REL = 'src/main/resources/'
    # 台本が作るファイルが、Minecraft と同じ組か（足りない・余分な形やファイルを作っていないか）
    want_paths = ({RES_REL + 'assets/minecraftedu/blockstates/%s.json' % n for n in want['blockstates']}
                  | {RES_REL + 'data/minecraftedu/loot_tables/blocks/%s.json' % n for n in want['loot_tables']}
                  | {RES_REL + 'assets/minecraftedu/models/item/%s.json' % n for n in want['item_models']}
                  | {RES_REL + 'assets/minecraftedu/models/block/%s.json' % n for n in want['models']})
    if set(made) != want_paths:
        fail('台本の決まり: 台本が作るファイルが Minecraft 1.20.1 の組とちがう: 足りない %s・余分 %s'
             % (sorted(want_paths - set(made)), sorted(set(made) - want_paths)))
    for n in sorted(VANILLA_SHAPES):
        if not same(made.get(RES_REL + 'assets/minecraftedu/blockstates/%s.json' % n), want['blockstates'][n]):
            fail('台本の決まり: %s のブロックの状態の表が Minecraft 1.20.1 とちがう（向き・回し方・2 まい重ね・名前の頭）' % n)
        if not same(made.get(RES_REL + 'data/minecraftedu/loot_tables/blocks/%s.json' % n), want['loot_tables'][n]):
            fail('台本の決まり: %s のこわしたとき落ちるものが Minecraft 1.20.1 とちがう' % n)
        if not same(made.get(RES_REL + 'assets/minecraftedu/models/item/%s.json' % n), want['item_models'][n]):
            fail('台本の決まり: %s の持ち物の形が Minecraft 1.20.1 とちがう' % n)
    for n, vmodel in want['models'].items():
        model = made.get(RES_REL + 'assets/minecraftedu/models/block/%s.json' % n)
        if not isinstance(model, dict) or not isinstance(model.get('textures'), dict):
            fail('台本の決まり: 台本の出力の形 %s がくずれている（%r）' % (n, model))
            continue
        # 絵のほかは、Minecraft と丸ごと同じ（親・余分なキーが無いこと）
        if not same({k: v for k, v in model.items() if k != 'textures'}, {k: v for k, v in vmodel.items() if k != 'textures'}):
            fail('台本の決まり: 形 %s が Minecraft 1.20.1 とちがう（親など）' % n)
        # 絵の名前（bottom・side・top）。ちがうと、親の形が使う面の絵が無く、紫と黒になる
        if sorted(model['textures']) != sorted(vmodel['textures']):
            fail('台本の決まり: 形 %s の絵の名前が %s（Minecraft 1.20.1 は %s）' % (n, sorted(model['textures']), sorted(vmodel['textures'])))
        # 絵そのもの: 台本はどの面にも元のブロックの絵 1 まい（ここでは minecraft:block/quartz_block）を使う決まり。
        # 石英は面ごとに絵がちがう（quartz_block_top・quartz_block_side）ので、絵の名前を丸ごとは比べられない。
        # そこで「台本の絵が minecraft:block/quartz_block そのもの」で「Minecraft の絵の名前がそれで始まる」かを見る
        # （名前の頭・フォルダ（minecraft:block/）の取りちがえで、紫と黒になるのを見つける）
        for k, v in model['textures'].items():
            vv = vmodel['textures'].get(k)
            if v != 'minecraft:block/quartz_block' or not (isinstance(vv, str) and vv.startswith(v)):
                fail('台本の決まり: 形 %s の絵 %s が %r（台本の決まりでは minecraft:block/quartz_block。'
                     'Minecraft 1.20.1 の絵 %r は、その名前で始まるはず）' % (n, k, v, vv))

gen_names = {p + s for p, _ in gen.MATERIALS for s in ('_stairs', '_slab')}
shaped = {n for n, k in kinds.items() if k in ('StairBlock', 'SlabBlock')}
if gen_names != shaped:
    fail('台本の MATERIALS と ModBlocks の階段・ハーフブロック: 台本だけ %s・ModBlocks だけ %s'
         % (sorted(gen_names - shaped), sorted(shaped - gen_names)))
for prefix, full in gen.MATERIALS:
    for suffix in ('_stairs', '_slab'):
        n = prefix + suffix
        if n in blocks and blocks[n][0] != full.upper():
            fail('台本の MATERIALS: %s の元のブロック %s が ModBlocks の Properties.copy（%s）とちがう'
                 % (n, full, blocks[n][0]))

if errors:
    print('MOD のブロックの名前に食い違いがあります（%d 件）' % len(errors))
    for e in errors:
        print('  - ' + e)
    sys.exit(1)
counts = {k: sum(1 for v in kinds.values() if v == k) for k in ('VerticalSlabBlock', 'StairBlock', 'SlabBlock')}
print('MOD のブロック %d 種（縦スラブ %d・階段 %d・ハーフブロック %d）の名前が、'
      'ModBlocks・ModItems・loot table・タグ・名前・見た目でそろっています（階段・ハーフの台本の決まりも Minecraft 1.20.1 と同じ）'
      % (len(blocks), counts['VerticalSlabBlock'], counts['StairBlock'], counts['SlabBlock']))

#!/usr/bin/env python3
"""MOD の階段とハーフブロック（横向き）の見た目とデータのファイルを作る

Minecraft には金ブロックなどの階段・ハーフブロックが無いので、MOD で足している（2026-11-10、まず金だけ）。
1 種類ごとに、ブロックの状態の表・形・持ち物の形・こわしたとき落ちるもの を作る。
階段のブロックの状態の表は向き 4 × 上下 2 × 曲がり方 5 の 40 通りあり、手で書くとまちがえやすいので、
Minecraft 1.20.1 の階段（石英の階段など）と同じ決まりで作る。

作らないもの（手で書く。tests/data/check_vertical_slab_data.py が名前のそろいを確かめる）:
  ModBlocks・ModItems への登録、道具のタグ（mineable/pickaxe・needs_*_tool）、
  階段・ハーフのタグ（stairs・slabs）、名前（lang の ja_jp・en_us）

種類を足すときは、下の MATERIALS に 1 行足して流す（minecraft-mod で）:
  python3 -I tools/make_stairs_slab_data.py
すでにあるファイルは、中身が同じなら何もしない。違えば書き直して、その名前を出す。
tests/data/check_vertical_slab_data.py は、この台本の all_files() とファイルの中身が同じかと、
この台本の決まりが Minecraft 1.20.1 の石英の階段・ハーフと同じか（tests/data/vanilla_quartz_stairs_slab_1_20_1.json）を確かめる。

1 行足すだけで作れるものの限り（ほかの種類を足す前に読む）:
  - 元のブロックが、上・横・下とも同じ 1 枚の絵で、絵の名前がブロックの名前と同じもの
    （minecraft:block/<名前> の絵と形があるもの。金・鉄・ダイヤ・エメラルドのブロックなど）。
    石英のブロック（上と横の絵がちがう）や、ろう引きの銅（絵の名前が copper_block）は、このままでは
    紫と黒のブロックになる。そういうものを足すときは、絵と 2 まい重ねの形を別に書けるように台本を広げる
  - こわすのに要る道具の強さは、確かめ（check_vertical_slab_data.py の VANILLA_NEEDS）が知っている元のブロックだけ
    正しく確かめられる。知らないもの（黒曜石など）を足すときは、VANILLA_NEEDS にも足す
  - gui.js の MOD_STAIRS_SLAB_BLOCKS・ブロックの一覧・訳の表にも、同じ名前を足す（足さないと Scratch から置けない）
"""
import json
import os

MOD = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
RES = os.path.join(MOD, 'src', 'main', 'resources')
ASSETS = os.path.join(RES, 'assets', 'minecraftedu')
LOOT = os.path.join(RES, 'data', 'minecraftedu', 'loot_tables', 'blocks')
NS = 'minecraftedu'

# 種類: (名前の頭, 元のブロックの名前〔丸ごとのブロックの形・絵に使う〕)
#   名前の頭が gold_block なら、gold_block_stairs と gold_block_slab を作る
MATERIALS = [
    ('gold_block', 'gold_block'),
]

# 階段の向きごとの、まっすぐのときの回し方（Y の角度）
STAIRS_BASE_Y = {'east': 0, 'south': 90, 'west': 180, 'north': 270}


def stairs_variant(facing, half, shape, name):
    """階段の 1 つの状態の見た目（Minecraft 1.20.1 の階段と同じ回し方）"""
    base = STAIRS_BASE_Y[facing]
    if shape == 'straight':
        model, turn = name, 0
    else:
        model = name + ('_inner' if shape.startswith('inner') else '_outer')
        if half == 'bottom':
            # 下向きの階段: 左に曲がる形は 90 度もどす
            turn = -90 if shape.endswith('left') else 0
        else:
            # さかさまの階段: 右に曲がる形を 90 度すすめる
            turn = 90 if shape.endswith('right') else 0
    x = 180 if half == 'top' else 0
    y = (base + turn) % 360
    v = {'model': '%s:block/%s' % (NS, model)}
    # 回さないときは、絵をブロックに合わせて回す必要がない（Minecraft も書かない）
    if x or y:
        v['uvlock'] = True
    if x:
        v['x'] = x
    if y:
        v['y'] = y
    return v


def stairs_files(name, full):
    """階段のファイル（パス → 中身）"""
    tex = 'minecraft:block/' + full
    textures = {'bottom': tex, 'side': tex, 'top': tex}
    variants = {}
    for facing in sorted(STAIRS_BASE_Y):
        for half in ('bottom', 'top'):
            for shape in ('inner_left', 'inner_right', 'outer_left', 'outer_right', 'straight'):
                variants['facing=%s,half=%s,shape=%s' % (facing, half, shape)] = stairs_variant(facing, half, shape, name)
    return {
        os.path.join(ASSETS, 'blockstates', name + '.json'): {'variants': variants},
        os.path.join(ASSETS, 'models', 'block', name + '.json'):
            {'parent': 'minecraft:block/stairs', 'textures': textures},
        os.path.join(ASSETS, 'models', 'block', name + '_inner.json'):
            {'parent': 'minecraft:block/inner_stairs', 'textures': textures},
        os.path.join(ASSETS, 'models', 'block', name + '_outer.json'):
            {'parent': 'minecraft:block/outer_stairs', 'textures': textures},
        os.path.join(ASSETS, 'models', 'item', name + '.json'):
            {'parent': '%s:block/%s' % (NS, name)},
        # こわすと階段が 1 つ落ちる（爆発で消えることがある。Minecraft の階段と同じ）
        os.path.join(LOOT, name + '.json'): {
            'type': 'minecraft:block',
            'pools': [{
                'bonus_rolls': 0.0,
                'conditions': [{'condition': 'minecraft:survives_explosion'}],
                'entries': [{'type': 'minecraft:item', 'name': '%s:%s' % (NS, name)}],
                'rolls': 1.0,
            }],
            'random_sequence': '%s:blocks/%s' % (NS, name),
        },
    }


def slab_files(name, full):
    """ハーフブロックのファイル（パス → 中身）"""
    tex = 'minecraft:block/' + full
    textures = {'bottom': tex, 'side': tex, 'top': tex}
    return {
        # 2 まい重ね（double）は、元の丸ごとのブロックと同じ見た目
        os.path.join(ASSETS, 'blockstates', name + '.json'): {'variants': {
            'type=bottom': {'model': '%s:block/%s' % (NS, name)},
            'type=double': {'model': 'minecraft:block/' + full},
            'type=top': {'model': '%s:block/%s_top' % (NS, name)},
        }},
        os.path.join(ASSETS, 'models', 'block', name + '.json'):
            {'parent': 'minecraft:block/slab', 'textures': textures},
        os.path.join(ASSETS, 'models', 'block', name + '_top.json'):
            {'parent': 'minecraft:block/slab_top', 'textures': textures},
        os.path.join(ASSETS, 'models', 'item', name + '.json'):
            {'parent': '%s:block/%s' % (NS, name)},
        # こわすと 1 つ、2 まい重ねなら 2 つ落ちる（Minecraft のハーフブロックと同じ）
        os.path.join(LOOT, name + '.json'): {
            'type': 'minecraft:block',
            'pools': [{
                'bonus_rolls': 0.0,
                'entries': [{
                    'type': 'minecraft:item',
                    'functions': [
                        {
                            'add': False,
                            'conditions': [{
                                'block': '%s:%s' % (NS, name),
                                'condition': 'minecraft:block_state_property',
                                'properties': {'type': 'double'},
                            }],
                            'count': 2.0,
                            'function': 'minecraft:set_count',
                        },
                        {'function': 'minecraft:explosion_decay'},
                    ],
                    'name': '%s:%s' % (NS, name),
                }],
                'rolls': 1.0,
            }],
            'random_sequence': '%s:blocks/%s' % (NS, name),
        },
    }


def all_files():
    """作るファイルをすべて（パス → 中身）"""
    files = {}
    for prefix, full in MATERIALS:
        files.update(stairs_files(prefix + '_stairs', full))
        files.update(slab_files(prefix + '_slab', full))
    return files


def file_text(content):
    """ファイルに書く文字（2 つの空白で字下げ・最後に改行）"""
    return json.dumps(content, indent=2, ensure_ascii=False) + '\n'


def main():
    changed = 0
    for path, content in sorted(all_files().items()):
        text = file_text(content)
        old = None
        if os.path.exists(path):
            with open(path, encoding='utf-8') as f:
                old = f.read()
        if old == text:
            continue
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(text)
        changed += 1
        print(('作った  ' if old is None else '書き直した ') + os.path.relpath(path, MOD))
    print('階段とハーフブロック %d 種（ファイル %d 個のうち %d 個を書いた）' % (len(MATERIALS), len(all_files()), changed))


if __name__ == '__main__':
    main()

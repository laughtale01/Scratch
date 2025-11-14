#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Scratchのgh-pagesブランチのJavaScriptファイルを更新して、
clearAreaとclearAllEntitiesブロックに座標パラメータを追加します。
"""

import re

# 修正するファイルリスト
files = [
    'gui.js',
    'blocksonly.js',
    'compatibilitytesting.js',
    'player.js'
]

# 1. clearAreaブロック定義の置換
# 元のパターン
old_cleararea_pattern = r'''(\{[\s\S]*?opcode:\s*['"]clearArea['"][\s\S]*?blockType:\s*['"]command['"][\s\S]*?text:\s*['"]周囲クリア['"][\s\S]*?\})'''

# 新しい定義
new_cleararea = '''{
        opcode: 'clearArea',
        blockType: 'command',
        text: 'X:[X] Z:[Z] を中心に周囲クリア',
        arguments: {
          X: {
            type: 'number',
            defaultValue: 0
          },
          Z: {
            type: 'number',
            defaultValue: 0
          }
        }
      }'''

# 2. clearAllEntitiesブロック定義の置換
old_clearallentities_pattern = r'''(\{[\s\S]*?opcode:\s*['"]clearAllEntities['"][\s\S]*?blockType:\s*['"]command['"][\s\S]*?text:\s*['"]全エンティティをクリア['"][\s\S]*?\})'''

new_clearallentities = '''{
        opcode: 'clearAllEntities',
        blockType: 'command',
        text: 'X:[X] Z:[Z] を中心に全エンティティをクリア',
        arguments: {
          X: {
            type: 'number',
            defaultValue: 0
          },
          Z: {
            type: 'number',
            defaultValue: 0
          }
        }
      }'''

# 3. clearAreaメソッド実装の置換
old_cleararea_method = r'''clearArea\s*\(\s*\)\s*\{[\s\S]*?console\.log\(['"]周囲クリア開始[\s\S]*?return\s+this\.sendCommand\(['"]clearArea['"],\s*\{\}\);[\s\S]*?\}'''

new_cleararea_method = '''clearArea(args) {
        const centerX = Number(args.X) || 0;
        const centerZ = Number(args.Z) || 0;
        console.log(`周囲クリア開始: 中心(${centerX}, ${centerZ}) から±50ブロック範囲`);
        return this.sendCommand('clearArea', {
          centerX: centerX,
          centerZ: centerZ
        });
      }'''

# 4. clearAllEntitiesメソッド実装の置換
old_clearallentities_method = r'''clearAllEntities\s*\(\s*\)\s*\{[\s\S]*?console\.log\(['"]全エンティティクリア開始['"]?\);[\s\S]*?return\s+this\.sendCommand\(['"]clearAllEntities['"],\s*\{\}\);[\s\S]*?\}'''

new_clearallentities_method = '''clearAllEntities(args) {
        const centerX = Number(args.X) || 0;
        const centerZ = Number(args.Z) || 0;
        console.log(`全エンティティクリア開始: 中心(${centerX}, ${centerZ}) から±50ブロック範囲`);
        return this.sendCommand('clearAllEntities', {
          centerX: centerX,
          centerZ: centerZ
        });
      }'''

for filename in files:
    print(f"Processing {filename}...")

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    # ブロック定義を置換（より簡単なパターンマッチング）
    # clearAreaブロック
    content = re.sub(
        r"opcode:\s*'clearArea',\s*blockType:\s*'command',\s*text:\s*'周囲クリア'",
        "opcode: 'clearArea', blockType: 'command', text: 'X:[X] Z:[Z] を中心に周囲クリア', arguments: { X: { type: 'number', defaultValue: 0 }, Z: { type: 'number', defaultValue: 0 } }",
        content
    )

    # clearAllEntitiesブロック
    content = re.sub(
        r"opcode:\s*'clearAllEntities',\s*blockType:\s*'command',\s*text:\s*'全エンティティをクリア'",
        "opcode: 'clearAllEntities', blockType: 'command', text: 'X:[X] Z:[Z] を中心に全エンティティをクリア', arguments: { X: { type: 'number', defaultValue: 0 }, Z: { type: 'number', defaultValue: 0 } }",
        content
    )

    # メソッド実装を置換
    content = re.sub(old_cleararea_method, new_cleararea_method, content, flags=re.MULTILINE)
    content = re.sub(old_clearallentities_method, new_clearallentities_method, content, flags=re.MULTILINE)

    with open(filename, 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"OK: {filename} updated")

print("All files updated successfully!")

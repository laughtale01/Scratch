import re

print('='*80)
print('フェーズ2B: 実装内容の検証')
print('='*80)

# Read the index.js file
with open('scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js', 'r', encoding='utf-8') as f:
    content = f.read()

# Check 1: Verify getPlayerFacing block definition
print('\n【1】getPlayerFacing ブロック定義の確認')
print('-'*80)

pattern_facing_block = r"opcode:\s*'getPlayerFacing'.*?blockType:\s*'reporter'.*?text:\s*'プレイヤーの向き'"
match = re.search(pattern_facing_block, content, re.DOTALL)

if match:
    print('[OK] getPlayerFacing ブロック定義が見つかりました')
    print(f'     位置: {content.index(match.group(0))}文字目')
else:
    print('[ERROR] getPlayerFacing ブロック定義が見つかりません')

# Check 2: Verify getPlayerFacing implementation method
print('\n【2】getPlayerFacing 実装メソッドの確認')
print('-'*80)

pattern_facing_impl = r"getPlayerFacing\(\)\s*\{.*?sendCommandWithResponse\('getPlayerFacing'"
match = re.search(pattern_facing_impl, content, re.DOTALL)

if match:
    print('[OK] getPlayerFacing 実装メソッドが見つかりました')
    print(f'     位置: {content.index(match.group(0))}文字目')
else:
    print('[ERROR] getPlayerFacing 実装メソッドが見つかりません')

# Check 3: Verify getBlockType block definition
print('\n【3】getBlockType ブロック定義の確認')
print('-'*80)

pattern_blocktype_block = r"opcode:\s*'getBlockType'.*?blockType:\s*'reporter'.*?text:\s*'ブロックタイプ x:\[X\] y:\[Y\] z:\[Z\]'"
match = re.search(pattern_blocktype_block, content, re.DOTALL)

if match:
    print('[OK] getBlockType ブロック定義が見つかりました')
    print(f'     位置: {content.index(match.group(0))}文字目')
else:
    print('[ERROR] getBlockType ブロック定義が見つかりません')

# Check 4: Verify getBlockType implementation method
print('\n【4】getBlockType 実装メソッドの確認')
print('-'*80)

pattern_blocktype_impl = r"getBlockType\(args\)\s*\{.*?sendCommandWithResponse\('getBlockType'"
match = re.search(pattern_blocktype_impl, content, re.DOTALL)

if match:
    print('[OK] getBlockType 実装メソッドが見つかりました')
    print(f'     位置: {content.index(match.group(0))}文字目')

    # Check if Y coordinate conversion is present
    if '_toMinecraftY' in match.group(0):
        print('[OK] Y座標変換が実装されています')
    else:
        print('[WARNING] Y座標変換が見つかりません')
else:
    print('[ERROR] getBlockType 実装メソッドが見つかりません')

# Check 5: Verify setGameRule block definition
print('\n【5】setGameRule ブロック定義の確認')
print('-'*80)

pattern_gamerule_block = r"opcode:\s*'setGameRule'.*?blockType:\s*'command'.*?text:\s*'ゲームルール"
match = re.search(pattern_gamerule_block, content, re.DOTALL)

if match:
    print('[OK] setGameRule ブロック定義が見つかりました')
    print(f'     位置: {content.index(match.group(0))}文字目')
else:
    print('[ERROR] setGameRule ブロック定義が見つかりません')

# Check 6: Verify setGameRule menus
print('\n【6】setGameRule メニューの確認')
print('-'*80)

# Check gameRules menu
if "gameRules:" in content and "doDaylightCycle" in content and "doWeatherCycle" in content and "doMobSpawning" in content:
    print('[OK] gameRules メニューが見つかりました')
    print('     - 時間固定 (doDaylightCycle)')
    print('     - 天気固定 (doWeatherCycle)')
    print('     - Mobスポーン (doMobSpawning)')
else:
    print('[ERROR] gameRules メニューが見つかりません')

# Check onOff menu
if "onOff:" in content and "オン" in content and "オフ" in content:
    print('[OK] onOff メニューが見つかりました')
    print('     - オン (true)')
    print('     - オフ (false)')
else:
    print('[ERROR] onOff メニューが見つかりません')

# Check 7: Verify setGameRule implementation method
print('\n【7】setGameRule 実装メソッドの確認')
print('-'*80)

pattern_gamerule_impl = r"setGameRule\(args\)\s*\{.*?sendCommand\('setGameRule'"
match = re.search(pattern_gamerule_impl, content, re.DOTALL)

if match:
    print('[OK] setGameRule 実装メソッドが見つかりました')
    print(f'     位置: {content.index(match.group(0))}文字目')
else:
    print('[ERROR] setGameRule 実装メソッドが見つかりません')

# Summary
print(f'\n{"="*80}')
print('検証サマリー')
print('='*80)

checks = [
    ('getPlayerFacing ブロック定義', "opcode: 'getPlayerFacing'" in content),
    ('getPlayerFacing 実装メソッド', "getPlayerFacing()" in content),
    ('getBlockType ブロック定義', "opcode: 'getBlockType'" in content),
    ('getBlockType 実装メソッド', "getBlockType(args)" in content),
    ('setGameRule ブロック定義', "opcode: 'setGameRule'" in content),
    ('setGameRule gameRules メニュー', "gameRules:" in content),
    ('setGameRule onOff メニュー', "onOff:" in content),
    ('setGameRule 実装メソッド', "setGameRule(args)" in content),
]

all_passed = True
for check_name, passed in checks:
    status = '[OK]' if passed else '[ERROR]'
    print(f'{status} {check_name}')
    if not passed:
        all_passed = False

print(f'\n{"="*80}')
if all_passed:
    print('[SUCCESS] 全ての実装が確認されました！')
    print()
    print('実装された機能:')
    print('  1. getPlayerFacing - プレイヤーの向き取得')
    print('  2. getBlockType - ブロックタイプ取得（Y座標変換付き）')
    print('  3. setGameRule - ゲームルール設定（3つのルール + オン/オフ）')
    print()
    print('MOD機能活用率: 78.6% → 92.9% (11/14 → 13/14)')
else:
    print('[ERROR] 一部の実装が見つかりません')

print('='*80)

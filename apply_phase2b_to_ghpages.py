import re
import shutil
from datetime import datetime

print('='*80)
print('フェーズ2B: gh-pagesブランチへの適用')
print('='*80)

# Backup timestamp
timestamp = datetime.now().strftime('%Y%m%d_%H%M%S')

# Files to modify
files_to_modify = [
    'player.js',
    'gui.js',
    'blocksonly.js',
    'compatibilitytesting.js'
]

print(f'\n修正対象ファイル: {len(files_to_modify)}個')
for f in files_to_modify:
    print(f'  - {f}')

# Modification functions
def add_getPlayerFacing_block(content):
    """Add getPlayerFacing block definition after getPosition"""

    # Pattern to find getPosition block
    pattern = r"(opcode:\s*'getPosition',\s*blockType:\s*'reporter',\s*text:\s*'プレイヤーの位置\s*\[COORD\]'[^}]+\}[^}]+\}),(\s*\{)"

    # Replacement with getPlayerFacing added
    replacement = r"\1,\n        {\n          opcode: 'getPlayerFacing',\n          blockType: 'reporter',\n          text: 'プレイヤーの向き'\n        },\2"

    return re.sub(pattern, replacement, content, count=1)

def add_getBlockType_block(content):
    """Add getBlockType block definition after getPlayerFacing"""

    # Pattern to find getPlayerFacing block
    pattern = r"(opcode:\s*'getPlayerFacing',\s*blockType:\s*'reporter',\s*text:\s*'プレイヤーの向き'\s*\}),(\s*\{)"

    # Replacement with getBlockType added
    replacement = r"""\1,
        {
          opcode: 'getBlockType',
          blockType: 'reporter',
          text: 'ブロックタイプ x:[X] y:[Y] z:[Z]',
          arguments: {
            X: {
              type: 'number',
              defaultValue: 0
            },
            Y: {
              type: 'number',
              defaultValue: 64
            },
            Z: {
              type: 'number',
              defaultValue: 0
            }
          }
        },\2"""

    return re.sub(pattern, replacement, content, count=1)

def add_setGameRule_block(content):
    """Add setGameRule block definition after setTime"""

    # Pattern to find setTime block
    pattern = r"(opcode:\s*'setTime',\s*blockType:\s*'command',\s*text:\s*'時刻を\s*\[TIME\]\s*にする'[^}]+menu:\s*'timeValues'[^}]+\}[^}]+\}),(\s*\{)"

    # Replacement with setGameRule added
    replacement = r"""\1,
        {
          opcode: 'setGameRule',
          blockType: 'command',
          text: 'ゲームルール [RULE] を [VALUE] にする',
          arguments: {
            RULE: {
              type: 'string',
              menu: 'gameRules',
              defaultValue: 'doDaylightCycle'
            },
            VALUE: {
              type: 'string',
              menu: 'onOff',
              defaultValue: 'true'
            }
          }
        },\2"""

    return re.sub(pattern, replacement, content, count=1)

def add_menus(content):
    """Add gameRules and onOff menus after timeValues"""

    # Pattern to find timeValues menu
    pattern = r"(timeValues:\s*\{[^}]+items:\s*\[[^\]]+\][^}]+\}),(\s*\w+:)"

    # Replacement with new menus added
    replacement = r"""\1,
        gameRules: {
          acceptReporters: false,
          items: [
            {text: '時間固定', value: 'doDaylightCycle'},
            {text: '天気固定', value: 'doWeatherCycle'},
            {text: 'Mobスポーン', value: 'doMobSpawning'}
          ]
        },
        onOff: {
          acceptReporters: false,
          items: [
            {text: 'オン', value: 'true'},
            {text: 'オフ', value: 'false'}
          ]
        },\2"""

    return re.sub(pattern, replacement, content, count=1)

def add_getPlayerFacing_method(content):
    """Add getPlayerFacing implementation method after getPosition"""

    # Pattern to find end of getPosition method
    pattern = r"(getPosition\([^)]*\)\s*\{[^}]+sendCommandWithResponse\('getPosition'[^}]+\}\s*catch[^}]+\}\s*\);?\s*\})"

    # Replacement with getPlayerFacing method added
    replacement = r"""\1

    getPlayerFacing() {
      return this.sendCommandWithResponse('getPlayerFacing', {})
        .then(response => {
          if (response && response.payload && response.payload.result) {
            const {result} = response.payload;
            return result.facing || 'north';
          }
          return 'north';
        })
        .catch(error => {
          console.error('getPlayerFacing error:', error);
          return 'north';
        });
    }"""

    return re.sub(pattern, replacement, content, count=1, flags=re.DOTALL)

def add_getBlockType_method(content):
    """Add getBlockType implementation method after getPlayerFacing"""

    # Pattern to find end of getPlayerFacing method
    pattern = r"(getPlayerFacing\(\)\s*\{[^}]+sendCommandWithResponse\('getPlayerFacing'[^}]+\}\s*catch[^}]+\}\s*\);?\s*\})"

    # Replacement with getBlockType method added
    replacement = r"""\1

    getBlockType(args) {
      const minecraftY = this._toMinecraftY(args.Y);
      return this.sendCommandWithResponse('getBlockType', {
        x: args.X,
        y: minecraftY,
        z: args.Z
      })
        .then(response => {
          if (response && response.payload && response.payload.result) {
            const {result} = response.payload;
            return result.blockType || 'air';
          }
          return 'air';
        })
        .catch(error => {
          console.error('getBlockType error:', error);
          return 'air';
        });
    }"""

    return re.sub(pattern, replacement, content, count=1, flags=re.DOTALL)

def add_setGameRule_method(content):
    """Add setGameRule implementation method after setTime"""

    # Pattern to find end of setTime method
    pattern = r"(setTime\([^)]*\)\s*\{[^}]+sendCommand\('setTime'[^}]+\);?\s*\})"

    # Replacement with setGameRule method added
    replacement = r"""\1

    setGameRule(args) {
      return this.sendCommand('setGameRule', {
        rule: args.RULE,
        value: args.VALUE
      });
    }"""

    return re.sub(pattern, replacement, content, count=1, flags=re.DOTALL)

# Apply modifications to each file
print(f'\n{"="*80}')
print('ファイルの修正')
print('='*80)

for filename in files_to_modify:
    print(f'\n【{filename}】')
    print('-'*80)

    try:
        # Backup
        backup_file = f'{filename}.backup_{timestamp}'
        shutil.copy(filename, backup_file)
        print(f'[OK] バックアップ作成: {backup_file}')

        # Read file
        with open(filename, 'r', encoding='utf-8') as f:
            content = f.read()

        original_length = len(content)

        # Apply modifications in order
        modifications = [
            ('getPlayerFacing block', add_getPlayerFacing_block),
            ('getBlockType block', add_getBlockType_block),
            ('setGameRule block', add_setGameRule_block),
            ('menus', add_menus),
            ('getPlayerFacing method', add_getPlayerFacing_method),
            ('getBlockType method', add_getBlockType_method),
            ('setGameRule method', add_setGameRule_method),
        ]

        for mod_name, mod_func in modifications:
            try:
                new_content = mod_func(content)
                if len(new_content) > len(content):
                    print(f'[OK] {mod_name} を追加')
                    content = new_content
                else:
                    print(f'[SKIP] {mod_name} (既に存在または見つかりません)')
            except Exception as e:
                print(f'[ERROR] {mod_name} の追加に失敗: {e}')

        # Write modified content
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(content)

        new_length = len(content)
        added_chars = new_length - original_length

        print(f'[OK] ファイル更新完了')
        print(f'     元のサイズ: {original_length:,} bytes')
        print(f'     新しいサイズ: {new_length:,} bytes')
        print(f'     追加: +{added_chars:,} bytes')

    except Exception as e:
        print(f'[ERROR] {filename} の処理に失敗: {e}')

print(f'\n{"="*80}')
print('フェーズ2B: gh-pagesへの適用完了')
print('='*80)

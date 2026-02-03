#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add prepareForLearning implementation to gui.js
"""

import re

def main():
    print("=== Adding prepareForLearning implementation ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # Check if already added
    if 'prepareForLearning()' in content:
        print("prepareForLearning implementation already exists")
        return

    # Find the location after setGameMode implementation
    # Look for the pattern: end of setGameMode function followed by blockBuilding
    pattern = r"(console\.error\('ゲームモード設定エラー:', error\);\s*throw error;\s*}\);\s*}\s*\n\s*/\*\*\s*\n\s*\* 建築ブロック選択)"

    replacement = r"""console.error('ゲームモード設定エラー:', error);
      throw error;
    });
  }

  /**
   * 学習準備
   * 時刻を正午に固定、天気をクリア、モブスポーンと昼夜/天気サイクルを無効化
   */
  prepareForLearning() {
    console.log('学習準備開始: 時刻固定、天気固定、モブスポーン無効化');
    return Promise.all([
      this.sendCommand('setTime', { time: 6000 }),
      this.sendCommand('setWeather', { weather: 'clear' }),
      this.sendCommand('setGameRule', { rule: 'doDaylightCycle', value: 'false' }),
      this.sendCommand('setGameRule', { rule: 'doWeatherCycle', value: 'false' }),
      this.sendCommand('setGameRule', { rule: 'doMobSpawning', value: 'false' })
    ]).then(() => {
      console.log('学習準備完了');
    }).catch(error => {
      console.error('学習準備エラー:', error);
      throw error;
    });
  }

  /**
   * 建築ブロック選択"""

    if re.search(pattern, content):
        content = re.sub(pattern, replacement, content)
        print("Found pattern and replaced")
    else:
        # Alternative: insert after setGameMode function using simpler pattern
        # Find "  }\n\n  /**\n   * 建築ブロック選択" after setGameMode
        alt_pattern = r"(throw error;\s*\n\s*}\);\s*\n\s*}\s*\n)\s*(/\*\*\s*\n\s*\* 建築ブロック選択)"

        alt_replacement = r"""\1
  /**
   * 学習準備
   * 時刻を正午に固定、天気をクリア、モブスポーンと昼夜/天気サイクルを無効化
   */
  prepareForLearning() {
    console.log('学習準備開始: 時刻固定、天気固定、モブスポーン無効化');
    return Promise.all([
      this.sendCommand('setTime', { time: 6000 }),
      this.sendCommand('setWeather', { weather: 'clear' }),
      this.sendCommand('setGameRule', { rule: 'doDaylightCycle', value: 'false' }),
      this.sendCommand('setGameRule', { rule: 'doWeatherCycle', value: 'false' }),
      this.sendCommand('setGameRule', { rule: 'doMobSpawning', value: 'false' })
    ]).then(() => {
      console.log('学習準備完了');
    }).catch(error => {
      console.error('学習準備エラー:', error);
      throw error;
    });
  }

  \2"""

        if re.search(alt_pattern, content):
            content = re.sub(alt_pattern, alt_replacement, content)
            print("Found alternative pattern and replaced")
        else:
            print("Could not find insertion point")
            return

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print("=== Done ===")


if __name__ == '__main__':
    main()

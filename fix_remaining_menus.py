#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fix remaining hardcoded Japanese menu items to use i18n
"""

import re

# Menu items to convert
MENU_ITEMS = {
    # gameRules
    "text: 'モブスポーン'": "text: { id: 'minecraft.menu.mobSpawning', default: 'Mob Spawning' }",
    "text: '時間固定'": "text: { id: 'minecraft.menu.daylightCycle', default: 'Daylight Cycle' }",
    "text: '天気固定'": "text: { id: 'minecraft.menu.weatherCycle', default: 'Weather Cycle' }",

    # gameModes
    "text: 'サバイバル'": "text: { id: 'minecraft.menu.survival', default: 'Survival' }",
    "text: 'クリエイティブ'": "text: { id: 'minecraft.menu.creative', default: 'Creative' }",
    "text: 'アドベンチャー'": "text: { id: 'minecraft.menu.adventure', default: 'Adventure' }",
    "text: 'スペクテイター'": "text: { id: 'minecraft.menu.spectator', default: 'Spectator' }",

    # timeValues
    "text: '朝'": "text: { id: 'minecraft.menu.day', default: 'Day' }",
    "text: '昼'": "text: { id: 'minecraft.menu.noon', default: 'Noon' }",
    "text: '夕方'": "text: { id: 'minecraft.menu.sunset', default: 'Sunset' }",
    "text: '夜'": "text: { id: 'minecraft.menu.night', default: 'Night' }",
    "text: '真夜中'": "text: { id: 'minecraft.menu.midnight', default: 'Midnight' }",

    # blockPlacement
    "text: '通常（下）'": "text: { id: 'minecraft.menu.bottom', default: 'Normal (Bottom)' }",
    "text: '上下反転（上）'": "text: { id: 'minecraft.menu.top', default: 'Flipped (Top)' }",
    "text: 'ダブル'": "text: { id: 'minecraft.menu.double', default: 'Double' }",

    # blockFacing
    "text: 'デフォルト'": "text: { id: 'minecraft.menu.default', default: 'Default' }",
    "text: '北'": "text: { id: 'minecraft.menu.north', default: 'North' }",
    "text: '南'": "text: { id: 'minecraft.menu.south', default: 'South' }",
    "text: '東'": "text: { id: 'minecraft.menu.east', default: 'East' }",
    "text: '西'": "text: { id: 'minecraft.menu.west', default: 'West' }",
}

# Translations for these menu items
TRANSLATIONS = {
    'en': {
        'minecraft.menu.mobSpawning': 'Mob Spawning',
        'minecraft.menu.daylightCycle': 'Daylight Cycle',
        'minecraft.menu.weatherCycle': 'Weather Cycle',
        'minecraft.menu.survival': 'Survival',
        'minecraft.menu.creative': 'Creative',
        'minecraft.menu.adventure': 'Adventure',
        'minecraft.menu.spectator': 'Spectator',
        'minecraft.menu.day': 'Day',
        'minecraft.menu.noon': 'Noon',
        'minecraft.menu.sunset': 'Sunset',
        'minecraft.menu.night': 'Night',
        'minecraft.menu.midnight': 'Midnight',
        'minecraft.menu.bottom': 'Normal (Bottom)',
        'minecraft.menu.top': 'Flipped (Top)',
        'minecraft.menu.double': 'Double',
        'minecraft.menu.default': 'Default',
        'minecraft.menu.north': 'North',
        'minecraft.menu.south': 'South',
        'minecraft.menu.east': 'East',
        'minecraft.menu.west': 'West',
    },
    'ja': {
        'minecraft.menu.mobSpawning': 'モブスポーン',
        'minecraft.menu.daylightCycle': '時間固定',
        'minecraft.menu.weatherCycle': '天気固定',
        'minecraft.menu.survival': 'サバイバル',
        'minecraft.menu.creative': 'クリエイティブ',
        'minecraft.menu.adventure': 'アドベンチャー',
        'minecraft.menu.spectator': 'スペクテイター',
        'minecraft.menu.day': '朝',
        'minecraft.menu.noon': '昼',
        'minecraft.menu.sunset': '夕方',
        'minecraft.menu.night': '夜',
        'minecraft.menu.midnight': '真夜中',
        'minecraft.menu.bottom': '通常（下）',
        'minecraft.menu.top': '上下反転（上）',
        'minecraft.menu.double': 'ダブル',
        'minecraft.menu.default': 'デフォルト',
        'minecraft.menu.north': '北',
        'minecraft.menu.south': '南',
        'minecraft.menu.east': '東',
        'minecraft.menu.west': '西',
    },
    'ja-Hira': {
        'minecraft.menu.mobSpawning': 'もぶすぽーん',
        'minecraft.menu.daylightCycle': 'じかんこてい',
        'minecraft.menu.weatherCycle': 'てんきこてい',
        'minecraft.menu.survival': 'さばいばる',
        'minecraft.menu.creative': 'くりえいてぃぶ',
        'minecraft.menu.adventure': 'あどべんちゃー',
        'minecraft.menu.spectator': 'すぺくていたー',
        'minecraft.menu.day': 'あさ',
        'minecraft.menu.noon': 'ひる',
        'minecraft.menu.sunset': 'ゆうがた',
        'minecraft.menu.night': 'よる',
        'minecraft.menu.midnight': 'まよなか',
        'minecraft.menu.bottom': 'つうじょう（した）',
        'minecraft.menu.top': 'うえしたはんてん（うえ）',
        'minecraft.menu.double': 'だぶる',
        'minecraft.menu.default': 'でふぉると',
        'minecraft.menu.north': 'きた',
        'minecraft.menu.south': 'みなみ',
        'minecraft.menu.east': 'ひがし',
        'minecraft.menu.west': 'にし',
    },
}


def main():
    print("=== Fixing remaining hardcoded menu items ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace hardcoded text with formatMessage objects
    for old, new in MENU_ITEMS.items():
        if old in content:
            content = content.replace(old, new)
            print(f"  Replaced: {old}")

    # Add translations to language sections
    for lang, translations in TRANSLATIONS.items():
        # Find the language section
        pattern = rf'  "{re.escape(lang)}": \{{'
        match = re.search(pattern, content)

        if match:
            insert_pos = match.end()

            # Build translation string
            trans_lines = []
            for key, value in translations.items():
                escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')
                trans_lines.append(f'    "{key}": "{escaped_value}"')

            trans_string = '\n' + ',\n'.join(trans_lines) + ',\n'

            # Insert after the opening brace
            content = content[:insert_pos] + trans_string + content[insert_pos:]
            print(f"  Added menu translations to {lang}")
        else:
            print(f"  Warning: Could not find section for {lang}")

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print("\n=== Done ===")


if __name__ == '__main__':
    main()

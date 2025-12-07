#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add extra Minecraft command translations that were missing
"""

import re

# Extra translations - English defaults
EXTRA_TRANSLATIONS_EN = {
    "minecraft.chat": "say [MESSAGE]",
    "minecraft.getPosition": "[COORD]",
    "minecraft.setBlock": "place block at X:[X] Y:[Y] Z:[Z] block:[BLOCK] placement:[PLACEMENT] facing:[FACING]",
    "minecraft.clearArea": "clear area X:[X] Z:[Z]",
    "minecraft.clearAllEntities": "clear all entities X:[X] Z:[Z]",
    "minecraft.setGameRule": "set game rule [RULE] to [VALUE]",
    "minecraft.setGameMode": "set game mode to [MODE]",
    "minecraft.summonEntity": "summon entity [ENTITY] at X:[X] Y:[Y] Z:[Z]",
}

# Languages that already have these translations
SKIP_LANGS = {'en', 'ja'}

# All language codes
ALL_LANGS = [
    'ab', 'af', 'ar', 'am', 'an', 'az', 'id', 'bn', 'be', 'bg', 'ca', 'cs', 'cy', 'da', 'de',
    'et', 'el', 'es', 'eo', 'eu', 'fa', 'fr', 'fy', 'ga', 'gd', 'gl', 'ko', 'ha', 'hy', 'he',
    'hi', 'hr', 'xh', 'zu', 'is', 'it', 'ka', 'kk', 'qu', 'sw', 'ht', 'ku', 'lv', 'lt', 'hu',
    'mi', 'mn', 'nl', 'ja-Hira', 'nb', 'nn', 'oc', 'or', 'uz', 'th', 'km', 'pl', 'pt', 'pt-br',
    'ro', 'ru', 'tn', 'sk', 'sl', 'sr', 'fi', 'sv', 'vi', 'tr', 'uk', 'zh-cn', 'zh-tw'
]


def main():
    print("=== Adding extra translations for all languages ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    added_count = 0

    for lang in ALL_LANGS:
        if lang in SKIP_LANGS:
            continue

        # Find the language section
        pattern = rf'  "{re.escape(lang)}": \{{'
        match = re.search(pattern, content)

        if not match:
            print(f"  Warning: Could not find section for {lang}")
            continue

        insert_pos = match.end()

        # Check if these translations already exist
        section_start = match.start()
        section_preview = content[section_start:section_start + 5000]
        if '"minecraft.chat"' in section_preview:
            print(f"  Skipping {lang} (already has extra translations)")
            continue

        # Build translation string
        trans_lines = []
        for key, value in EXTRA_TRANSLATIONS_EN.items():
            escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')
            trans_lines.append(f'    "{key}": "{escaped_value}"')

        trans_string = '\n' + ',\n'.join(trans_lines) + ',\n'

        # Insert after the opening brace
        content = content[:insert_pos] + trans_string + content[insert_pos:]
        added_count += 1
        print(f"  Added extra translations to {lang}")

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"\n=== Done - added to {added_count} languages ===")


if __name__ == '__main__':
    main()

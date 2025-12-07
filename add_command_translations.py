#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add Minecraft command translations for all languages
These are the block command texts like "connect to Minecraft", "place block", etc.
"""

import re

# Command translations - English defaults
# These will be used for all non-English, non-Japanese languages
COMMAND_TRANSLATIONS_EN = {
    "minecraft.connect": "connect to Minecraft host [HOST] port [PORT]",
    "minecraft.disconnect": "disconnect",
    "minecraft.place": "place block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeBuilding": "place building block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeLighting": "place lighting block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeDecoration": "place decoration block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeNature": "place nature block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeFunctional": "place functional block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeOre": "place ore block [BLOCK] at [X] [Y] [Z]",
    "minecraft.placeSpecial": "place special block [BLOCK] at [X] [Y] [Z]",
    "minecraft.fill": "fill from [X1] [Y1] [Z1] to [X2] [Y2] [Z2] with [BLOCK]",
    "minecraft.clone": "clone from [X1] [Y1] [Z1] to [X2] [Y2] [Z2] to [X3] [Y3] [Z3]",
    "minecraft.destroyBlock": "destroy block at [X] [Y] [Z]",
    "minecraft.teleport": "teleport to [X] [Y] [Z]",
    "minecraft.summon": "summon [ENTITY] at [X] [Y] [Z]",
    "minecraft.entityPassive": "passive entity [ENTITY]",
    "minecraft.entityNeutral": "neutral entity [ENTITY]",
    "minecraft.entityHostile": "hostile entity [ENTITY]",
    "minecraft.entityBoss": "boss entity [ENTITY]",
    "minecraft.entityAquatic": "aquatic entity [ENTITY]",
    "minecraft.entityVillager": "villager entity [ENTITY]",
    "minecraft.entityOther": "other entity [ENTITY]",
    "minecraft.setWeather": "set weather to [WEATHER]",
    "minecraft.setTime": "set time to [TIME]",
    "minecraft.getPlayerFacing": "player facing direction",
    "minecraft.getBlockType": "block type at [X] [Y] [Z]",
    "minecraft.isConnected": "connected?",
    "minecraft.blockBuilding": "building block [BLOCK]",
    "minecraft.blockLighting": "lighting block [BLOCK]",
    "minecraft.blockDecoration": "decoration block [BLOCK]",
    "minecraft.blockNature": "nature block [BLOCK]",
    "minecraft.blockFunctional": "functional block [BLOCK]",
    "minecraft.blockOre": "ore block [BLOCK]",
    "minecraft.blockSpecial": "special block [BLOCK]",
}

# Languages that already have command translations
SKIP_LANGS = {'en', 'ja'}

# All other language codes in the editor-msgs
ALL_LANGS = [
    'ab', 'af', 'ar', 'am', 'an', 'az', 'id', 'bn', 'be', 'bg', 'ca', 'cs', 'cy', 'da', 'de',
    'et', 'el', 'es', 'eo', 'eu', 'fa', 'fr', 'fy', 'ga', 'gd', 'gl', 'ko', 'ha', 'hy', 'he',
    'hi', 'hr', 'xh', 'zu', 'is', 'it', 'ka', 'kk', 'qu', 'sw', 'ht', 'ku', 'lv', 'lt', 'hu',
    'mi', 'mn', 'nl', 'ja-Hira', 'nb', 'nn', 'oc', 'or', 'uz', 'th', 'km', 'pl', 'pt', 'pt-br',
    'ro', 'ru', 'tn', 'sk', 'sl', 'sr', 'fi', 'sv', 'vi', 'tr', 'uk', 'zh-cn', 'zh-tw'
]


def main():
    print("=== Adding command translations for all languages ===")

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

        # Check if command translations already exist for this language
        # Look for minecraft.connect in the next 5000 characters
        section_start = match.start()
        section_preview = content[section_start:section_start + 5000]
        if '"minecraft.connect"' in section_preview:
            print(f"  Skipping {lang} (already has command translations)")
            continue

        # Build translation string
        trans_lines = []
        for key, value in COMMAND_TRANSLATIONS_EN.items():
            escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')
            trans_lines.append(f'    "{key}": "{escaped_value}"')

        trans_string = '\n' + ',\n'.join(trans_lines) + ',\n'

        # Insert after the opening brace
        content = content[:insert_pos] + trans_string + content[insert_pos:]
        added_count += 1
        print(f"  Added command translations to {lang}")

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"\n=== Done - added to {added_count} languages ===")


if __name__ == '__main__':
    main()

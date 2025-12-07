#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add Minecraft menu translations for all languages
"""

import re

# Menu translations - English defaults
MENU_TRANSLATIONS_EN = {
    "minecraft.menu.mobSpawning": "Mob Spawning",
    "minecraft.menu.daylightCycle": "Daylight Cycle",
    "minecraft.menu.weatherCycle": "Weather Cycle",
    "minecraft.menu.survival": "Survival",
    "minecraft.menu.creative": "Creative",
    "minecraft.menu.adventure": "Adventure",
    "minecraft.menu.spectator": "Spectator",
    "minecraft.menu.day": "Day",
    "minecraft.menu.noon": "Noon",
    "minecraft.menu.sunset": "Sunset",
    "minecraft.menu.night": "Night",
    "minecraft.menu.midnight": "Midnight",
    "minecraft.menu.bottom": "Normal (Bottom)",
    "minecraft.menu.top": "Flipped (Top)",
    "minecraft.menu.double": "Double",
    "minecraft.menu.default": "Default",
    "minecraft.menu.north": "North",
    "minecraft.menu.south": "South",
    "minecraft.menu.east": "East",
    "minecraft.menu.west": "West",
}

# Languages that already have menu translations
SKIP_LANGS = {'en', 'ja', 'ja-Hira'}

# All language codes
ALL_LANGS = [
    'ab', 'af', 'ar', 'am', 'an', 'az', 'id', 'bn', 'be', 'bg', 'ca', 'cs', 'cy', 'da', 'de',
    'et', 'el', 'es', 'eo', 'eu', 'fa', 'fr', 'fy', 'ga', 'gd', 'gl', 'ko', 'ha', 'hy', 'he',
    'hi', 'hr', 'xh', 'zu', 'is', 'it', 'ka', 'kk', 'qu', 'sw', 'ht', 'ku', 'lv', 'lt', 'hu',
    'mi', 'mn', 'nl', 'nb', 'nn', 'oc', 'or', 'uz', 'th', 'km', 'pl', 'pt', 'pt-br',
    'ro', 'ru', 'tn', 'sk', 'sl', 'sr', 'fi', 'sv', 'vi', 'tr', 'uk', 'zh-cn', 'zh-tw'
]


def main():
    print("=== Adding menu translations for all languages ===")

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

        # Check if menu translations already exist for this language
        section_start = match.start()
        section_preview = content[section_start:section_start + 3000]
        if '"minecraft.menu.survival"' in section_preview:
            print(f"  Skipping {lang} (already has menu translations)")
            continue

        # Build translation string
        trans_lines = []
        for key, value in MENU_TRANSLATIONS_EN.items():
            escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')
            trans_lines.append(f'    "{key}": "{escaped_value}"')

        trans_string = '\n' + ',\n'.join(trans_lines) + ',\n'

        # Insert after the opening brace
        content = content[:insert_pos] + trans_string + content[insert_pos:]
        added_count += 1
        print(f"  Added menu translations to {lang}")

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"\n=== Done - added to {added_count} languages ===")


if __name__ == '__main__':
    main()

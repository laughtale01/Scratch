#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add hiragana translations for ja-Hira
"""

import re

# Hiragana translations for menu items and commands
JA_HIRA_TRANSLATIONS = {
    # Menu items in hiragana
    "minecraft.menu.survival": "さばいばる",
    "minecraft.menu.creative": "くりえいてぃぶ",
    "minecraft.menu.adventure": "ぼうけん",
    "minecraft.menu.spectator": "かんせん",
    "minecraft.menu.north": "きた",
    "minecraft.menu.south": "みなみ",
    "minecraft.menu.east": "ひがし",
    "minecraft.menu.west": "にし",
    "minecraft.menu.day": "ひる",
    "minecraft.menu.noon": "しょうご",
    "minecraft.menu.sunset": "ゆうがた",
    "minecraft.menu.night": "よる",
    "minecraft.menu.midnight": "しんや",
    "minecraft.menu.bottom": "ふつう (した)",
    "minecraft.menu.top": "さかさま (うえ)",
    "minecraft.menu.double": "にじゅう",
    "minecraft.menu.default": "きほん",
    "minecraft.menu.mobSpawning": "もぶ すぽーん",
    "minecraft.menu.daylightCycle": "ひる と よる",
    "minecraft.menu.weatherCycle": "てんき",
    # Commands in hiragana
    "minecraft.connect": "Minecraft に つなぐ ほすと [HOST] ぽーと [PORT]",
    "minecraft.disconnect": "きる",
    "minecraft.place": "ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeBuilding": "たてもの ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeLighting": "ひかり ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeDecoration": "かざり ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeNature": "しぜん ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeFunctional": "きのう ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeOre": "こうせき ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.placeSpecial": "とくしゅ ぶろっく [BLOCK] を [X] [Y] [Z] に おく",
    "minecraft.fill": "[X1] [Y1] [Z1] から [X2] [Y2] [Z2] まで [BLOCK] で うめる",
    "minecraft.clone": "[X1] [Y1] [Z1] から [X2] [Y2] [Z2] を [X3] [Y3] [Z3] に こぴー",
    "minecraft.destroyBlock": "[X] [Y] [Z] の ぶろっく を こわす",
    "minecraft.teleport": "[X] [Y] [Z] に てれぽーと",
    "minecraft.summon": "[ENTITY] を [X] [Y] [Z] に よびだす",
    "minecraft.entityPassive": "おとなしい もぶ [ENTITY]",
    "minecraft.entityNeutral": "ちゅうりつ もぶ [ENTITY]",
    "minecraft.entityHostile": "てきたい もぶ [ENTITY]",
    "minecraft.entityBoss": "ぼす [ENTITY]",
    "minecraft.entityAquatic": "みずの もぶ [ENTITY]",
    "minecraft.entityVillager": "むらびと [ENTITY]",
    "minecraft.entityOther": "そのた もぶ [ENTITY]",
    "minecraft.setWeather": "てんき を [WEATHER] に する",
    "minecraft.setTime": "じかん を [TIME] に する",
    "minecraft.getPlayerFacing": "ぷれいやー の むき",
    "minecraft.getBlockType": "[X] [Y] [Z] の ぶろっく",
    "minecraft.isConnected": "つながってる?",
    "minecraft.blockBuilding": "たてもの ぶろっく [BLOCK]",
    "minecraft.blockLighting": "ひかり ぶろっく [BLOCK]",
    "minecraft.blockDecoration": "かざり ぶろっく [BLOCK]",
    "minecraft.blockNature": "しぜん ぶろっく [BLOCK]",
    "minecraft.blockFunctional": "きのう ぶろっく [BLOCK]",
    "minecraft.blockOre": "こうせき ぶろっく [BLOCK]",
    "minecraft.blockSpecial": "とくしゅ ぶろっく [BLOCK]",
    "minecraft.chat": "[MESSAGE] と いう",
    "minecraft.getPosition": "[COORD]",
    "minecraft.setBlock": "ぶろっく おく X:[X] Y:[Y] Z:[Z] ぶろっく:[BLOCK] おきかた:[PLACEMENT] むき:[FACING]",
    "minecraft.clearArea": "えりあ を きれいにする X:[X] Z:[Z]",
    "minecraft.clearAllEntities": "すべての もぶ を けす X:[X] Z:[Z]",
    "minecraft.setGameRule": "るーる [RULE] を [VALUE] に する",
    "minecraft.setGameMode": "げーむもーど を [MODE] に する",
    "minecraft.summonEntity": "もぶ [ENTITY] を X:[X] Y:[Y] Z:[Z] に よびだす",
}


def main():
    print("=== Adding hiragana translations for ja-Hira ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the ja-Hira section
    pattern = r'  "ja-Hira": \{'
    match = re.search(pattern, content)

    if not match:
        print("Could not find ja-Hira section")
        return

    section_start = match.start()

    # Find end of section
    next_lang = re.search(r'\n  \},\n  "[a-z]', content[section_start + 10:])
    if next_lang:
        section_end = section_start + 10 + next_lang.start() + 4
    else:
        section_end = content.find('\n  }\n}', section_start) + 4

    section = content[section_start:section_end]

    # Replace each translation
    replaced = 0
    for key, value in JA_HIRA_TRANSLATIONS.items():
        escaped_key = re.escape(key)
        escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')

        old_pattern = rf'"{escaped_key}": "[^"]*"'
        new_value = f'"{key}": "{escaped_value}"'

        if re.search(old_pattern, section):
            section = re.sub(old_pattern, new_value, section)
            replaced += 1
        else:
            # Key doesn't exist, need to add it
            # Find the first key and insert before it
            first_key_match = re.search(r'\n    "minecraft\.', section)
            if first_key_match:
                insert_pos = first_key_match.start()
                insert_str = f'\n    "{key}": "{escaped_value}",'
                section = section[:insert_pos] + insert_str + section[insert_pos:]
                replaced += 1

    content = content[:section_start] + section + content[section_end:]

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Updated ja-Hira with {replaced} translations")
    print("=== Done ===")


if __name__ == '__main__':
    main()

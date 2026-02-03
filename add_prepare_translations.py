#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add translations for prepareForLearning block
"""

import re

# Translations for "prepare for learning" in various languages
TRANSLATIONS = {
    "en": "prepare for learning",
    "ja": "学習準備",
    "ja-Hira": "がくしゅう じゅんび",
    "af": "Berei voor vir leer",
    "ar": "التحضير للتعلم",
    "az": "Öyrənməyə hazırlıq",
    "bg": "Подготовка за обучение",
    "ca": "Preparar per aprendre",
    "cs": "Připravit na učení",
    "cy": "Paratoi ar gyfer dysgu",
    "da": "Forbered til læring",
    "de": "Lernvorbereitung",
    "el": "Προετοιμασία για μάθηση",
    "es": "Preparar para aprender",
    "es-419": "Preparar para aprender",
    "et": "Õppimiseks ettevalmistus",
    "eu": "Ikasteko prestatu",
    "fa": "آماده‌سازی برای یادگیری",
    "fi": "Valmistaudu oppimiseen",
    "fr": "Préparer pour l'apprentissage",
    "ga": "Ullmhaigh don fhoghlaim",
    "gd": "Deasaich airson ionnsachadh",
    "gl": "Preparar para aprender",
    "he": "הכנה ללמידה",
    "hr": "Priprema za učenje",
    "hu": "Felkészülés tanulásra",
    "id": "Persiapan untuk belajar",
    "is": "Undirbúa nám",
    "it": "Prepara per l'apprendimento",
    "ko": "학습 준비",
    "lt": "Pasiruošimas mokymuisi",
    "lv": "Sagatavošanās mācībām",
    "mi": "Whakariterite mō te ako",
    "nb": "Forbered for læring",
    "nl": "Voorbereiden om te leren",
    "pl": "Przygotowanie do nauki",
    "pt": "Preparar para aprender",
    "pt-br": "Preparar para aprender",
    "ro": "Pregătire pentru învățare",
    "ru": "Подготовка к обучению",
    "sk": "Príprava na učenie",
    "sl": "Priprava na učenje",
    "sr": "Припрема за учење",
    "sv": "Förbered för inlärning",
    "th": "เตรียมพร้อมสำหรับการเรียนรู้",
    "tr": "Öğrenmeye hazırlan",
    "uk": "Підготовка до навчання",
    "vi": "Chuẩn bị học tập",
    "zh-cn": "学习准备",
    "zh-tw": "學習準備",
    "zu": "Lungiselela ukufunda",
    "ckb": "ئامادەکاری بۆ فێربوون",
    "am": "ለመማር ዝግጅት",
    "bn": "শেখার জন্য প্রস্তুতি",
    "ha": "Shirya don koyo",
    "sw": "Jiandae kujifunza",
}


def main():
    print("=== Adding prepareForLearning translations ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    translation_key = "minecraft.prepareForLearning"
    added_count = 0

    for lang, translation in TRANSLATIONS.items():
        # Pattern to find language section and insert translation
        # Look for existing minecraft translations in this language section

        # First, check if translation already exists
        check_pattern = f'"{translation_key}":\\s*"[^"]*"'

        # Find language section pattern - look for the language followed by minecraft translations
        lang_section_pattern = f'"({lang})":\\s*\\{{'

        # Find a minecraft key in this language section to insert after
        search_pattern = f'("{lang}":\\s*\\{{[^}}]*?"minecraft\\.setGameMode":\\s*"[^"]*")'

        match = re.search(search_pattern, content, re.DOTALL)
        if match:
            # Check if translation already exists in this section
            section_start = match.start()
            section_end = content.find('\n  },\n  "', section_start + 1)
            if section_end == -1:
                section_end = content.find('\n  }\n}', section_start + 1)

            section = content[section_start:section_end] if section_end > section_start else ""

            if f'"{translation_key}"' in section:
                continue  # Already exists

            # Insert after minecraft.setGameMode
            insert_after = f'"minecraft.setGameMode": "'
            insert_pos = content.find(insert_after, section_start)
            if insert_pos != -1 and (section_end == -1 or insert_pos < section_end):
                # Find end of this line
                line_end = content.find('",\n', insert_pos)
                if line_end != -1:
                    # Get the indentation
                    line_start = content.rfind('\n', 0, insert_pos)
                    indent = ""
                    if line_start != -1:
                        spaces = content[line_start+1:insert_pos]
                        indent = spaces[:len(spaces) - len(spaces.lstrip())]

                    # Insert new translation
                    escaped_translation = translation.replace('\\', '\\\\').replace('"', '\\"')
                    new_line = f',\n{indent}"{translation_key}": "{escaped_translation}"'
                    content = content[:line_end+1] + new_line + content[line_end+1:]
                    added_count += 1

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Added {added_count} translations")
    print("=== Done ===")


if __name__ == '__main__':
    main()

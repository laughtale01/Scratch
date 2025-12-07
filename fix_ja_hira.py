#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fix ja-Hira section by removing kanji block translations
Let it fall back to English defaults for block names
"""

import re

def main():
    print("=== Fixing ja-Hira section ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # Find the ja-Hira section
    # We need to remove all minecraft.block.* entries from this section
    # but keep the minecraft.menu.* entries (which are in hiragana)

    # Pattern to find ja-Hira section
    ja_hira_start = content.find('"ja-Hira": {')
    if ja_hira_start == -1:
        print("Could not find ja-Hira section")
        return

    # Find the end of this section (next language section or end)
    # Look for the pattern },\n  "xx": { or just }
    section_end = content.find('\n  },\n  "', ja_hira_start + 12)
    if section_end == -1:
        section_end = content.find('\n  }\n}', ja_hira_start + 12)

    if section_end == -1:
        print("Could not find end of ja-Hira section")
        return

    # Extract the section content
    section_content = content[ja_hira_start:section_end]

    # Split into lines
    lines = section_content.split('\n')

    # Keep only lines that don't start with "minecraft.block.
    filtered_lines = []
    removed_count = 0
    for line in lines:
        if '"minecraft.block.' in line:
            removed_count += 1
        else:
            filtered_lines.append(line)

    new_section = '\n'.join(filtered_lines)

    # Replace the section
    content = content[:ja_hira_start] + new_section + content[section_end:]

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print(f"Removed {removed_count} kanji block translations from ja-Hira")
    print("Block names will now fall back to English")
    print("=== Done ===")

if __name__ == '__main__':
    main()

#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fix newline characters in translations that break JSON syntax
"""

import re

def main():
    print("=== Fixing newline issues in translations ===")

    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # Find and fix broken translation lines
    # Pattern: "minecraft.block.xxx": "text without closing quote
    # followed by newline and then ", or just "

    # First, let's find all minecraft.block translations that span multiple lines
    pattern = r'("minecraft\.block\.[^"]+": ")([^"]*)\n([^"]*)"'

    def fix_match(m):
        key = m.group(1)
        text1 = m.group(2)
        text2 = m.group(3)
        # Combine the text parts, replacing newline with space
        combined = text1.strip() + ' ' + text2.strip()
        return f'{key}{combined}"'

    count = len(re.findall(pattern, content))
    print(f"Found {count} broken translations")

    content = re.sub(pattern, fix_match, content)

    # Also fix any remaining issues where newline is in the value
    # Look for lines that start with just ", (orphaned closing quotes)
    orphan_pattern = r'\n",\n'
    orphan_count = content.count(orphan_pattern)
    if orphan_count > 0:
        print(f"Found {orphan_count} orphaned quotes")
        # These need more careful handling - remove the orphaned lines
        content = content.replace('\n",\n', '",\n')

    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print("=== Done ===")

if __name__ == '__main__':
    main()

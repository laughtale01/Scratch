#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Find syntax errors in gui.js translations
"""

import re

def main():
    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()
        lines = content.split('\n')

    errors = []

    for i, line in enumerate(lines, 1):
        if 'minecraft.block' in line or 'minecraft.entity' in line:
            # Check for control characters (except newline which we already split on)
            for j, c in enumerate(line):
                if ord(c) < 32 and c not in '\r\t':
                    errors.append(f'Line {i}: Control char ord={ord(c)} at pos {j}')
                    break

            # Check for unescaped quotes inside strings
            # Pattern: "key": "value with " unescaped"
            if '": "' in line:
                # Extract the value part
                match = re.search(r'": "(.*)$', line)
                if match:
                    value = match.group(1)
                    # Count quotes - should end with exactly one " and possibly ,
                    if value.endswith('",'):
                        value = value[:-2]
                    elif value.endswith('"'):
                        value = value[:-1]
                    else:
                        errors.append(f'Line {i}: Missing closing quote: {line[:80]}')
                        continue

                    # Check for unescaped quotes in value
                    unescaped = re.findall(r'(?<!\\)"', value)
                    if unescaped:
                        errors.append(f'Line {i}: Unescaped quote in value: {line[:80]}')

    # Also look for lines that look like broken translations
    # (lines that start with ", which would be orphaned closing quotes)
    for i, line in enumerate(lines, 1):
        stripped = line.strip()
        if stripped == '",' or stripped == '"':
            errors.append(f'Line {i}: Orphaned quote: {line}')

    print(f"Found {len(errors)} potential issues:")
    for error in errors[:50]:  # Show first 50
        print(error)

if __name__ == '__main__':
    main()

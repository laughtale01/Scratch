#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fix dynamic menu methods to use formatMessage objects instead of calling formatMessage directly
"""

def main():
    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # The old pattern uses formatMessage(...) which returns a string
    old = "text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' })"

    # The new pattern returns an object directly, which Scratch's maybeFormatMessage will process
    new = "text: { id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ') }"

    count = content.count(old)
    print(f'Found {count} occurrences to replace')

    if count > 0:
        content = content.replace(old, new)

        with open('gui.js', 'w', encoding='utf-8') as f:
            f.write(content)

        print(f'Replaced {count} occurrences')
    else:
        print('No matches found - pattern may have already been fixed or differs')
        # Let's search for a shorter pattern to see what's there
        test = "text: formatMessage({ id: `minecraft.block"
        test_count = content.count(test)
        print(f'Found {test_count} partial matches for "text: formatMessage({{ id: `minecraft.block"')

if __name__ == '__main__':
    main()

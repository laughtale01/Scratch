#!/usr/bin/env python3
import re

# Read the updated BUILDING_BLOCKS from gui.js
with open('gui.js', 'r', encoding='utf-8') as f:
    gui_content = f.read()

# Extract BUILDING_BLOCKS section from gui.js
match = re.search(r'(const BUILDING_BLOCKS = \[.*?\n\}];)', gui_content, re.DOTALL)
if not match:
    print("ERROR: Could not find BUILDING_BLOCKS in gui.js")
    exit(1)

new_building_blocks = match.group(1)
print(f"Extracted BUILDING_BLOCKS section ({len(new_building_blocks)} characters)")

# Update each of the remaining files
files_to_update = ['blocksonly.js', 'compatibilitytesting.js', 'player.js']

for filename in files_to_update:
    print(f"\nUpdating {filename}...")

    with open(filename, 'r', encoding='utf-8') as f:
        content = f.read()

    # Replace BUILDING_BLOCKS section
    updated_content = re.sub(
        r'const BUILDING_BLOCKS = \[.*?\n\}];',
        new_building_blocks,
        content,
        count=1,
        flags=re.DOTALL
    )

    if updated_content == content:
        print(f"  WARNING: No changes made to {filename}")
    else:
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(updated_content)
        print(f"  [OK] Successfully updated {filename}")

print("\nAll files updated!")

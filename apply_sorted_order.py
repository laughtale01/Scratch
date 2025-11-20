import re
import json

# Load sorted order from main branch
with open('sorted_categories.json', 'r', encoding='utf-8') as f:
    sorted_categories = json.load(f)

print("Sorted order loaded from sorted_categories.json")

# Files to update
files = ['player.js', 'gui.js', 'blocksonly.js', 'compatibilitytesting.js']

category_names = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS',
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']

# Format arrays
def format_array(cat_name, blocks):
    lines = [f"const {cat_name} = ["]
    for i, block in enumerate(blocks):
        text = block['text'].replace("'", "\\'")
        value = block['value']
        if i < len(blocks) - 1:
            lines.append(f"{{\n  text: '{text}',\n  value: '{value}'\n}}, ")
        else:
            lines.append(f"{{\n  text: '{text}',\n  value: '{value}'\n}}")
    lines.append("];")
    return ''.join(lines)

# Update each file
for filename in files:
    print(f"\nUpdating {filename}...")

    try:
        with open(filename, 'r', encoding='utf-8') as f:
            content = f.read()

        new_content = content

        for cat_name in category_names:
            if cat_name in sorted_categories:
                blocks = sorted_categories[cat_name]
                new_array = format_array(cat_name, blocks)

                pattern = rf"const {cat_name} = \[.*?\];"
                new_content = re.sub(pattern, new_array, new_content, flags=re.DOTALL)

        with open(filename, 'w', encoding='utf-8') as f:
            f.write(new_content)

        print(f"  [OK] {filename} updated")

    except Exception as e:
        print(f"  [ERROR] {e}")

print("\nAll files updated with logical ordering!")

import re

# Files to update  
files = ['player.js', 'gui.js', 'blocksonly.js', 'compatibilitytesting.js']

# Get original order from commit 0b24f38
import subprocess
result = subprocess.run(['git', 'show', '0b24f38:player.js'], 
                       capture_output=True, text=True, encoding='utf-8')
original_content = result.stdout

# Priority order for deduplication
priority_order = [
    'LIGHTING_BLOCKS',
    'ORE_BLOCKS',
    'FUNCTIONAL_BLOCKS',
    'SPECIAL_BLOCKS',
    'DECORATION_BLOCKS',
    'NATURE_BLOCKS',
    'BUILDING_BLOCKS'
]

category_names = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS', 
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']

print("=" * 80)
print("DEDUPLICATING WITH ORIGINAL ORDER PRESERVED")
print("=" * 80)

# Extract original order with comments from 0b24f38
original_categories = {}

for cat_name in category_names:
    pattern = rf"const {cat_name} = \[(.*?)\];"
    match = re.search(pattern, original_content, re.DOTALL)
    
    if match:
        array_content = match.group(1)
        
        # Parse blocks and comments
        blocks_in_order = []
        lines = array_content.split('\n')
        
        current_comment = None
        for line in lines:
            # Check for comment
            comment_match = re.search(r'//\s*(.+)', line)
            if comment_match:
                current_comment = comment_match.group(1).strip()
            
            # Check for block
            block_match = re.search(r"\{\s*text:\s*'([^']+)',\s*value:\s*'([^']+)'\s*\}", line)
            if block_match:
                text = block_match.group(1)
                value = block_match.group(2)
                blocks_in_order.append({
                    'text': text,
                    'value': value,
                    'comment': current_comment
                })
        
        original_categories[cat_name] = blocks_in_order

print(f"\nExtracted original order:")
for cat, blocks in original_categories.items():
    print(f"  {cat}: {len(blocks)} blocks")

# Build deduplication map
value_to_category = {}
for cat_name in priority_order:
    if cat_name in original_categories:
        for block in original_categories[cat_name]:
            value = block['value']
            if value not in value_to_category:
                value_to_category[value] = cat_name

# Build deduplicated categories preserving original order
deduplicated_categories = {}

for cat_name in category_names:
    if cat_name in original_categories:
        deduplicated_blocks = []
        for block in original_categories[cat_name]:
            # Only include if this category is the designated one for this block
            if value_to_category.get(block['value']) == cat_name:
                deduplicated_blocks.append(block)
        deduplicated_categories[cat_name] = deduplicated_blocks

print(f"\nDeduplicated categories:")
for cat, blocks in deduplicated_categories.items():
    print(f"  {cat}: {len(blocks)} blocks")

# Format category array with comments
def format_category_with_comments(cat_name, blocks):
    lines = [f"const {cat_name} = ["]
    
    last_comment = None
    for i, block in enumerate(blocks):
        # Add comment if it changed
        if block['comment'] != last_comment and block['comment']:
            if i > 0:
                lines.append("\n")
            lines.append(f"// {block['comment']}\n")
            last_comment = block['comment']
        
        text = block['text'].replace("'", "\'")
        value = block['value']
        
        if i < len(blocks) - 1:
            lines.append(f"{{\n  text: '{text}',\n  value: '{value}'\n}}, ")
        else:
            lines.append(f"{{\n  text: '{text}',\n  value: '{value}'\n}}")
    
    lines.append("];")
    return ''.join(lines)

# Update each file
for filename in files:
    print(f"\n--- Updating {filename} ---")
    
    try:
        with open(filename, 'r', encoding='utf-8') as f:
            content = f.read()
        
        new_content = content
        
        for cat_name in category_names:
            if cat_name in deduplicated_categories:
                blocks = deduplicated_categories[cat_name]
                new_array = format_category_with_comments(cat_name, blocks)
                
                pattern = rf"const {cat_name} = \[.*?\];"
                new_content = re.sub(pattern, new_array, new_content, flags=re.DOTALL)
        
        with open(filename, 'w', encoding='utf-8') as f:
            f.write(new_content)
        
        print(f"  [OK] Updated {filename}")
    
    except Exception as e:
        print(f"  [ERROR] {e}")

print("\n" + "=" * 80)
print("DEDUPLICATION COMPLETE - ORIGINAL ORDER PRESERVED")
print("=" * 80)

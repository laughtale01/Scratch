#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Fix Minecraft block menus to be dynamic for i18n support
"""

import re

def convert_value_to_english(value):
    """Convert minecraft block value to readable English name"""
    return value.replace('_', ' ').title()

def main():
    print("=== Fix Dynamic Menu for i18n ===")
    
    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Step 1: Extract block data from each category and convert to simple data structure
    categories = ['BUILDING_BLOCKS', 'LIGHTING_BLOCKS', 'DECORATION_BLOCKS', 
                  'NATURE_BLOCKS', 'FUNCTIONAL_BLOCKS', 'ORE_BLOCKS', 'SPECIAL_BLOCKS']
    
    for cat in categories:
        # Find the category definition
        pattern = rf"const {cat} = \[(.*?)\];"
        match = re.search(pattern, content, re.DOTALL)
        
        if not match:
            print(f"  ERROR: Could not find {cat}")
            continue
        
        cat_content = match.group(1)
        
        # Extract values only (the 'value' field from each block)
        value_pattern = r"value:\s*'([^']+)'"
        values = re.findall(value_pattern, cat_content)
        
        # Create new simple data structure
        new_data = f"const {cat}_DATA = {values};"
        
        # Replace the old definition with the new simple one
        old_def = f"const {cat} = [{cat_content}];"
        content = content.replace(old_def, new_data)
        
        print(f"  Converted {cat}: {len(values)} blocks")
    
    # Step 2: Update menu definitions to use method names
    menu_replacements = [
        ("items: BUILDING_BLOCKS", "items: '_getBuildingBlocks'"),
        ("items: LIGHTING_BLOCKS", "items: '_getLightingBlocks'"),
        ("items: DECORATION_BLOCKS", "items: '_getDecorationBlocks'"),
        ("items: NATURE_BLOCKS", "items: '_getNatureBlocks'"),
        ("items: FUNCTIONAL_BLOCKS", "items: '_getFunctionalBlocks'"),
        ("items: ORE_BLOCKS", "items: '_getOreBlocks'"),
        ("items: SPECIAL_BLOCKS", "items: '_getSpecialBlocks'"),
    ]
    
    for old, new in menu_replacements:
        if old in content:
            content = content.replace(old, new)
            print(f"  Updated menu: {old} -> {new}")
    
    # Step 3: Find where to add the menu methods (before the class closing brace)
    # Find the last method in Scratch3MinecraftBlocks class
    
    # Generate menu methods
    menu_methods = '''
  // Dynamic menu methods for i18n support
  _getBuildingBlocks() {
    return BUILDING_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
  _getLightingBlocks() {
    return LIGHTING_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
  _getDecorationBlocks() {
    return DECORATION_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
  _getNatureBlocks() {
    return NATURE_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
  _getFunctionalBlocks() {
    return FUNCTIONAL_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
  _getOreBlocks() {
    return ORE_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
  _getSpecialBlocks() {
    return SPECIAL_BLOCKS_DATA.map(value => ({
      text: formatMessage({ id: `minecraft.block.${value}`, default: value.replace(/_/g, ' ').split(' ').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' '), description: 'minecraft block name' }),
      value: value
    }));
  }
'''
    
    # Find the end of the Scratch3MinecraftBlocks class
    # Look for the pattern where the class ends
    class_end_pattern = r"(getBlockType\(args\) \{.*?return this\.sendCommandWithResponse\('getBlockType'.*?\}\);?\s*\}\s*\})"
    match = re.search(class_end_pattern, content, re.DOTALL)
    
    if match:
        # Insert methods before the final closing brace
        old_text = match.group(1)
        new_text = old_text.rstrip('}') + menu_methods + '\n}'
        content = content.replace(old_text, new_text)
        print("  Added dynamic menu methods to class")
    else:
        print("  ERROR: Could not find class end to insert methods")
    
    # Step 4: Update _getAllBlocks to use the new data structures
    # Find and update _getAllBlocks method
    old_getAllBlocks = "...BUILDING_BLOCKS,"
    new_getAllBlocks = "...this._getBuildingBlocks(),"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    old_getAllBlocks = "...LIGHTING_BLOCKS,"
    new_getAllBlocks = "...this._getLightingBlocks(),"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    old_getAllBlocks = "...DECORATION_BLOCKS,"
    new_getAllBlocks = "...this._getDecorationBlocks(),"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    old_getAllBlocks = "...NATURE_BLOCKS,"
    new_getAllBlocks = "...this._getNatureBlocks(),"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    old_getAllBlocks = "...FUNCTIONAL_BLOCKS,"
    new_getAllBlocks = "...this._getFunctionalBlocks(),"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    old_getAllBlocks = "...ORE_BLOCKS,"
    new_getAllBlocks = "...this._getOreBlocks(),"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    old_getAllBlocks = "...SPECIAL_BLOCKS]"
    new_getAllBlocks = "...this._getSpecialBlocks()]"
    content = content.replace(old_getAllBlocks, new_getAllBlocks)
    
    print("  Updated _getAllBlocks references")
    
    # Write the modified file
    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)
    
    print("\n=== Done ===")

if __name__ == '__main__':
    main()

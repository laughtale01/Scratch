#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Add Minecraft block translations for all Scratch-supported languages
Uses official Minecraft language files from InventivetalentDev/minecraft-assets
"""

import json
import urllib.request
import re
import time

# Mapping from Scratch language codes to Minecraft language codes
SCRATCH_TO_MINECRAFT = {
    'ab': None,  # Abkhazian - not in Minecraft
    'af': 'af_za',  # Afrikaans
    'ar': 'ar_sa',  # Arabic
    'am': None,  # Amharic - not in Minecraft
    'an': None,  # Aragonese - not in Minecraft
    'az': 'az_az',  # Azerbaijani
    'id': 'id_id',  # Indonesian
    'bn': 'bn_bd',  # Bengali (Bangladesh)
    'be': 'be_by',  # Belarusian
    'bg': 'bg_bg',  # Bulgarian
    'ca': 'ca_es',  # Catalan
    'cs': 'cs_cz',  # Czech
    'cy': 'cy_gb',  # Welsh
    'da': 'da_dk',  # Danish
    'de': 'de_de',  # German
    'et': 'et_ee',  # Estonian
    'el': 'el_gr',  # Greek
    'en': 'en_us',  # English (already done)
    'es': 'es_es',  # Spanish
    'eo': 'eo_uy',  # Esperanto
    'eu': 'eu_es',  # Basque
    'fa': 'fa_ir',  # Persian
    'fr': 'fr_fr',  # French
    'fy': None,  # Frisian - not in Minecraft
    'ga': 'ga_ie',  # Irish
    'gd': 'gd_gb',  # Scottish Gaelic
    'gl': 'gl_es',  # Galician
    'ko': 'ko_kr',  # Korean
    'ha': None,  # Hausa - not in Minecraft
    'hy': 'hy_am',  # Armenian
    'he': 'he_il',  # Hebrew
    'hi': 'hi_in',  # Hindi
    'hr': 'hr_hr',  # Croatian
    'xh': None,  # Xhosa - not in Minecraft
    'zu': None,  # Zulu - not in Minecraft
    'is': 'is_is',  # Icelandic
    'it': 'it_it',  # Italian
    'ka': 'ka_ge',  # Georgian
    'kk': 'kk_kz',  # Kazakh
    'qu': None,  # Quechua - not in Minecraft
    'sw': 'sw_ke',  # Swahili (Kenya)
    'ht': None,  # Haitian Creole - not in Minecraft
    'ku': None,  # Kurdish - not in Minecraft
    'lv': 'lv_lv',  # Latvian
    'lt': 'lt_lt',  # Lithuanian
    'hu': 'hu_hu',  # Hungarian
    'mi': 'mi_nz',  # Maori
    'mn': 'mn_mn',  # Mongolian
    'nl': 'nl_nl',  # Dutch
    'ja': 'ja_jp',  # Japanese (already done)
    'ja-Hira': 'ja_jp',  # Japanese Hiragana - use same as Japanese
    'nb': 'no_no',  # Norwegian Bokmal
    'nn': 'nn_no',  # Norwegian Nynorsk
    'oc': 'oc_fr',  # Occitan
    'or': None,  # Odia - not in Minecraft
    'uz': 'uz_uz',  # Uzbek
    'th': 'th_th',  # Thai
    'km': 'km_kh',  # Khmer
    'pl': 'pl_pl',  # Polish
    'pt': 'pt_pt',  # Portuguese
    'pt-br': 'pt_br',  # Brazilian Portuguese
    'ro': 'ro_ro',  # Romanian
    'ru': 'ru_ru',  # Russian
    'tn': None,  # Tswana - not in Minecraft
    'sk': 'sk_sk',  # Slovak
    'sl': 'sl_si',  # Slovenian
    'sr': 'sr_sp',  # Serbian
    'fi': 'fi_fi',  # Finnish
    'sv': 'sv_se',  # Swedish
    'vi': 'vi_vn',  # Vietnamese
    'tr': 'tr_tr',  # Turkish
    'uk': 'uk_ua',  # Ukrainian
    'zh-cn': 'zh_cn',  # Chinese Simplified
    'zh-tw': 'zh_tw',  # Chinese Traditional
}

# Block IDs we need translations for (from BUILDING_BLOCKS_DATA etc.)
BLOCK_IDS = [
    'stone', 'cobblestone', 'terracotta', 'oak_planks', 'spruce_planks', 'birch_planks',
    'jungle_planks', 'acacia_planks', 'dark_oak_planks', 'mangrove_planks', 'cherry_planks',
    'bamboo_planks', 'crimson_planks', 'warped_planks', 'stripped_oak_log', 'stripped_spruce_log',
    'stripped_birch_log', 'stripped_jungle_log', 'stripped_acacia_log', 'stripped_dark_oak_log',
    'stripped_mangrove_log', 'stripped_cherry_log', 'stripped_crimson_stem', 'stripped_warped_stem',
    'oak_wood', 'spruce_wood', 'birch_wood', 'jungle_wood', 'acacia_wood', 'dark_oak_wood',
    'mangrove_wood', 'cherry_wood', 'crimson_hyphae', 'warped_hyphae', 'stripped_oak_wood',
    'stripped_spruce_wood', 'stripped_birch_wood', 'stripped_jungle_wood', 'stripped_acacia_wood',
    'stripped_dark_oak_wood', 'stripped_mangrove_wood', 'stripped_cherry_wood',
    'stripped_crimson_hyphae', 'stripped_warped_hyphae', 'andesite', 'polished_andesite',
    'diorite', 'polished_diorite', 'granite', 'polished_granite', 'calcite', 'tuff',
    'chiseled_deepslate', 'cobbled_deepslate', 'cracked_deepslate_bricks', 'cracked_deepslate_tiles',
    'deepslate', 'deepslate_bricks', 'deepslate_tiles', 'polished_deepslate', 'basalt',
    'polished_basalt', 'blackstone', 'gilded_blackstone', 'polished_blackstone',
    'polished_blackstone_bricks', 'cracked_polished_blackstone_bricks', 'bricks',
    'end_stone_bricks', 'nether_bricks', 'red_nether_bricks', 'prismarine_bricks',
    'quartz_bricks', 'stone_bricks', 'mossy_stone_bricks', 'cracked_stone_bricks',
    'chiseled_stone_bricks', 'chiseled_nether_bricks', 'cracked_nether_bricks', 'mud_bricks',
    'white_concrete', 'orange_concrete', 'magenta_concrete', 'light_blue_concrete',
    'yellow_concrete', 'lime_concrete', 'pink_concrete', 'gray_concrete', 'light_gray_concrete',
    'cyan_concrete', 'purple_concrete', 'blue_concrete', 'brown_concrete', 'green_concrete',
    'red_concrete', 'black_concrete', 'dark_prismarine', 'chiseled_quartz_block', 'quartz_pillar',
    'smooth_quartz', 'purpur_block', 'purpur_pillar', 'bamboo_mosaic', 'sandstone',
    'chiseled_sandstone', 'cut_sandstone', 'smooth_sandstone', 'red_sandstone',
    'chiseled_red_sandstone', 'cut_red_sandstone', 'smooth_red_sandstone', 'smooth_stone',
    # Stairs
    'oak_stairs', 'spruce_stairs', 'birch_stairs', 'jungle_stairs', 'acacia_stairs',
    'dark_oak_stairs', 'mangrove_stairs', 'cherry_stairs', 'bamboo_mosaic_stairs',
    'bamboo_stairs', 'crimson_stairs', 'warped_stairs', 'blackstone_stairs', 'cobblestone_stairs',
    'end_stone_brick_stairs', 'polished_blackstone_brick_stairs', 'polished_blackstone_stairs',
    'red_sandstone_stairs', 'sandstone_stairs', 'smooth_red_sandstone_stairs',
    'smooth_sandstone_stairs', 'stone_brick_stairs', 'stone_stairs', 'andesite_stairs',
    'polished_andesite_stairs', 'diorite_stairs', 'polished_diorite_stairs', 'granite_stairs',
    'polished_granite_stairs', 'cobbled_deepslate_stairs', 'polished_deepslate_stairs',
    'deepslate_brick_stairs', 'deepslate_tile_stairs', 'brick_stairs', 'nether_brick_stairs',
    'prismarine_brick_stairs', 'red_nether_brick_stairs', 'dark_prismarine_stairs',
    'prismarine_stairs', 'quartz_stairs', 'purpur_stairs', 'cut_copper_stairs',
    'exposed_cut_copper_stairs', 'weathered_cut_copper_stairs', 'oxidized_cut_copper_stairs',
    'waxed_cut_copper_stairs', 'waxed_exposed_cut_copper_stairs', 'waxed_weathered_cut_copper_stairs',
    'waxed_oxidized_cut_copper_stairs',
    # Slabs
    'oak_slab', 'spruce_slab', 'birch_slab', 'jungle_slab', 'acacia_slab', 'dark_oak_slab',
    'mangrove_slab', 'cherry_slab', 'bamboo_mosaic_slab', 'bamboo_slab', 'crimson_slab',
    'warped_slab', 'blackstone_slab', 'cobblestone_slab', 'cut_red_sandstone_slab',
    'cut_sandstone_slab', 'end_stone_brick_slab', 'polished_blackstone_brick_slab',
    'polished_blackstone_slab', 'red_sandstone_slab', 'sandstone_slab', 'smooth_red_sandstone_slab',
    'smooth_sandstone_slab', 'smooth_stone_slab', 'stone_brick_slab', 'stone_slab',
    'andesite_slab', 'polished_andesite_slab', 'diorite_slab', 'polished_diorite_slab',
    'granite_slab', 'polished_granite_slab', 'cobbled_deepslate_slab', 'polished_deepslate_slab',
    'deepslate_brick_slab', 'deepslate_tile_slab', 'brick_slab', 'nether_brick_slab',
    'prismarine_brick_slab', 'dark_prismarine_slab', 'prismarine_slab', 'quartz_slab',
    'smooth_quartz_slab', 'purpur_slab', 'cut_copper_slab', 'exposed_cut_copper_slab',
    'weathered_cut_copper_slab', 'oxidized_cut_copper_slab', 'waxed_cut_copper_slab',
    'waxed_exposed_cut_copper_slab', 'waxed_weathered_cut_copper_slab', 'waxed_oxidized_cut_copper_slab',
    # Custom vertical slabs (not in vanilla Minecraft - use English)
    'waxed_vertical_copper_block_slab', 'waxed_vertical_cut_copper_slab',
    'waxed_vertical_exposed_cut_copper_slab', 'waxed_vertical_oxidized_cut_copper_slab',
    'waxed_vertical_weathered_cut_copper_slab', 'vertical_acacia_slab', 'vertical_amethyst_block_slab',
    'vertical_andesite_slab', 'vertical_birch_slab', 'vertical_brick_slab', 'vertical_cherry_slab',
    'vertical_coal_block_slab', 'vertical_cobblestone_slab', 'vertical_copper_block_slab',
    'vertical_crimson_slab', 'vertical_cut_copper_slab', 'vertical_dark_oak_slab',
    'vertical_diamond_block_slab', 'vertical_diorite_slab', 'vertical_emerald_block_slab',
    'vertical_exposed_cut_copper_slab', 'vertical_gold_block_slab', 'vertical_granite_slab',
    'vertical_iron_block_slab', 'vertical_jungle_slab', 'vertical_lapis_block_slab',
    'vertical_mangrove_slab', 'vertical_netherite_block_slab', 'vertical_oak_slab',
    'vertical_oxidized_cut_copper_slab', 'vertical_quartz_slab', 'vertical_redstone_block_slab',
    'vertical_sandstone_slab', 'vertical_smooth_stone_slab', 'vertical_spruce_slab',
    'vertical_stone_brick_slab', 'vertical_stone_slab', 'vertical_warped_slab',
    'vertical_weathered_cut_copper_slab',
    # Walls
    'cobblestone_wall', 'mossy_cobblestone_wall', 'andesite_wall', 'diorite_wall', 'granite_wall',
    'cobbled_deepslate_wall', 'polished_deepslate_wall', 'deepslate_brick_wall', 'deepslate_tile_wall',
    'blackstone_wall', 'polished_blackstone_brick_wall', 'polished_blackstone_wall', 'brick_wall',
    'end_stone_brick_wall', 'mud_brick_wall', 'nether_brick_wall', 'red_nether_brick_wall',
    'stone_brick_wall', 'red_sandstone_wall', 'sandstone_wall', 'prismarine_wall',
    # Fences and gates
    'oak_fence', 'spruce_fence', 'birch_fence', 'jungle_fence', 'acacia_fence', 'dark_oak_fence',
    'mangrove_fence', 'cherry_fence', 'bamboo_fence', 'crimson_fence', 'warped_fence',
    'nether_brick_fence', 'oak_fence_gate', 'spruce_fence_gate', 'birch_fence_gate',
    'jungle_fence_gate', 'acacia_fence_gate', 'dark_oak_fence_gate', 'mangrove_fence_gate',
    'cherry_fence_gate', 'bamboo_fence_gate', 'crimson_fence_gate', 'warped_fence_gate',
    # Buttons
    'spruce_button', 'birch_button', 'jungle_button', 'acacia_button', 'dark_oak_button',
    'mangrove_button', 'cherry_button', 'bamboo_button', 'crimson_button', 'warped_button',
    # Pressure plates
    'spruce_pressure_plate', 'birch_pressure_plate', 'jungle_pressure_plate', 'acacia_pressure_plate',
    'dark_oak_pressure_plate', 'mangrove_pressure_plate', 'cherry_pressure_plate',
    'bamboo_pressure_plate', 'crimson_pressure_plate', 'warped_pressure_plate',
    # Doors
    'spruce_door', 'birch_door', 'jungle_door', 'acacia_door', 'dark_oak_door', 'mangrove_door',
    'cherry_door', 'bamboo_door', 'crimson_door', 'warped_door',
    # Trapdoors
    'spruce_trapdoor', 'birch_trapdoor', 'jungle_trapdoor', 'acacia_trapdoor', 'dark_oak_trapdoor',
    'mangrove_trapdoor', 'cherry_trapdoor', 'bamboo_trapdoor', 'crimson_trapdoor', 'warped_trapdoor',
    # Misc building blocks
    'black_carpet', 'black_concrete_powder', 'black_glazed_terracotta', 'black_shulker_box',
    'black_stained_glass_pane', 'blue_carpet', 'blue_concrete_powder', 'blue_stained_glass_pane',
    'bone_block', 'brown_bed', 'brown_carpet', 'brown_concrete_powder', 'brown_glazed_terracotta',
    'brown_shulker_box', 'brown_stained_glass_pane', 'chain', 'cyan_bed', 'cyan_carpet',
    'cyan_concrete_powder', 'cyan_glazed_terracotta', 'cyan_shulker_box', 'cyan_stained_glass_pane',
    'dirt_path', 'glass_pane', 'gray_bed', 'gray_concrete_powder', 'gray_glazed_terracotta',
    'gray_shulker_box', 'gray_stained_glass_pane', 'green_carpet', 'green_concrete_powder',
    'green_stained_glass_pane', 'iron_bars', 'ladder', 'light_blue_bed', 'light_blue_concrete_powder',
    'light_blue_glazed_terracotta', 'light_blue_shulker_box', 'light_blue_stained_glass_pane',
    'light_gray_bed', 'light_gray_carpet', 'light_gray_concrete_powder', 'light_gray_glazed_terracotta',
    'light_gray_shulker_box', 'light_gray_stained_glass_pane', 'lime_bed', 'lime_concrete_powder',
    'lime_glazed_terracotta', 'lime_shulker_box', 'lime_stained_glass_pane', 'magenta_bed',
    'magenta_concrete_powder', 'magenta_glazed_terracotta', 'magenta_shulker_box',
    'magenta_stained_glass_pane', 'mangrove_roots', 'mossy_cobblestone', 'muddy_mangrove_roots',
    'orange_bed', 'orange_concrete_powder', 'orange_shulker_box', 'orange_stained_glass_pane',
    'packed_mud', 'pink_bed', 'pink_concrete_powder', 'pink_glazed_terracotta', 'pink_shulker_box',
    'pink_stained_glass_pane', 'powder_snow', 'purple_bed', 'purple_carpet', 'purple_concrete_powder',
    'purple_glazed_terracotta', 'purple_shulker_box', 'purple_stained_glass_pane', 'red_carpet',
    'red_concrete_powder', 'red_glazed_terracotta', 'red_stained_glass_pane', 'rooted_dirt',
    'stripped_bamboo_block', 'tinted_glass', 'white_concrete_powder', 'white_stained_glass_pane',
    'yellow_concrete_powder', 'yellow_glazed_terracotta', 'yellow_shulker_box', 'yellow_stained_glass_pane',
    # Lighting blocks
    'torch', 'soul_torch', 'lantern', 'soul_lantern', 'glowstone', 'sea_lantern', 'shroomlight',
    'jack_o_lantern', 'candle', 'white_candle', 'orange_candle', 'magenta_candle', 'light_blue_candle',
    'yellow_candle', 'lime_candle', 'pink_candle', 'gray_candle', 'light_gray_candle', 'cyan_candle',
    'purple_candle', 'blue_candle', 'brown_candle', 'green_candle', 'red_candle', 'black_candle',
    'campfire', 'soul_campfire', 'redstone_lamp', 'redstone_torch', 'soul_fire',
    # Decoration blocks
    'white_wool', 'orange_wool', 'magenta_wool', 'light_blue_wool', 'yellow_wool', 'lime_wool',
    'pink_wool', 'gray_wool', 'light_gray_wool', 'cyan_wool', 'purple_wool', 'blue_wool',
    'brown_wool', 'green_wool', 'red_wool', 'black_wool', 'moss_carpet', 'white_carpet',
    'orange_carpet', 'magenta_carpet', 'light_blue_carpet', 'yellow_carpet', 'lime_carpet',
    'pink_carpet', 'gray_carpet', 'white_terracotta', 'orange_terracotta', 'magenta_terracotta',
    'light_blue_terracotta', 'yellow_terracotta', 'lime_terracotta', 'pink_terracotta',
    'gray_terracotta', 'light_gray_terracotta', 'cyan_terracotta', 'purple_terracotta',
    'blue_terracotta', 'brown_terracotta', 'green_terracotta', 'red_terracotta', 'black_terracotta',
    'white_glazed_terracotta', 'orange_glazed_terracotta', 'blue_glazed_terracotta',
    'green_glazed_terracotta', 'glass', 'white_stained_glass', 'orange_stained_glass',
    'magenta_stained_glass', 'light_blue_stained_glass', 'yellow_stained_glass', 'lime_stained_glass',
    'pink_stained_glass', 'gray_stained_glass', 'light_gray_stained_glass', 'cyan_stained_glass',
    'purple_stained_glass', 'blue_stained_glass', 'brown_stained_glass', 'green_stained_glass',
    'red_stained_glass', 'black_stained_glass', 'white_bed', 'yellow_bed', 'blue_bed', 'green_bed',
    'red_bed', 'black_bed', 'shulker_box', 'white_shulker_box', 'blue_shulker_box', 'green_shulker_box',
    'red_shulker_box', 'bookshelf', 'budding_amethyst', 'dripstone_block', 'flower_pot', 'item_frame',
    'moss_block', 'painting', 'smooth_basalt', 'oak_sign', 'spruce_sign', 'birch_sign', 'jungle_sign',
    'acacia_sign', 'dark_oak_sign', 'mangrove_sign', 'cherry_sign', 'bamboo_sign', 'crimson_sign',
    'warped_sign', 'oak_hanging_sign', 'spruce_hanging_sign', 'birch_hanging_sign',
    'jungle_hanging_sign', 'acacia_hanging_sign', 'dark_oak_hanging_sign', 'mangrove_hanging_sign',
    'cherry_hanging_sign', 'bamboo_hanging_sign', 'crimson_hanging_sign', 'warped_hanging_sign',
    # Nature blocks
    'dirt', 'grass_block', 'coarse_dirt', 'podzol', 'mycelium', 'farmland', 'mud', 'sand', 'red_sand',
    'gravel', 'soul_sand', 'soul_soil', 'oak_log', 'spruce_log', 'birch_log', 'jungle_log', 'acacia_log',
    'dark_oak_log', 'mangrove_log', 'cherry_log', 'crimson_stem', 'warped_stem', 'mushroom_stem',
    'oak_leaves', 'spruce_leaves', 'birch_leaves', 'jungle_leaves', 'acacia_leaves', 'dark_oak_leaves',
    'mangrove_leaves', 'cherry_leaves', 'azalea_leaves', 'flowering_azalea_leaves', 'poppy', 'dandelion',
    'blue_orchid', 'allium', 'oxeye_daisy', 'cornflower', 'lily_of_the_valley', 'lilac', 'rose_bush',
    'peony', 'sunflower', 'ice', 'packed_ice', 'snow', 'snow_block', 'water', 'bamboo', 'bamboo_block',
    'cactus', 'sugar_cane', 'kelp', 'seagrass', 'grass', 'fern', 'dead_bush', 'vine', 'twisting_vines',
    'weeping_vines', 'glow_berries', 'azalea', 'brain_coral_block', 'brown_mushroom_block',
    'bubble_coral_block', 'clay', 'fire_coral_block', 'flowering_azalea', 'hay_block', 'horn_coral_block',
    'melon', 'nether_wart_block', 'prismarine', 'pumpkin', 'red_mushroom_block', 'tube_coral_block',
    'warped_wart_block',
    # Functional blocks
    'crafting_table', 'furnace', 'blast_furnace', 'smoker', 'anvil', 'grindstone', 'stonecutter',
    'smithing_table', 'cartography_table', 'fletching_table', 'loom', 'brewing_stand', 'enchanting_table',
    'chest', 'barrel', 'hopper', 'iron_door', 'oak_door', 'iron_trapdoor', 'oak_trapdoor', 'comparator',
    'repeater', 'observer', 'lever', 'target', 'dispenser', 'dropper', 'piston', 'sticky_piston', 'rail',
    'powered_rail', 'detector_rail', 'activator_rail', 'oak_button', 'stone_button', 'oak_pressure_plate',
    'stone_pressure_plate', 'beacon', 'bell', 'cauldron', 'composter', 'conduit', 'furnace_minecart',
    'heavy_weighted_pressure_plate', 'honey_block', 'jukebox', 'lectern', 'light_weighted_pressure_plate',
    'note_block', 'respawn_anchor', 'slime_block', 'tnt',
    # Ore blocks
    'coal_ore', 'iron_ore', 'copper_ore', 'gold_ore', 'lapis_ore', 'redstone_ore', 'diamond_ore',
    'emerald_ore', 'deepslate_coal_ore', 'deepslate_iron_ore', 'deepslate_copper_ore',
    'deepslate_gold_ore', 'deepslate_lapis_ore', 'deepslate_redstone_ore', 'deepslate_diamond_ore',
    'deepslate_emerald_ore', 'nether_gold_ore', 'nether_quartz_ore', 'coal_block', 'iron_block',
    'gold_block', 'lapis_block', 'redstone_block', 'diamond_block', 'emerald_block', 'quartz_block',
    'amethyst_block', 'netherite_block', 'copper_block', 'cut_copper', 'exposed_copper',
    'weathered_copper', 'oxidized_copper', 'waxed_copper_block', 'waxed_cut_copper', 'waxed_exposed_copper',
    'waxed_weathered_copper', 'waxed_oxidized_copper', 'raw_iron_block', 'raw_copper_block',
    'raw_gold_block', 'ancient_debris',
    # Special blocks
    'air', 'barrier', 'structure_void', 'light', 'bedrock', 'obsidian', 'crying_obsidian', 'sponge',
    'wet_sponge', 'lava', 'end_portal', 'end_portal_frame', 'end_gateway', 'end_rod', 'end_stone',
    'netherrack', 'magma_block', 'sculk', 'sculk_vein', 'sculk_sensor', 'sculk_catalyst', 'sculk_shrieker',
    'blue_ice', 'command_block', 'debug_stick', 'dragon_egg', 'jigsaw', 'moving_piston',
    'reinforced_deepslate', 'spawner', 'structure_block',
]

# Block command translations (32 commands)
COMMAND_TRANSLATIONS = {
    'minecraft.connect': 'Connect to Minecraft host [HOST] port [PORT]',
    'minecraft.disconnect': 'Disconnect',
    'minecraft.place': 'Place block [BLOCK] at [X] [Y] [Z]',
    'minecraft.placeBuilding': 'Place [BLOCK] building block at [X] [Y] [Z]',
    'minecraft.placeLighting': 'Place [BLOCK] lighting block at [X] [Y] [Z]',
    'minecraft.placeDecoration': 'Place [BLOCK] decoration block at [X] [Y] [Z]',
    'minecraft.placeNature': 'Place [BLOCK] nature block at [X] [Y] [Z]',
    'minecraft.placeFunctional': 'Place [BLOCK] functional block at [X] [Y] [Z]',
    'minecraft.placeOre': 'Place [BLOCK] ore block at [X] [Y] [Z]',
    'minecraft.placeSpecial': 'Place [BLOCK] special block at [X] [Y] [Z]',
    'minecraft.fill': 'Fill from [X1] [Y1] [Z1] to [X2] [Y2] [Z2] with [BLOCK]',
    'minecraft.clone': 'Clone from [X1] [Y1] [Z1] to [X2] [Y2] [Z2] to [X3] [Y3] [Z3]',
    'minecraft.destroyBlock': 'Destroy block at [X] [Y] [Z]',
    'minecraft.teleport': 'Teleport to [X] [Y] [Z]',
    'minecraft.summon': 'Summon [ENTITY] at [X] [Y] [Z]',
    'minecraft.entityPassive': 'Summon passive entity [ENTITY]',
    'minecraft.entityNeutral': 'Summon neutral entity [ENTITY]',
    'minecraft.entityHostile': 'Summon hostile entity [ENTITY]',
    'minecraft.entityBoss': 'Summon boss entity [ENTITY]',
    'minecraft.entityAquatic': 'Summon aquatic entity [ENTITY]',
    'minecraft.entityVillager': 'Summon villager entity [ENTITY]',
    'minecraft.entityOther': 'Summon other entity [ENTITY]',
    'minecraft.setWeather': 'Set weather to [WEATHER]',
    'minecraft.setTime': 'Set time to [TIME]',
    'minecraft.getPlayerFacing': 'Player facing direction',
    'minecraft.getBlockType': 'Block type at [X] [Y] [Z]',
    'minecraft.isConnected': 'Connected?',
}


def download_minecraft_lang(lang_code):
    """Download Minecraft language file"""
    url = f"https://raw.githubusercontent.com/InventivetalentDev/minecraft-assets/1.21/assets/minecraft/lang/{lang_code}.json"
    try:
        with urllib.request.urlopen(url, timeout=30) as response:
            return json.loads(response.read().decode('utf-8'))
    except Exception as e:
        print(f"  Warning: Could not download {lang_code}: {e}")
        return None


def get_block_translation(mc_lang_data, block_id):
    """Get block translation from Minecraft language data"""
    if mc_lang_data is None:
        return None

    # Try different key formats
    key = f"block.minecraft.{block_id}"
    if key in mc_lang_data:
        return mc_lang_data[key]

    return None


def title_case(s):
    """Convert underscore-separated string to Title Case"""
    return ' '.join(word.capitalize() for word in s.replace('_', ' ').split())


def main():
    print("=== Adding Minecraft translations for all Scratch languages ===")

    # Read gui.js
    with open('gui.js', 'r', encoding='utf-8') as f:
        content = f.read()

    # Languages to skip (already done or will use fallback)
    skip_langs = {'en', 'ja'}  # Already have translations

    # Process each Scratch language
    for scratch_lang, mc_lang in SCRATCH_TO_MINECRAFT.items():
        if scratch_lang in skip_langs:
            print(f"  Skipping {scratch_lang} (already done)")
            continue

        if mc_lang is None:
            print(f"  Skipping {scratch_lang} (no Minecraft translation available)")
            continue

        print(f"  Processing {scratch_lang} ({mc_lang})...")

        # Download Minecraft language file
        mc_data = download_minecraft_lang(mc_lang)

        if mc_data is None:
            print(f"    Using English fallback for {scratch_lang}")
            continue

        # Build translations for this language
        translations = {}

        # Add block translations
        for block_id in BLOCK_IDS:
            trans = get_block_translation(mc_data, block_id)
            if trans:
                translations[f"minecraft.block.{block_id}"] = trans
            else:
                # Use English fallback (title case)
                translations[f"minecraft.block.{block_id}"] = title_case(block_id)

        # Find the language section in gui.js and add translations
        # Look for pattern: "lang_code": {
        lang_pattern = rf'  "{re.escape(scratch_lang)}": \{{'
        match = re.search(lang_pattern, content)

        if match:
            insert_pos = match.end()

            # Build the translation string
            trans_lines = []
            for key, value in translations.items():
                # Escape quotes in value
                escaped_value = value.replace('\\', '\\\\').replace('"', '\\"')
                trans_lines.append(f'    "{key}": "{escaped_value}"')

            trans_string = ',\n'.join(trans_lines) + ',\n'

            # Insert after the opening brace
            content = content[:insert_pos] + '\n' + trans_string + content[insert_pos:]

            print(f"    Added {len(translations)} translations for {scratch_lang}")
        else:
            print(f"    Warning: Could not find section for {scratch_lang}")

        # Rate limiting to avoid hitting GitHub too fast
        time.sleep(0.5)

    # Write back to gui.js
    with open('gui.js', 'w', encoding='utf-8') as f:
        f.write(content)

    print("\n=== Done ===")


if __name__ == '__main__':
    main()

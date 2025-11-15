# Minecraft Scratch Blocks Reference

This document provides a comprehensive reference for all available Scratch blocks in the MinecraftEdu Scratch Controller extension.

## 📋 Table of Contents

- [Connection Blocks](#connection-blocks)
- [Communication Blocks](#communication-blocks)
- [Player Blocks](#player-blocks)
- [Block Placement](#block-placement)
- [Entity Blocks](#entity-blocks)
- [World Control](#world-control)
- [Utility Blocks](#utility-blocks)
- [Available Block Types](#available-block-types)
- [Block Properties](#block-properties)
- [Coordinate System](#coordinate-system)

---

## 🔌 Connection Blocks

### Connect to Minecraft
**Block**: `Minecraftに接続 ホスト [HOST] ポート [PORT]`
**Type**: Command
**Default Values**: `HOST: 'localhost'`, `PORT: 14711`

Establishes WebSocket connection to Minecraft server.

**Example Usage**:
```
When green flag clicked
└─ Connect to Minecraft [localhost] [14711]
```

**Parameters**:
- `HOST` (string): Server hostname or IP address
- `PORT` (number): WebSocket port (default: 14711)

**Notes**:
- Must be called before using any other Minecraft blocks
- Connection status can be checked with "接続中？" block
- Automatically reconnects on connection loss

---

### Disconnect
**Block**: `切断`
**Type**: Command

Disconnects from Minecraft server and closes WebSocket connection.

**Example Usage**:
```
When [space] key pressed
└─ Disconnect
```

---

## 💬 Communication Blocks

### Send Chat Message
**Block**: `チャットで言う [MESSAGE]`
**Type**: Command
**Default Value**: `MESSAGE: 'Hello, Minecraft!'`

Sends a message to Minecraft chat that appears to all players.

**Example Usage**:
```
When green flag clicked
├─ Connect to Minecraft [localhost] [14711]
└─ Send chat message [プログラムが開始されました！]
```

**Educational Use Cases**:
- Announce game events
- Provide instructions to players
- Debug program flow
- Create interactive storytelling

---

## 🎮 Player Blocks

### Teleport Player
**Block**: `テレポート x:[X] y:[Y] z:[Z]`
**Type**: Command
**Default Values**: `X: 0`, `Y: 64`, `Z: 0`

Teleports the player to specified coordinates.

**Example Usage**:
```
When [t] key pressed
└─ Teleport x:[100] y:[80] z:[200]
```

**Parameters**:
- `X` (number): X coordinate (-30,000,000 to 30,000,000)
- `Y` (number): Y coordinate in Scratch system (-4 to 379)
- `Z` (number): Z coordinate (-30,000,000 to 30,000,000)

**Notes**:
- Y coordinates use Scratch system (0 = sea level, 64 = ground level)
- Automatically converts to Minecraft Y coordinates internally
- Values outside valid range are clamped to nearest valid value

---

### Get Player Position
**Block**: `プレイヤーの位置 [COORD]`
**Type**: Reporter (returns number)
**Menu Options**: `x`, `y`, `z`

Returns the player's current coordinate for the specified axis.

**Example Usage**:
```
Set [current_x] to (Get player position [x])
Set [current_y] to (Get player position [y])
Set [current_z] to (Get player position [z])

Say (join [位置: ] (Get player position [x]))
```

**Educational Use Cases**:
- Track player movement
- Create position-based triggers
- Build navigation systems
- Calculate distances

---

## 🧱 Block Placement

### Place Block (Absolute Position)
**Block**: `ブロックを置く x:[X] y:[Y] z:[Z] ブロック:[BLOCK] 配置:[PLACEMENT] 向き:[FACING]`
**Type**: Command
**Default Values**: `X: 0`, `Y: 64`, `Z: 0`, `BLOCK: 'stone'`, `PLACEMENT: 'bottom'`, `FACING: 'none'`

Places a single block at absolute coordinates.

**Example Usage**:
```
Repeat [10]
├─ Place block x:[0] y:(64 + counter) z:[0] [stone] [bottom] [none]
└─ Wait [0.1] seconds
```

**Parameters**:
- `X, Y, Z`: Absolute world coordinates
- `BLOCK`: Block type from menu (400+ options)
- `PLACEMENT`: Slab/stair placement (bottom/top/double)
- `FACING`: Block direction (north/south/east/west/none)

---

### Place Block (Relative Position)
**Block**: `ブロックを置く ~[X] ~[Y] ~[Z] ブロック:[BLOCK] 配置:[PLACEMENT] 向き:[FACING]`
**Type**: Command
**Default Values**: `X: 0`, `Y: 1`, `Z: 2`, `BLOCK: 'stone'`, `PLACEMENT: 'bottom'`, `FACING: 'none'`

Places a block relative to player's current position.

**Example Usage**:
```
When [b] key pressed
├─ Place block ~[0] ~[0] ~[1] [diamond_block] [bottom] [none]
```

**Parameters**:
- `~X, ~Y, ~Z`: Offset from player position
- Other parameters same as absolute placement

**Educational Use Cases**:
- Build structures around player
- Create platforms beneath player
- Make interactive building tools

---

### Fill Area with Blocks
**Block**: `X:[X1] Y:[Y1] Z:[Z1] からX:[X2] Y:[Y2] Z:[Z2] ブロック:[BLOCK] 配置:[PLACEMENT] 向き:[FACING]`
**Type**: Command
**Default Values**: `X1: 0`, `Y1: 0`, `Z1: 0`, `X2: 5`, `Y2: 5`, `Z2: 5`

Fills a rectangular area with blocks.

**Example Usage**:
```
// Build a 10x10x10 glass cube
Fill area X:[0] Y:[64] Z:[0] to X:[10] Y:[74] Z:[10] [glass] [bottom] [none]
```

**Parameters**:
- `X1, Y1, Z1`: Starting corner coordinates
- `X2, Y2, Z2`: Ending corner coordinates
- `BLOCK`: Block type to fill
- Maximum volume: 2,000,000 blocks

**Safety Limits**:
- Validates volume before placement
- Prevents server overload
- Shows friendly error messages for children

**Educational Use Cases**:
- Build walls and floors quickly
- Create 3D geometric shapes
- Teach volume calculations
- Make game arenas

---

## 🐾 Entity Blocks

### Summon Entity
**Block**: `エンティティを召喚 [ENTITY] x:[X] y:[Y] z:[Z]`
**Type**: Command
**Default Values**: `ENTITY: 'pig'`, `X: 0`, `Y: 64`, `Z: 0`

Spawns an entity (mob/animal) at specified coordinates.

**Example Usage**:
```
Repeat [5]
├─ Summon entity [sheep] x:(random 0 to 100) y:[70] z:(random 0 to 100)
└─ Wait [0.5] seconds
```

**Available Entities**:
- **Animals**: ブタ (pig), ウシ (cow), ヒツジ (sheep), ニワトリ (chicken), ウマ (horse)
- **Pets**: オオカミ (wolf), ネコ (cat)
- **Villagers**: 村人 (villager)
- **Hostile Mobs**: ゾンビ (zombie), スケルトン (skeleton), クリーパー (creeper), クモ (spider)

**Educational Use Cases**:
- Create farms and zoos
- Spawn mobs for challenges
- Teach ecosystem concepts
- Practice loops and randomization

---

## 🌍 World Control

### Set Weather
**Block**: `天気を [WEATHER] にする`
**Type**: Command
**Menu Options**: 晴れ (clear), 雨 (rain), 雷雨 (thunder)

Changes the world weather.

**Example Usage**:
```
When [w] key pressed
└─ Set weather to [雨]

// Cycle through weather
When green flag clicked
├─ Forever
│   ├─ Set weather to [晴れ]
│   ├─ Wait [60] seconds
│   ├─ Set weather to [雨]
│   ├─ Wait [30] seconds
│   ├─ Set weather to [雷雨]
│   └─ Wait [20] seconds
```

---

### Set Time
**Block**: `時刻を [TIME] にする`
**Type**: Command
**Menu Options**: 朝 (day), 昼 (noon), 夕方 (sunset), 夜 (night), 真夜中 (midnight)

Changes the world time of day.

**Example Usage**:
```
When [1] key pressed
└─ Set time to [朝]

When [2] key pressed
└─ Set time to [夜]
```

**Educational Use Cases**:
- Create day/night cycle programs
- Control game difficulty (mobs spawn at night)
- Teach time concepts
- Set mood for building

---

### Clear Area
**Block**: `周囲をクリア X:[X] Z:[Z]`
**Type**: Command
**Default Values**: `X: 0`, `Z: 0`

Clears all blocks in a ±50 block radius around specified center coordinates.

**Example Usage**:
```
When [c] key pressed
├─ Get player position [x]
├─ Get player position [z]
└─ Clear area X:(answer) Z:(answer)
```

**Parameters**:
- `X, Z`: Center coordinates
- Range: X: -50 to +50, Z: -50 to +50 from center
- Maximum blocks affected: 500,000+

**Safety Features**:
- Shows warning message before clearing
- Child-friendly error messages

---

### Clear All Entities
**Block**: `全エンティティをクリア X:[X] Z:[Z]`
**Type**: Command
**Default Values**: `X: 0`, `Z: 0`

Removes all entities (mobs/animals) in a ±50 block radius around specified coordinates.

**Example Usage**:
```
When [e] key pressed
└─ Clear all entities X:[0] Z:[0]
```

**Parameters**:
- `X, Z`: Center coordinates
- Range: X: -50 to +50, Z: -50 to +50 from center

**Educational Use Cases**:
- Clean up after experiments
- Reset game areas
- Remove excess mobs
- Practice cleanup/reset logic

---

## 🔧 Utility Blocks

### Is Connected?
**Block**: `接続中？`
**Type**: Boolean Reporter (returns true/false)

Checks if currently connected to Minecraft server.

**Example Usage**:
```
If <Is connected?> then
├─ Send chat message [接続しています]
Else
├─ Say [未接続です]
```

**Educational Use Cases**:
- Validate connection before commands
- Create auto-reconnect logic
- Display connection status
- Handle errors gracefully

---

## 🧱 Available Block Types

The extension includes **400+ block types** across the following categories:

### Basic Blocks
`stone`, `dirt`, `grass_block`, `cobblestone`, `bedrock`, `sand`, `red_sand`, `gravel`, `clay`, `terracotta`

### Wood Types (12 varieties)
Oak, Spruce, Birch, Jungle, Acacia, Dark Oak, Mangrove, Cherry, Bamboo, Crimson, Warped

**Variants per wood type**:
- Planks (e.g., `oak_planks`)
- Logs (e.g., `oak_log`, `stripped_oak_log`)
- Wood blocks (e.g., `oak_wood`, `stripped_oak_wood`)
- Slabs (e.g., `oak_slab`)
- Stairs (e.g., `oak_stairs`)
- Fences (e.g., `oak_fence`)
- Fence Gates (e.g., `oak_fence_gate`)
- Doors (e.g., `oak_door`)
- Trapdoors (e.g., `oak_trapdoor`)
- Buttons (e.g., `oak_button`)
- Pressure Plates (e.g., `oak_pressure_plate`)

### Stone Types
`stone`, `smooth_stone`, `stone_bricks`, `cobblestone`, `mossy_cobblestone`, `granite`, `polished_granite`, `diorite`, `polished_diorite`, `andesite`, `polished_andesite`, `calcite`, `tuff`, `deepslate` (+ variants)

### Colored Blocks (16 colors each)
- **Wool**: `white_wool`, `orange_wool`, `magenta_wool`, etc.
- **Carpet**: `white_carpet`, `orange_carpet`, etc.
- **Concrete**: `white_concrete`, `orange_concrete`, etc.
- **Concrete Powder**: `white_concrete_powder`, etc.
- **Terracotta**: `white_terracotta`, `orange_terracotta`, etc.
- **Glazed Terracotta**: `white_glazed_terracotta`, etc.
- **Stained Glass**: `white_stained_glass`, etc.
- **Stained Glass Pane**: `white_stained_glass_pane`, etc.
- **Shulker Boxes**: `white_shulker_box`, etc.
- **Beds**: `white_bed`, `orange_bed`, etc.
- **Candles**: `white_candle`, `orange_candle`, etc.

**16 Colors**: white, orange, magenta, light_blue, yellow, lime, pink, gray, light_gray, cyan, purple, blue, brown, green, red, black

### Ores & Mineral Blocks
`coal_ore`, `iron_ore`, `gold_ore`, `diamond_ore`, `emerald_ore`, `lapis_ore`, `redstone_ore`, `copper_ore`

**Storage Blocks**: `gold_block`, `iron_block`, `copper_block`, `diamond_block`, `emerald_block`, `coal_block`, `redstone_block`, `lapis_block`, `netherite_block`

### Glass Types
`glass`, `tinted_glass` + 16 stained glass colors + glass panes

### Natural Blocks
`podzol`, `mycelium`, `dirt_path`, `farmland`, `ice`, `packed_ice`, `blue_ice`, `snow_block`, `moss_block`

### Nether Blocks
`netherrack`, `soul_sand`, `soul_soil`, `basalt`, `smooth_basalt`, `polished_basalt`, `blackstone` (+ variants), `nether_bricks`, `red_nether_bricks`, `glowstone`, `shroomlight`, `magma_block`

### End Blocks
`end_stone`, `end_stone_bricks`, `purpur_block`, `purpur_pillar`

### Functional Blocks
`crafting_table`, `furnace`, `chest`, `barrel`, `brewing_stand`, `anvil`, `grindstone`, `stonecutter`, `lectern`, `composter`, `cauldron`

### Decorative Blocks
`ladder`, `chain`, `lantern`, `soul_lantern`, `iron_bars`, `bell`, `bookshelf`, `bone_block`

### Special Blocks
`obsidian`, `crying_obsidian`, `tnt`, `sponge`, `wet_sponge`, `slime_block`, `honey_block`, `water`, `lava`

---

## 🎯 Block Properties

### Block Placement
Controls vertical orientation for slabs and stairs.

**Options**:
- `通常（下）` (bottom): Normal placement at bottom half
- `上下反転（上）` (top): Upside-down placement at top half
- `ダブル` (double): Full block (double slab)

**Example**:
```
// Place slab at bottom
Place block x:[0] y:[64] z:[0] [stone_slab] [bottom] [none]

// Place upside-down slab
Place block x:[0] y:[64] z:[0] [stone_slab] [top] [none]

// Place double slab (full block)
Place block x:[0] y:[64] z:[0] [stone_slab] [double] [none]
```

---

### Block Facing
Controls horizontal direction for directional blocks (stairs, doors, etc.).

**Options**:
- `デフォルト` (none): No specific direction
- `北` (north): Facing north
- `南` (south): Facing south
- `東` (east): Facing east
- `西` (west): Facing west

**Example**:
```
// Place stairs facing north
Place block x:[0] y:[64] z:[0] [oak_stairs] [bottom] [north]

// Place door facing east
Place block x:[0] y:[64] z:[0] [oak_door] [bottom] [east]
```

---

## 📐 Coordinate System

### Understanding Minecraft Coordinates

**X Axis**: East (+) / West (-)
- Range: -30,000,000 to 30,000,000

**Y Axis**: Up (+) / Down (-)
- Scratch coordinates: -4 to 379
- Minecraft coordinates: -64 to 319
- **Automatic conversion**: The extension handles conversion internally

**Z Axis**: South (+) / North (-)
- Range: -30,000,000 to 30,000,000

### Coordinate Reference Points

**Scratch Y Coordinates** (used in blocks):
- `Y = -4`: Bedrock level (Minecraft Y = -64)
- `Y = 0`: Sea level (Minecraft Y = -60)
- `Y = 64`: Standard ground level (Minecraft Y = 4)
- `Y = 100`: 36 blocks above ground (Minecraft Y = 40)
- `Y = 379`: Build limit (Minecraft Y = 319)

**Why Different?**
- Scratch-friendly coordinate system for children
- Y=0 represents sea level (intuitive reference point)
- Y=64 is standard "ground" in most worlds
- Negative values are minimized for simplicity

### Relative Coordinates

When using `~` prefix (relative placement):
- `~0 ~0 ~0`: At player position
- `~0 ~1 ~0`: 1 block above player
- `~1 ~0 ~0`: 1 block east of player
- `~0 ~0 ~-1`: 1 block north of player

**Example**:
```
// Build 3 blocks in front of player
Place block ~[0] ~[0] ~[1] [stone] [bottom] [none]
Place block ~[0] ~[0] ~[2] [stone] [bottom] [none]
Place block ~[0] ~[0] ~[3] [stone] [bottom] [none]
```

---

## 🎓 Educational Examples

### Example 1: Build a Simple House
```
When green flag clicked
├─ Connect to Minecraft [localhost] [14711]
├─ Wait until <Is connected?>
├─ Send chat message [家を建てます！]
│
├─ // Floor (10x10)
├─ Fill area X:[0] Y:[64] Z:[0] to X:[10] Y:[64] Z:[10] [oak_planks] [bottom] [none]
│
├─ // Walls (stone bricks)
├─ Fill area X:[0] Y:[65] Z:[0] to X:[10] Y:[69] Z:[0] [stone_bricks] [bottom] [none]
├─ Fill area X:[0] Y:[65] Z:[10] to X:[10] Y:[69] Z:[10] [stone_bricks] [bottom] [none]
├─ Fill area X:[0] Y:[65] Z:[0] to X:[0] Y:[69] Z:[10] [stone_bricks] [bottom] [none]
├─ Fill area X:[10] Y:[65] Z:[0] to X:[10] Y:[69] Z:[10] [stone_bricks] [bottom] [none]
│
├─ // Roof (slabs)
├─ Fill area X:[0] Y:[70] Z:[0] to X:[10] Y:[70] Z:[10] [oak_slab] [top] [none]
│
└─ Send chat message [完成！]
```

### Example 2: Create a Rainbow
```
When green flag clicked
├─ Connect to Minecraft [localhost] [14711]
├─ Set [colors] to [red orange yellow green blue purple]
├─ Set [y] to [70]
│
├─ For each [color] in [colors]
│   ├─ Fill area X:[0] Y:(y) Z:[0] to X:[50] Y:(y) Z:[1] [(color)_wool] [bottom] [none]
│   └─ Change [y] by [1]
│
└─ Send chat message [虹が完成！]
```

### Example 3: Spawn Animals in a Grid
```
When green flag clicked
├─ Connect to Minecraft [localhost] [14711]
├─ Set [animals] to [pig cow sheep chicken]
│
├─ Repeat [4]
│   ├─ Set [x] to (counter * 5)
│   ├─ Repeat [4]
│   │   ├─ Set [z] to (counter * 5)
│   │   ├─ Set [animal] to (item (random 1 to 4) of [animals])
│   │   └─ Summon entity (animal) x:(x) y:[70] z:(z)
│
└─ Send chat message [動物園が完成！]
```

### Example 4: Position-Based Trigger
```
When green flag clicked
├─ Connect to Minecraft [localhost] [14711]
├─ Forever
│   ├─ If <(Get player position [x]) > [100]> then
│   │   ├─ Send chat message [東エリアに入りました！]
│   │   ├─ Set time to [夜]
│   │   └─ Wait [5] seconds
│   │
│   ├─ If <(Get player position [x]) < [-100]> then
│   │   ├─ Send chat message [西エリアに入りました！]
│   │   ├─ Set time to [朝]
│   │   └─ Wait [5] seconds
```

---

## ⚠️ Important Notes

### Safety and Limits

1. **Volume Limit**: Range fill operations are limited to 2,000,000 blocks
2. **Coordinate Clamping**: Invalid coordinates are automatically adjusted to valid range
3. **Child-Friendly Errors**: Error messages are encouraging and instructive
4. **Connection Required**: All commands (except connection blocks) require active connection

### Best Practices

1. **Always check connection**: Use `Is connected?` block before commands
2. **Use relative coordinates**: For building around player
3. **Add delays**: Between large operations to prevent server lag
4. **Clear areas**: Use clear functions to reset before new creations
5. **Test small first**: Start with small ranges before large fill operations

### Performance Tips

1. **Batch operations**: Use range fill instead of many individual blocks
2. **Limit entity spawns**: Too many entities can cause lag
3. **Use chat messages**: To track program progress
4. **Clear unused entities**: Periodically clean up test mobs

---

## 📚 Learning Progression

### Beginner (Ages 7-9)
- Connect to Minecraft
- Send chat messages
- Teleport player
- Place single blocks
- Change weather and time

### Intermediate (Ages 10-12)
- Use relative coordinates
- Build with loops
- Create patterns with fill operations
- Spawn entities in patterns
- Use position reporters

### Advanced (Ages 13+)
- Complex 3D structures
- Position-based triggers
- Game mechanics (e.g., teleport pads)
- Procedural generation
- Interactive mini-games

---

## 🐛 Troubleshooting

### Connection Issues
- Verify Minecraft mod is installed
- Check WebSocket port (default: 14711)
- Ensure firewall allows connections
- Restart Minecraft if needed

### Blocks Not Appearing
- Check Y coordinate is valid
- Verify connection with `Is connected?`
- Ensure coordinates are within world bounds
- Check block type spelling

### Performance Problems
- Reduce fill operation volume
- Add delays between commands
- Clear unused entities
- Limit simultaneous operations

---

## 📞 Getting Help

- **Issues**: https://github.com/laughtale01/Scratch/issues
- **Documentation**: See README.md and CONTRIBUTING.md
- **Community**: GitHub Discussions

---

**Happy Building! 🎮✨**

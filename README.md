# Scratch 3.0 + Minecraft Extension

A visual programming environment based on Scratch 3.0 that connects with Minecraft Java Edition.
From children to adults, anyone can control the Minecraft world through programming by simply combining blocks.

## Demo / Access

**https://laughtale01.github.io/Scratch**

Access directly from your browser. No account registration or installation required.

---

## Features

### Full Scratch 3.0 Compatibility
- Full programming functionality equivalent to official Scratch 3.0
- All categories supported: Motion, Looks, Sound, Events, Control, Operators, Variables, and more
- Sprite, costume, and backdrop editing capabilities
- Project save/load functionality (.sb3 format)

### Minecraft Extension
- Real-time control of Minecraft Java Edition
- Support for 700+ block types
- High-speed communication via WebSocket
- Intuitive block-based programming

### Ease of Use
- Full Japanese interface support
- Runs in browser, no installation needed
- Responsive design supported

---

## About the Minecraft Extension

Use Scratch blocks to freely manipulate the Minecraft world.
Learn programming fundamentals while creating creative builds and automation.

### Main Features

| Feature | Description |
|---------|-------------|
| Place Block | Place a block at specified coordinates |
| Break Block | Destroy a block at specified coordinates |
| Move Player | Change player position |
| Get Block | Retrieve block information at specified coordinates |
| Range Operations | Place blocks in bulk within a specified range |

### Programming Examples

**Example 1: Build a Stone Tower**
```
repeat 10 times
  place "Stone" block at x:0 y:(counter) z:0
```

**Example 2: Create a Rainbow Bridge**
```
colorList = [Red Wool, Orange Wool, Yellow Wool, ...]
for each color in list
  place "color" block and x += 1
```

**Example 3: Auto-Leveling**
```
repeat x: -10 to 10
  repeat z: -10 to 10
    change block at (x, 64, z) to "Grass Block"
```

---

## Supported Blocks

Over 700 Minecraft blocks organized into 7 categories.

### Building Blocks (BUILDING_BLOCKS)
Basic blocks for construction
- Stone, Cobblestone, Stone Bricks
- Various Wood Types (Oak, Birch, Dark Oak, Mangrove, etc.)
- Bricks, Nether Bricks
- Concrete, Terracotta
- Quartz Block, Prismarine
- Copper Blocks (Normal, Weathered, Oxidized)

### Lighting Blocks (LIGHTING_BLOCKS)
Light source blocks
- Torch, Soul Torch
- Lantern, Soul Lantern
- Glowstone, Sea Lantern
- End Rod, Froglight
- Various Candles

### Decoration Blocks (DECORATION_BLOCKS)
Blocks for decoration and interior design
- Carpet (all 16 colors)
- Banners, Item Frames
- Flower Pots, Vases
- Glass, Stained Glass
- Stairs, Slabs

### Nature Blocks (NATURE_BLOCKS)
Blocks that make up natural environments
- Grass Block, Dirt, Sand
- Various Flowers (Poppy, Dandelion, Sunflower, etc.)
- Various Leaves (Oak, Birch, Cherry, etc.)
- Moss, Vines, Glow Berries
- Coral, Seagrass

### Functional Blocks (FUNCTIONAL_BLOCKS)
Blocks with game functionality
- Chest, Ender Chest
- Crafting Table, Furnace, Blast Furnace
- Enchanting Table, Anvil
- Beacon, Conduit
- Redstone Components

### Ore Blocks (ORE_BLOCKS)
Ores and metal blocks
- Diamond Ore/Block
- Gold Ore/Block
- Iron Ore/Block
- Emerald Ore/Block
- Netherite, Ancient Debris

### Special Blocks (SPECIAL_BLOCKS)
Blocks with special functions
- Command Block
- Structure Block
- Barrier Block
- Spawner

---

## How to Use

### Basic Usage

1. **Access**: Go to https://laughtale01.github.io/Scratch
2. **Add Extension**: Click the "Add Extension" button (block icon) at bottom left
3. **Select Minecraft**: Choose "Minecraft" from the extension list
4. **Start Programming**: Green Minecraft blocks will be added

### Connecting to Minecraft (When Using MOD)

1. Install the dedicated MOD in Minecraft Java Edition
2. Launch Minecraft and enter a world
3. Execute the "Connect" block on the Scratch side
4. After successful connection, programs will be reflected in Minecraft

### Saving Projects

- **File → Save to your computer**: Save as .sb3 file
- **File → Load from your computer**: Open saved projects

---

## System Requirements

### Recommended Browsers

| Browser | Support Status |
|---------|----------------|
| Google Chrome | ✅ Recommended |
| Microsoft Edge | ✅ Recommended |
| Brave | ✅ Supported |
| Opera | ✅ Supported |
| Firefox | ⚠️ Limited functionality |
| Safari | ❌ Not supported |

> **Important**: Safari does not work properly. Chrome or Edge is strongly recommended.

### Requirements for Minecraft Integration

- Minecraft Java Edition (version 1.20.x recommended)
- Dedicated integration MOD
- Operation on the same network

---

## Technical Specifications

### Communication Protocol
- Bidirectional real-time communication via WebSocket
- JSON format messaging
- Auto-reconnection functionality

### Supported Minecraft Versions
- Minecraft Java Edition 1.20.x
- Forge / Fabric MOD compatible

### Base Technologies
- Scratch 3.0 (scratch-gui, scratch-vm, scratch-blocks)
- React.js
- WebSocket API

---

## Troubleshooting

### No Sound
Due to browser autoplay policies, audio cannot play before the first click.
Click anywhere on the screen before using sound blocks.

### Cannot Connect to Minecraft
1. Verify the MOD is correctly installed
2. Confirm Minecraft is running and in a world
3. Check firewall settings
4. Check browser console for errors

### Blocks Not Displaying
1. Reload the page (Ctrl+F5)
2. Clear browser cache
3. Try a different browser

---

## Update History

### December 2025
- Fixed sound library loading issues
- Resolved FetchWorkerTool compatibility problems
- Added AudioContext browser policy support
- Cleaned up development and debug files

### November 2025
- Improved Minecraft extension icon
- Localized extension description to Japanese
- Added copper block variations (including waxed)
- Support for 700+ block types

---

## License

MIT License

This project is open source. Feel free to use, modify, and redistribute.

---

## Credits & Acknowledgments

### Base Projects
- [Scratch](https://scratch.mit.edu/) - MIT Media Lab
- [scratch-gui](https://github.com/scratchfoundation/scratch-gui)
- [scratch-vm](https://github.com/scratchfoundation/scratch-vm)

### Development
- Minecraft Extension Development: **Laughtale01**

### Special Thanks
- Scratch Foundation
- Minecraft Community
- Open Source Contributors

---

## Contact & Feedback

For bug reports and feature requests, please visit [GitHub Issues](https://github.com/laughtale01/Scratch/issues).

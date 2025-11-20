# Vertical Slab MOD Implementation Plan

**Project**: Minecraft Laughtare - Vertical Slab Extension
**Target**: Minecraft 1.20.1 with Forge MOD
**Approach**: Prototype-first development
**Date**: 2025-11-15

---

## Executive Summary

This document outlines the implementation plan for adding vertical slabs to the Minecraft Laughtare project. Based on requirements gathering, we will implement a **prototype with oak_vertical_slab** as the initial block, with the architecture designed for future expansion to other slab types.

---

## Requirements Summary

### Functional Requirements

| Requirement | Decision | Rationale |
|------------|----------|-----------|
| **Initial Scope** | Single block: `vertical_oak_slab` | Prototype-first approach for validation |
| **Naming Convention** | `vertical_oak_slab` (prefix format) | Clear distinction from vanilla slabs |
| **Scratch UI Display** | Japanese names (e.g., "オークのスラブ（垂直）") | User-friendly distinction in UI |
| **Texture Strategy** | Reuse vanilla `oak_slab` texture | Minimize asset creation, leverage existing quality |
| **Block Properties** | `facing`: north/south/east/west | Directional placement capability |
| **Placement Behavior** | Similar to vanilla slabs (click side) | Familiar user experience |

### Technical Requirements

| Requirement | Decision | Rationale |
|------------|----------|-----------|
| **Development Priority** | 1. Working prototype<br>2. Texture/model quality | Validate concept before perfection |
| **Testing Strategy** | Local + JUnit + Scratch integration | Comprehensive coverage |
| **Code Modification Scope** | Large-scale refactoring permitted | Optimize architecture from start |
| **Forge Version** | 1.20.1 (existing) | Match current project version |

---

## Technical Architecture

### 1. Block Class Design

**File**: `minecraft-mod/src/main/java/com/example/minecraftmod/block/VerticalSlabBlock.java` (NEW)

```java
public class VerticalSlabBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Properties:
    // - facing: north, south, east, west (horizontal only)
    // - waterlogged: true, false (for compatibility)

    // Behavior:
    // - Collision box: 8x16x16 (half width, full height/depth)
    // - Placement: Based on player look direction and hit side
    // - Combine: Two vertical slabs = full block
}
```

**Key Features**:
- Extends `Block` (not `SlabBlock` - different behavior)
- Uses `DirectionProperty` for 4 horizontal directions
- Custom VoxelShape for half-width collision
- Waterloggable for realism

### 2. Block Registration

**File**: `minecraft-mod/src/main/java/com/example/minecraftmod/init/ModBlocks.java` (MODIFY)

```java
public class ModBlocks {
    // Existing registrations...

    public static final RegistryObject<Block> VERTICAL_OAK_SLAB = BLOCKS.register(
        "vertical_oak_slab",
        () -> new VerticalSlabBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS))
    );
}
```

### 3. Item Registration

**File**: `minecraft-mod/src/main/java/com/example/minecraftmod/init/ModItems.java` (MODIFY)

```java
public class ModItems {
    // Existing registrations...

    public static final RegistryObject<Item> VERTICAL_OAK_SLAB = ITEMS.register(
        "vertical_oak_slab",
        () -> new BlockItem(ModBlocks.VERTICAL_OAK_SLAB.get(), new Item.Properties())
    );
}
```

### 4. Block States and Models

**Files to Create**:

1. `minecraft-mod/src/main/resources/assets/minecraftmod/blockstates/vertical_oak_slab.json`
```json
{
  "variants": {
    "facing=north": { "model": "minecraftmod:block/vertical_oak_slab" },
    "facing=south": { "model": "minecraftmod:block/vertical_oak_slab", "y": 180 },
    "facing=east": { "model": "minecraftmod:block/vertical_oak_slab", "y": 90 },
    "facing=west": { "model": "minecraftmod:block/vertical_oak_slab", "y": 270 }
  }
}
```

2. `minecraft-mod/src/main/resources/assets/minecraftmod/models/block/vertical_oak_slab.json`
```json
{
  "parent": "block/block",
  "textures": {
    "texture": "minecraft:block/oak_planks"
  },
  "elements": [
    {
      "from": [0, 0, 0],
      "to": [8, 16, 16],
      "faces": {
        "north": { "texture": "#texture" },
        "south": { "texture": "#texture" },
        "east": { "texture": "#texture" },
        "west": { "texture": "#texture" },
        "up": { "texture": "#texture" },
        "down": { "texture": "#texture" }
      }
    }
  ]
}
```

3. `minecraft-mod/src/main/resources/assets/minecraftmod/models/item/vertical_oak_slab.json`
```json
{
  "parent": "minecraftmod:block/vertical_oak_slab"
}
```

### 5. Scratch Extension Integration

**File**: `scratch-client/scratch-vm/src/extensions/scratch3_minecraft/index.js` (MODIFY)

**Changes Needed**:

1. Add vertical_oak_slab to block list menu
2. Update `_buildBlockTypeWithProperties` to handle vertical slabs
3. Add Japanese localization: "オークのスラブ（垂直）"

**Code Location**: Around line 180-250 (block definitions)

```javascript
// Add to BLOCK_TYPES menu
{
    text: 'オークのスラブ（垂直）',
    value: 'vertical_oak_slab'
}
```

### 6. WebSocket Protocol

**No Changes Required** - Existing protocol supports:
- Block placement with properties
- `facing` parameter already exists for stairs/doors
- Can reuse existing `placeBlock` action

---

## Implementation Steps

### Phase 1: Core Block Implementation (Forge MOD)

**Priority**: HIGHEST
**Estimated Time**: 2-3 hours

1. Create `VerticalSlabBlock.java` class
   - Define FACING property
   - Implement VoxelShape (collision box)
   - Implement placement logic
   - Add waterlogging support

2. Register block in `ModBlocks.java`
3. Register item in `ModItems.java`
4. Verify compilation with `./gradlew build`

**Success Criteria**:
- ✅ Code compiles without errors
- ✅ Block registered in Forge registry
- ✅ Item registered in Forge registry

### Phase 2: Block States and Models

**Priority**: HIGH
**Estimated Time**: 1-2 hours

1. Create blockstate JSON with 4 facing variants
2. Create block model JSON (half-width cube)
3. Create item model JSON
4. Reuse vanilla oak_planks texture

**Success Criteria**:
- ✅ All JSON files valid (no syntax errors)
- ✅ Model references correct texture path
- ✅ 4 rotation states defined correctly

### Phase 3: Local Testing

**Priority**: HIGH
**Estimated Time**: 1 hour

1. Build MOD with `./gradlew build`
2. Launch Minecraft with MOD loaded
3. Test in creative mode:
   - Block appears in creative inventory
   - Block places in correct direction
   - Block renders correctly
   - Collision box is correct
   - Texture displays properly

**Success Criteria**:
- ✅ Block visible in game
- ✅ Placement works in all 4 directions
- ✅ Visual rendering correct
- ✅ No crashes or errors

### Phase 4: Scratch Extension Update

**Priority**: MEDIUM
**Estimated Time**: 1 hour

1. Add vertical_oak_slab to block menu
2. Add Japanese localization
3. Update block list in `getInfo()`
4. Verify `_buildBlockTypeWithProperties` handles it

**Success Criteria**:
- ✅ Block appears in Scratch dropdown
- ✅ Japanese name displays correctly
- ✅ Block ID sent correctly over WebSocket

### Phase 5: JUnit Unit Tests

**Priority**: MEDIUM
**Estimated Time**: 1-2 hours

**File**: `minecraft-mod/src/test/java/com/example/minecraftmod/block/VerticalSlabBlockTest.java` (NEW)

**Test Cases**:
1. Block registration test
2. Properties test (FACING has 4 values)
3. VoxelShape test (correct dimensions)
4. Placement logic test
5. Blockstate property test

**Success Criteria**:
- ✅ All unit tests pass
- ✅ Code coverage > 80% for VerticalSlabBlock

### Phase 6: Integration Tests

**Priority**: MEDIUM
**Estimated Time**: 1 hour

**File**: `scratch-client/scratch-vm/__tests__/scratch3_minecraft.test.js` (MODIFY)

**Test Cases**:
1. `_buildBlockTypeWithProperties('vertical_oak_slab', 'bottom', 'north')` → `minecraftmod:vertical_oak_slab[facing=north]`
2. All 4 facing directions
3. Verify placement parameter ignored for vertical slabs

**Success Criteria**:
- ✅ All Scratch extension tests pass
- ✅ Vertical slab tests added and passing

### Phase 7: Scratch-to-Minecraft Integration Test

**Priority**: MEDIUM
**Estimated Time**: 1 hour

**Manual Test**:
1. Start Minecraft server with MOD
2. Start Scratch with extension
3. Connect Scratch to Minecraft
4. Use Scratch block to place vertical_oak_slab
5. Verify placement in all 4 directions
6. Test from different player orientations

**Success Criteria**:
- ✅ Scratch successfully places vertical slabs
- ✅ Direction parameter respected
- ✅ No WebSocket errors
- ✅ Visual confirmation in Minecraft

### Phase 8: Documentation

**Priority**: LOW
**Estimated Time**: 30 minutes

**Files to Update**:
1. `BLOCK_REFERENCE.md` - Add vertical slab section
2. `README.md` - Note new feature
3. Create `docs/VERTICAL_SLAB_USAGE.md` - Usage guide

**Content**:
- How to use vertical slabs in Scratch
- Differences from vanilla slabs
- Screenshots/examples
- Technical details

**Success Criteria**:
- ✅ Documentation complete and accurate
- ✅ Examples provided
- ✅ Japanese translations included

---

## Code Refactoring Opportunities

Since user approved large-scale refactoring, consider:

### 1. Block Property System Refactor

**Current**: `_buildBlockTypeWithProperties` has growing if/else logic
**Proposed**: Block metadata system

```javascript
const BLOCK_METADATA = {
    'stone_slab': {
        type: 'slab',
        properties: ['type'],  // Only type, no facing
        namespace: 'minecraft'
    },
    'oak_stairs': {
        type: 'stairs',
        properties: ['half', 'facing'],
        namespace: 'minecraft'
    },
    'vertical_oak_slab': {
        type: 'vertical_slab',
        properties: ['facing'],  // Only facing
        namespace: 'minecraftmod'
    }
};
```

**Benefits**:
- Easier to add new blocks
- Clearer property validation
- Self-documenting code

### 2. WebSocket Message Handler Refactor

**Current**: Large switch statement in message handler
**Proposed**: Command pattern with handlers

**Benefits**:
- Each action in separate handler
- Easier testing
- Better separation of concerns

---

## Testing Matrix

| Test Type | Scope | Tools | Priority |
|-----------|-------|-------|----------|
| **Unit Tests (Java)** | Block class logic | JUnit 5 | HIGH |
| **Unit Tests (JS)** | Extension property building | Jest | HIGH |
| **Integration Tests** | Scratch → Minecraft flow | Manual + Jest | MEDIUM |
| **Local Minecraft** | Visual/gameplay verification | Manual | HIGH |
| **Regression Tests** | Existing functionality | Existing test suite | HIGH |

---

## Risk Assessment

| Risk | Probability | Impact | Mitigation |
|------|------------|--------|------------|
| **Texture doesn't render** | LOW | MEDIUM | Use exact vanilla texture path |
| **Collision box incorrect** | MEDIUM | MEDIUM | Test thoroughly in local Minecraft |
| **WebSocket compatibility** | LOW | HIGH | Reuse existing facing parameter |
| **Performance impact** | LOW | LOW | Single block, minimal overhead |
| **Forge version conflict** | LOW | HIGH | Use exact 1.20.1 version |

---

## Success Metrics

### MVP (Minimum Viable Product)
- ✅ vertical_oak_slab block exists in game
- ✅ Can be placed in 4 directions
- ✅ Accessible from Scratch
- ✅ No crashes or errors

### Quality Goals
- ✅ All tests passing
- ✅ Code coverage > 80%
- ✅ Documentation complete
- ✅ Performance: < 5ms placement latency

### Expansion Readiness
- ✅ Architecture supports adding more vertical slabs
- ✅ Naming convention established
- ✅ Testing framework in place

---

## Future Expansion Path

After successful oak prototype:

1. **Phase 2 Blocks** (Low effort):
   - vertical_stone_slab
   - vertical_birch_slab
   - vertical_spruce_slab

2. **Phase 3 Blocks** (Medium effort):
   - All vanilla wood types (6 total)
   - Common stone types (4-5 total)

3. **Phase 4 Features** (Higher effort):
   - Crafting recipes (2 vertical = 1 full block)
   - Stonecutter support
   - Double vertical slabs (corner pieces)

---

## Implementation Timeline

| Phase | Duration | Dependencies |
|-------|----------|--------------|
| 1. Core Block | 2-3 hours | None |
| 2. Models | 1-2 hours | Phase 1 |
| 3. Local Testing | 1 hour | Phase 1-2 |
| 4. Scratch Extension | 1 hour | Phase 3 |
| 5. JUnit Tests | 1-2 hours | Phase 1 |
| 6. Integration Tests | 1 hour | Phase 4-5 |
| 7. Scratch Integration | 1 hour | Phase 4 |
| 8. Documentation | 30 min | Phase 7 |

**Total Estimated Time**: 8-11 hours of focused development

---

## Appendix A: File Structure

```
minecraft-mod/
├── src/main/java/com/example/minecraftmod/
│   ├── block/
│   │   └── VerticalSlabBlock.java          [NEW]
│   └── init/
│       ├── ModBlocks.java                   [MODIFY]
│       └── ModItems.java                    [MODIFY]
├── src/main/resources/assets/minecraftmod/
│   ├── blockstates/
│   │   └── vertical_oak_slab.json          [NEW]
│   ├── models/
│   │   ├── block/
│   │   │   └── vertical_oak_slab.json      [NEW]
│   │   └── item/
│   │       └── vertical_oak_slab.json      [NEW]
│   └── lang/
│       └── ja_jp.json                       [MODIFY]
└── src/test/java/com/example/minecraftmod/
    └── block/
        └── VerticalSlabBlockTest.java       [NEW]

scratch-client/scratch-vm/
├── src/extensions/scratch3_minecraft/
│   └── index.js                             [MODIFY]
└── __tests__/
    └── scratch3_minecraft.test.js           [MODIFY]

docs/
├── VERTICAL_SLAB_IMPLEMENTATION_PLAN.md     [THIS FILE]
└── VERTICAL_SLAB_USAGE.md                   [NEW]
```

---

## Appendix B: Key Code Locations

### Scratch Extension (index.js)

| Function | Line Range | Purpose |
|----------|------------|---------|
| `getInfo()` | ~150-180 | Add block to menu |
| Block definitions | ~180-250 | Add vertical_oak_slab entry |
| `_buildBlockTypeWithProperties` | ~730-750 | Handle vertical slab properties |
| Localization | ~900-1000 | Japanese names |

### Forge MOD

| File | Purpose | Status |
|------|---------|--------|
| `ModBlocks.java` | Block registry | EXISTS - needs vertical_oak_slab |
| `ModItems.java` | Item registry | EXISTS - needs vertical_oak_slab |
| `VerticalSlabBlock.java` | Block logic | NEW - create from scratch |

---

## Notes

- This plan assumes existing WebSocket infrastructure works correctly
- Texture reuse reduces development time significantly
- Prototype-first approach allows validation before expansion
- Large refactoring approved - can optimize architecture during implementation
- Focus on quality over speed (user explicitly approved long development time)

---

**Plan Status**: READY FOR IMPLEMENTATION
**Next Step**: Begin Phase 1 - Core Block Implementation

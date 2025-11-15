/**
 * Tests for Scratch3MinecraftBlocks extension
 *
 * テスト対象:
 * 1. ブロックプロパティ構築ロジック (_buildBlockTypeWithProperties)
 * 2. Y座標変換ロジック (_toMinecraftY, _toScratchY)
 * 3. 座標検証ロジック (_validateCoordinate, _validateXZCoordinate, _validateYCoordinate)
 */

const Scratch3MinecraftBlocks = require('../src/extensions/scratch3_minecraft/index.js');

describe('Scratch3MinecraftBlocks', () => {
    let instance;

    beforeEach(() => {
        // 各テストの前に新しいインスタンスを作成
        // runtimeモックを渡す（最小限の実装）
        const mockRuntime = {
            on: jest.fn(),
            emit: jest.fn()
        };
        instance = new Scratch3MinecraftBlocks(mockRuntime);
    });

    describe('Constructor', () => {
        test('should initialize with correct constants', () => {
            expect(instance.Y_OFFSET).toBe(-60);
            expect(instance.MAX_XZ_COORD).toBe(30000000);
            expect(instance.MIN_XZ_COORD).toBe(-30000000);
            expect(instance.MAX_Y_COORD).toBe(319);
            expect(instance.MIN_Y_COORD).toBe(-64);
        });

        test('should initialize WebSocket connection state', () => {
            expect(instance.socket).toBeNull();
            expect(instance.connected).toBe(false);
            expect(instance.sessionId).toBeNull();
        });
    });

    describe('_buildBlockTypeWithProperties', () => {
        test('should build basic block type without properties', () => {
            const result = instance._buildBlockTypeWithProperties('stone', 'bottom', 'none');
            expect(result).toBe('minecraft:stone');
        });

        test('should build slab with type=top property', () => {
            const result = instance._buildBlockTypeWithProperties('stone_slab', 'top', 'none');
            expect(result).toBe('minecraft:stone_slab[type=top]');
        });

        test('should build slab with type=double property', () => {
            const result = instance._buildBlockTypeWithProperties('oak_slab', 'double', 'none');
            expect(result).toBe('minecraft:oak_slab[type=double]');
        });

        test('should build slab with type=bottom (default) - no property added', () => {
            const result = instance._buildBlockTypeWithProperties('stone_slab', 'bottom', 'none');
            expect(result).toBe('minecraft:stone_slab');
        });

        // スラブにfacingを設定した場合のテスト（修正の検証）
        test('should ignore facing property for slabs (type=top)', () => {
            const result = instance._buildBlockTypeWithProperties('stone_slab', 'top', 'north');
            expect(result).toBe('minecraft:stone_slab[type=top]');
            // facingが無視されることを確認
        });

        test('should ignore facing property for slabs (type=bottom)', () => {
            const result = instance._buildBlockTypeWithProperties('oak_slab', 'bottom', 'east');
            expect(result).toBe('minecraft:oak_slab');
            // bottomとfacingの両方が無視されることを確認
        });

        test('should ignore facing property for slabs (type=double)', () => {
            const result = instance._buildBlockTypeWithProperties('birch_slab', 'double', 'west');
            expect(result).toBe('minecraft:birch_slab[type=double]');
            // facingが無視されることを確認
        });

        test('should build stairs with half=top property', () => {
            const result = instance._buildBlockTypeWithProperties('oak_stairs', 'top', 'none');
            expect(result).toBe('minecraft:oak_stairs[half=top]');
        });

        test('should build stairs with facing property', () => {
            const result = instance._buildBlockTypeWithProperties('oak_stairs', 'bottom', 'north');
            expect(result).toBe('minecraft:oak_stairs[facing=north]');
        });

        test('should build stairs with both half and facing properties', () => {
            const result = instance._buildBlockTypeWithProperties('stone_stairs', 'top', 'east');
            expect(result).toBe('minecraft:stone_stairs[half=top,facing=east]');
        });

        test('should handle all facing directions', () => {
            expect(instance._buildBlockTypeWithProperties('oak_stairs', 'bottom', 'north'))
                .toBe('minecraft:oak_stairs[facing=north]');
            expect(instance._buildBlockTypeWithProperties('oak_stairs', 'bottom', 'south'))
                .toBe('minecraft:oak_stairs[facing=south]');
            expect(instance._buildBlockTypeWithProperties('oak_stairs', 'bottom', 'east'))
                .toBe('minecraft:oak_stairs[facing=east]');
            expect(instance._buildBlockTypeWithProperties('oak_stairs', 'bottom', 'west'))
                .toBe('minecraft:oak_stairs[facing=west]');
        });
    });

    describe('_toMinecraftY', () => {
        test('should convert Scratch Y=0 to Minecraft Y=-60', () => {
            expect(instance._toMinecraftY(0)).toBe(-60);
        });

        test('should convert Scratch Y=64 to Minecraft Y=4', () => {
            expect(instance._toMinecraftY(64)).toBe(4);
        });

        test('should convert Scratch Y=100 to Minecraft Y=40', () => {
            expect(instance._toMinecraftY(100)).toBe(40);
        });

        test('should convert negative Scratch Y values correctly', () => {
            expect(instance._toMinecraftY(-10)).toBe(-70);
        });

        test('should handle string input by converting to number', () => {
            expect(instance._toMinecraftY('50')).toBe(-10);
        });

        test('should handle Y_OFFSET constant correctly', () => {
            // Y_OFFSET = -60
            // Scratch Y + Y_OFFSET = Minecraft Y
            const scratchY = 20;
            const expectedMinecraftY = scratchY + instance.Y_OFFSET;
            expect(instance._toMinecraftY(scratchY)).toBe(expectedMinecraftY);
        });
    });

    describe('_toScratchY', () => {
        test('should convert Minecraft Y=-60 to Scratch Y=0', () => {
            expect(instance._toScratchY(-60)).toBe(0);
        });

        test('should convert Minecraft Y=4 to Scratch Y=64', () => {
            expect(instance._toScratchY(4)).toBe(64);
        });

        test('should convert Minecraft Y=40 to Scratch Y=100', () => {
            expect(instance._toScratchY(40)).toBe(100);
        });

        test('should convert Minecraft Y=-64 (bedrock) to Scratch Y=-4', () => {
            expect(instance._toScratchY(-64)).toBe(-4);
        });

        test('should convert Minecraft Y=319 (max) to Scratch Y=379', () => {
            expect(instance._toScratchY(319)).toBe(379);
        });

        test('should handle string input by converting to number', () => {
            expect(instance._toScratchY('0')).toBe(60);
        });

        test('should be inverse of _toMinecraftY', () => {
            // Round-trip test
            const originalScratchY = 50;
            const minecraftY = instance._toMinecraftY(originalScratchY);
            const backToScratchY = instance._toScratchY(minecraftY);
            expect(backToScratchY).toBe(originalScratchY);
        });
    });

    describe('_validateCoordinate', () => {
        test('should return valid coordinate within range', () => {
            const result = instance._validateCoordinate(100, 'X座標', -1000, 1000);
            expect(result).toBe(100);
        });

        test('should clamp value above maximum to max', () => {
            const result = instance._validateCoordinate(2000, 'X座標', -1000, 1000);
            expect(result).toBe(1000);
        });

        test('should clamp value below minimum to min', () => {
            const result = instance._validateCoordinate(-2000, 'X座標', -1000, 1000);
            expect(result).toBe(-1000);
        });

        test('should handle NaN by returning 0', () => {
            const result = instance._validateCoordinate('invalid', 'X座標', -1000, 1000);
            expect(result).toBe(0);
        });

        test('should handle undefined by returning 0', () => {
            const result = instance._validateCoordinate(undefined, 'X座標', -1000, 1000);
            expect(result).toBe(0);
        });

        test('should handle null by returning 0', () => {
            const result = instance._validateCoordinate(null, 'X座標', -1000, 1000);
            expect(result).toBe(0);
        });

        test('should convert string numbers correctly', () => {
            const result = instance._validateCoordinate('500', 'X座標', -1000, 1000);
            expect(result).toBe(500);
        });

        test('should handle boundary values correctly', () => {
            expect(instance._validateCoordinate(1000, 'X座標', -1000, 1000)).toBe(1000);
            expect(instance._validateCoordinate(-1000, 'X座標', -1000, 1000)).toBe(-1000);
        });
    });

    describe('_validateXZCoordinate', () => {
        test('should accept valid X coordinate within range', () => {
            const result = instance._validateXZCoordinate(1000, 'X座標');
            expect(result).toBe(1000);
        });

        test('should accept maximum X coordinate (30,000,000)', () => {
            const result = instance._validateXZCoordinate(30000000, 'X座標');
            expect(result).toBe(30000000);
        });

        test('should accept minimum X coordinate (-30,000,000)', () => {
            const result = instance._validateXZCoordinate(-30000000, 'X座標');
            expect(result).toBe(-30000000);
        });

        test('should clamp X coordinate above maximum', () => {
            const result = instance._validateXZCoordinate(40000000, 'X座標');
            expect(result).toBe(30000000);
        });

        test('should clamp X coordinate below minimum', () => {
            const result = instance._validateXZCoordinate(-40000000, 'X座標');
            expect(result).toBe(-30000000);
        });

        test('should accept valid Z coordinate within range', () => {
            const result = instance._validateXZCoordinate(5000, 'Z座標');
            expect(result).toBe(5000);
        });
    });

    describe('_validateYCoordinate', () => {
        test('should accept valid Scratch Y coordinate (Y=0)', () => {
            const result = instance._validateYCoordinate(0, 'Y座標', true);
            expect(result).toBe(0);
        });

        test('should accept valid Scratch Y coordinate (Y=100)', () => {
            const result = instance._validateYCoordinate(100, 'Y座標', true);
            expect(result).toBe(100);
        });

        test('should accept Scratch Y coordinate at minimum (-4)', () => {
            // Minecraft Y=-64 corresponds to Scratch Y=-4
            const result = instance._validateYCoordinate(-4, 'Y座標', true);
            expect(result).toBe(-4);
        });

        test('should accept Scratch Y coordinate at maximum (379)', () => {
            // Minecraft Y=319 corresponds to Scratch Y=379
            const result = instance._validateYCoordinate(379, 'Y座標', true);
            expect(result).toBe(379);
        });

        test('should clamp Scratch Y above maximum', () => {
            const result = instance._validateYCoordinate(500, 'Y座標', true);
            expect(result).toBe(379); // Max Scratch Y
        });

        test('should clamp Scratch Y below minimum', () => {
            const result = instance._validateYCoordinate(-100, 'Y座標', true);
            expect(result).toBe(-4); // Min Scratch Y
        });

        test('should accept valid Minecraft Y coordinate when isScratchY=false', () => {
            const result = instance._validateYCoordinate(0, 'Y座標', false);
            expect(result).toBe(0);
        });

        test('should accept Minecraft Y at minimum (-64)', () => {
            const result = instance._validateYCoordinate(-64, 'Y座標', false);
            expect(result).toBe(-64);
        });

        test('should accept Minecraft Y at maximum (319)', () => {
            const result = instance._validateYCoordinate(319, 'Y座標', false);
            expect(result).toBe(319);
        });

        test('should clamp Minecraft Y above maximum', () => {
            const result = instance._validateYCoordinate(400, 'Y座標', false);
            expect(result).toBe(319);
        });

        test('should clamp Minecraft Y below minimum', () => {
            const result = instance._validateYCoordinate(-100, 'Y座標', false);
            expect(result).toBe(-64);
        });
    });

    describe('_generateClientToken', () => {
        test('should generate unique tokens', () => {
            const token1 = instance._generateClientToken();
            const token2 = instance._generateClientToken();
            expect(token1).not.toBe(token2);
        });

        test('should generate token with correct format', () => {
            const token = instance._generateClientToken();
            expect(token).toMatch(/^scratch_token_\d+_[a-z0-9]+$/);
        });

        test('should include timestamp in token', () => {
            const beforeTime = Date.now();
            const token = instance._generateClientToken();
            const afterTime = Date.now();

            const match = token.match(/^scratch_token_(\d+)_/);
            expect(match).not.toBeNull();

            const timestamp = parseInt(match[1]);
            expect(timestamp).toBeGreaterThanOrEqual(beforeTime);
            expect(timestamp).toBeLessThanOrEqual(afterTime);
        });
    });
});

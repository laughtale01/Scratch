// Simple Test Extension
(function(Scratch) {
    'use strict';

    if (!Scratch.extensions) {
        throw new Error('Scratch.extensions is not defined');
    }

    class SimpleTestExtension {
        constructor(runtime) {
            this.runtime = runtime;
        }

        getInfo() {
            return {
                id: 'simpleTest',
                name: 'Simple Test',
                color1: '#FF6B6B',
                color2: '#FF5252',
                blocks: [
                    {
                        opcode: 'block1',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 1'
                    },
                    {
                        opcode: 'block2',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 2'
                    },
                    {
                        opcode: 'block3',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 3'
                    },
                    {
                        opcode: 'block4',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 4'
                    },
                    {
                        opcode: 'block5',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 5'
                    },
                    {
                        opcode: 'block6',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 6'
                    },
                    {
                        opcode: 'block7',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 7'
                    },
                    {
                        opcode: 'block8',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 8'
                    },
                    {
                        opcode: 'block9',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 9'
                    },
                    {
                        opcode: 'block10',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 10'
                    },
                    {
                        opcode: 'block11',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 11'
                    },
                    {
                        opcode: 'block12',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 12'
                    },
                    {
                        opcode: 'block13',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 13'
                    },
                    {
                        opcode: 'block14',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 14'
                    },
                    {
                        opcode: 'block15',
                        blockType: Scratch.BlockType.COMMAND,
                        text: 'Block 15'
                    }
                ]
            };
        }

        // Block implementation methods
        block1() { console.log('Block 1 executed'); }
        block2() { console.log('Block 2 executed'); }
        block3() { console.log('Block 3 executed'); }
        block4() { console.log('Block 4 executed'); }
        block5() { console.log('Block 5 executed'); }
        block6() { console.log('Block 6 executed'); }
        block7() { console.log('Block 7 executed'); }
        block8() { console.log('Block 8 executed'); }
        block9() { console.log('Block 9 executed'); }
        block10() { console.log('Block 10 executed'); }
        block11() { console.log('Block 11 executed'); }
        block12() { console.log('Block 12 executed'); }
        block13() { console.log('Block 13 executed'); }
        block14() { console.log('Block 14 executed'); }
        block15() { console.log('Block 15 executed'); }
    }

    Scratch.extensions.register(new SimpleTestExtension());
})(Scratch);
const MinecraftExtension = require('../src/index.js');

// Mock WebSocket
class MockWebSocket {
    constructor(url) {
        this.url = url;
        this.readyState = WebSocket.CONNECTING;
        this.listeners = {};
        this.sentMessages = [];
        
        // Simulate connection
        setTimeout(() => {
            this.readyState = WebSocket.OPEN;
            this.trigger('open');
        }, 10);
    }
    
    on(event, handler) {
        if (!this.listeners[event]) {
            this.listeners[event] = [];
        }
        this.listeners[event].push(handler);
    }
    
    trigger(event, data) {
        if (this.listeners[event]) {
            this.listeners[event].forEach(handler => handler(data));
        }
    }
    
    send(data) {
        this.sentMessages.push(data);
    }
    
    close() {
        this.readyState = WebSocket.CLOSED;
        this.trigger('close');
    }
}

// Mock runtime
const mockRuntime = {
    on: jest.fn()
};

// Replace global WebSocket with mock
global.WebSocket = MockWebSocket;
global.WebSocket.OPEN = 1;
global.WebSocket.CLOSED = 3;
global.WebSocket.CONNECTING = 0;

describe('MinecraftExtension', () => {
    let extension;
    
    beforeEach(() => {
        extension = new MinecraftExtension(mockRuntime);
        jest.clearAllMocks();
    });
    
    afterEach(() => {
        if (extension.websocket) {
            extension.websocket.close();
        }
    });
    
    describe('Extension Info', () => {
        test('should return correct extension info', () => {
            const info = extension.getInfo();
            
            expect(info.id).toBe('minecraft');
            expect(info.name).toBe('🎮 Minecraft Controller');
            expect(info.blocks).toBeInstanceOf(Array);
            expect(info.blocks.length).toBeGreaterThan(0);
            expect(info.menus).toBeInstanceOf(Object);
        });
        
        test('should define all required blocks', () => {
            const info = extension.getInfo();
            const blockOpcodes = info.blocks.filter(b => typeof b === 'object').map(b => b.opcode);
            
            const requiredBlocks = [
                'connect', 'isConnected', 'placeBlock', 'removeBlock',
                'getPlayerX', 'getPlayerY', 'getPlayerZ',
                'teleportPlayer', 'sendChat', 'inviteFriend',
                'getInvitations', 'getCurrentWorld', 'returnHome'
            ];
            
            requiredBlocks.forEach(opcode => {
                expect(blockOpcodes).toContain(opcode);
            });
        });
    });
    
    describe('Connection Management', () => {
        test('should connect to WebSocket server', (done) => {
            extension.connect();
            
            setTimeout(() => {
                expect(extension.websocket).toBeDefined();
                expect(extension.websocket.url).toBe('ws://localhost:14711');
                expect(extension.connectionStatus).toBe('connected');
                done();
            }, 20);
        });
        
        test('should not create duplicate connections', () => {
            extension.connect();
            const firstWs = extension.websocket;
            
            extension.connect();
            const secondWs = extension.websocket;
            
            expect(firstWs).toBe(secondWs);
        });
        
        test('should report connection status correctly', (done) => {
            expect(extension.isConnected()).toBe(false);
            
            extension.connect();
            
            setTimeout(() => {
                expect(extension.isConnected()).toBe(true);
                done();
            }, 20);
        });
    });
    
    describe('Block Commands', () => {
        beforeEach((done) => {
            extension.connect();
            setTimeout(done, 20);
        });
        
        test('placeBlock should send correct command', () => {
            extension.placeBlock({
                X: 10,
                Y: 64,
                Z: 20,
                BLOCK: 'stone'
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.command).toBe('placeBlock');
            expect(sentMessage.args).toEqual({
                x: '10',
                y: '64',
                z: '20',
                block: 'stone'
            });
        });
        
        test('removeBlock should send removeBlock command', () => {
            extension.removeBlock({
                X: 5,
                Y: 70,
                Z: -10
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.command).toBe('removeBlock');
            expect(sentMessage.args).toEqual({
                x: '5',
                y: '70',
                z: '-10'
            });
        });
        
        test('should validate numeric inputs', () => {
            extension.placeBlock({
                X: 'abc',
                Y: null,
                Z: undefined,
                BLOCK: 'dirt'
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            // Should use default values for invalid inputs
            expect(sentMessage.args.x).toBe('0');
            expect(sentMessage.args.y).toBe('64');
            expect(sentMessage.args.z).toBe('0');
        });
    });
    
    describe('Player Position', () => {
        beforeEach((done) => {
            extension.connect();
            setTimeout(done, 20);
        });
        
        test('should update player position from server message', () => {
            extension.websocket.trigger('message', JSON.stringify({
                type: 'playerPos',
                data: { x: 100.5, y: 65, z: -200.3 }
            }));
            
            expect(extension.playerPos).toEqual({ x: 100.5, y: 65, z: -200.3 });
        });
        
        test('should return correct player coordinates', () => {
            extension.playerPos = { x: 50, y: 70, z: -30 };
            
            expect(extension.getPlayerX()).toBe(50);
            expect(extension.getPlayerY()).toBe(70);
            expect(extension.getPlayerZ()).toBe(-30);
        });
    });
    
    describe('Collaboration Features', () => {
        beforeEach((done) => {
            extension.connect();
            setTimeout(done, 20);
        });
        
        test('inviteFriend should send legacy format', () => {
            extension.inviteFriend({ FRIEND: 'TestPlayer' });
            
            const sentMessage = extension.websocket.sentMessages[0];
            expect(sentMessage).toBe('collaboration.invite(TestPlayer)');
        });
        
        test('requestVisit should send legacy format', () => {
            extension.requestVisit({ FRIEND: 'HostPlayer' });
            
            const sentMessage = extension.websocket.sentMessages[0];
            expect(sentMessage).toBe('collaboration.requestVisit(HostPlayer)');
        });
        
        test('should update invitation count from server', () => {
            extension.websocket.trigger('message', JSON.stringify({
                type: 'invitations',
                data: { count: 3 }
            }));
            
            expect(extension.invitationCount).toBe(3);
            expect(extension.getInvitations()).toBe(3);
        });
        
        test('should update current world from server', () => {
            extension.websocket.trigger('message', JSON.stringify({
                type: 'currentWorld',
                data: { world: 'minecraft:nether' }
            }));
            
            expect(extension.currentWorld).toBe('minecraft:nether');
            expect(extension.getCurrentWorld()).toBe('minecraft:nether');
        });
    });
    
    describe('Building Features', () => {
        beforeEach((done) => {
            extension.connect();
            setTimeout(done, 20);
        });
        
        test('buildCircle should send correct parameters', () => {
            extension.buildCircle({
                X: 0,
                Y: 64,
                Z: 0,
                RADIUS: 5,
                BLOCK: 'stone'
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.command).toBe('buildCircle');
            expect(sentMessage.args).toEqual({
                x: '0',
                y: '64',
                z: '0',
                radius: '5',
                block: 'stone'
            });
        });
        
        test('buildSphere should send correct parameters', () => {
            extension.buildSphere({
                X: 10,
                Y: 70,
                Z: 10,
                RADIUS: 3,
                BLOCK: 'glass'
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.command).toBe('buildSphere');
            expect(sentMessage.args.radius).toBe('3');
            expect(sentMessage.args.block).toBe('glass');
        });
        
        test('buildWall should send correct parameters', () => {
            extension.buildWall({
                X1: 0,
                Z1: 0,
                X2: 10,
                Z2: 0,
                HEIGHT: 5,
                BLOCK: 'brick'
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.command).toBe('buildWall');
            expect(sentMessage.args.height).toBe('5');
        });
    });
    
    describe('Agent System', () => {
        beforeEach((done) => {
            extension.connect();
            setTimeout(done, 20);
        });
        
        test('summonAgent should send agent name', () => {
            extension.summonAgent({ NAME: 'MyHelper' });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.command).toBe('summonAgent');
            expect(sentMessage.args.name).toBe('MyHelper');
        });
        
        test('moveAgentDirection should validate distance', () => {
            extension.moveAgentDirection({
                DIRECTION: 'forward',
                DISTANCE: 15
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.args.distance).toBe('10'); // Should cap at 10
        });
        
        test('agentFollow should convert to boolean', () => {
            extension.agentFollow({ FOLLOW: 'follow' });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.args.follow).toBe('true');
        });
    });
    
    describe('Error Handling', () => {
        test('should handle invalid JSON messages gracefully', () => {
            extension.connect();
            
            expect(() => {
                extension.websocket.trigger('message', 'invalid json');
            }).not.toThrow();
        });
        
        test('should handle null/undefined arguments', () => {
            extension.connect();
            
            expect(() => {
                extension.sendChat({ MESSAGE: null });
                extension.placeBlock({ X: undefined, Y: null, Z: NaN, BLOCK: '' });
            }).not.toThrow();
        });
        
        test('should not send commands when not connected', () => {
            // Don't connect
            extension.placeBlock({ X: 0, Y: 64, Z: 0, BLOCK: 'stone' });
            
            // No WebSocket should be created
            expect(extension.websocket).toBeNull();
        });
    });
    
    describe('Message Validation', () => {
        beforeEach((done) => {
            extension.connect();
            setTimeout(done, 20);
        });
        
        test('should truncate long chat messages', () => {
            const longMessage = 'A'.repeat(300);
            extension.sendChat({ MESSAGE: longMessage });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            expect(sentMessage.args.message.length).toBe(256);
        });
        
        test('should validate coordinate bounds', () => {
            extension.teleportPlayer({
                X: 40000000,
                Y: 400,
                Z: -40000000
            });
            
            const sentMessage = JSON.parse(extension.websocket.sentMessages[0]);
            // Coordinates should be capped
            expect(parseInt(sentMessage.args.x)).toBeLessThanOrEqual(30000000);
            expect(parseInt(sentMessage.args.y)).toBeLessThanOrEqual(320);
        });
    });
});

// Run tests
if (require.main === module) {
    const jest = require('jest');
    jest.run();
}
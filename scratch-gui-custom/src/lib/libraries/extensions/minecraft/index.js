/**
 * Minecraft拡張機能の定義
 * takecx版と互換性のあるブロック構成
 */

import minecraftIconURL from './minecraft.png';
import minecraftInsetIconURL from './minecraft-small.png';

export default {
    name: 'Minecraft',
    extensionId: 'minecraft',
    collaborator: 'Minecraft Collaboration Project',
    iconURL: minecraftIconURL,
    insetIconURL: minecraftInsetIconURL,
    description: 'Minecraftをプログラミングで操作する',
    featured: true,
    disabled: false,
    bluetoothRequired: false,
    internetConnectionRequired: true,
    launchPeripheralConnectionFlow: false,
    useAutoScan: false,
    connectionIconURL: minecraftIconURL,
    connectionSmallIconURL: minecraftInsetIconURL,
    connectingMessage: 'Minecraftに接続中...',
    
    // 拡張機能のURL（ローカルまたはGitHub Pages）
    getExtensionURL: () => {
        // 開発環境
        if (window.location.hostname === 'localhost') {
            return 'http://localhost:8000/dist/minecraft-collaboration-extension.js';
        }
        // 本番環境（GitHub Pages）
        return 'https://[your-github-username].github.io/minecraft-scratch-extension/minecraft-collaboration-extension.js';
    },
    
    // ヘルプリンク
    helpLink: 'https://github.com/[your-github-username]/minecraft_collaboration_project',
    
    // 翻訳
    translationMap: {
        'ja': {
            'gui.extension.minecraft.description': 'Minecraftをプログラミングで操作する',
            'gui.extension.minecraft.name': 'Minecraft'
        },
        'ja-Hira': {
            'gui.extension.minecraft.description': 'マインクラフトをプログラミングでそうさする',
            'gui.extension.minecraft.name': 'マインクラフト'
        }
    }
};
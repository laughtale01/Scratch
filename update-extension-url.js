const fs = require('fs');
const path = require('path');

// GitHub Pages のユーザー名とリポジトリ名を設定
const GITHUB_USERNAME = 'YOUR_USERNAME'; // 変更してください
const REPO_NAME = 'minecraft-collaboration-project'; // 変更してください
const EXTENSION_URL = `https://${GITHUB_USERNAME}.github.io/${REPO_NAME}/minecraft-extension.js`;

// Scratch GUI の拡張機能設定ファイルのパス
const extensionIndexPath = path.join('scratch-gui', 'src', 'lib', 'libraries', 'extensions', 'index.jsx');

// 拡張機能の設定を読み込み
let content = fs.readFileSync(extensionIndexPath, 'utf8');

// Minecraft 拡張機能の設定を追加（既存の場合は更新）
const minecraftExtension = `
    {
        name: 'Minecraft',
        extensionId: 'minecraftCollaboration',
        collaborator: 'Minecraft x Scratch',
        iconURL: minecraftIcon,
        insetIconURL: minecraftInsetIcon,
        description: 'Control Minecraft from Scratch',
        featured: true,
        disabled: false,
        bluetoothRequired: false,
        internetConnectionRequired: true,
        launchPeripheralConnectionFlow: false,
        useAutoScan: false,
        extensionURL: '${EXTENSION_URL}',
        translationMap: {
            'ja': {
                'gui.extension.minecraftCollaboration.description': 'ScratchからMinecraftを制御'
            },
            'en': {
                'gui.extension.minecraftCollaboration.description': 'Control Minecraft from Scratch'
            }
        }
    }`;

// 既存のMinecraft拡張機能を削除して新しいものを追加
content = content.replace(/\/\/ Minecraft extension start[\s\S]*?\/\/ Minecraft extension end/g, '');

// export default の前に追加
const exportMatch = content.match(/export default \[/);
if (exportMatch) {
    const insertPos = content.indexOf('[', exportMatch.index) + 1;
    content = content.slice(0, insertPos) + 
              '\n    // Minecraft extension start' + 
              minecraftExtension + ',' +
              '\n    // Minecraft extension end\n' + 
              content.slice(insertPos);
}

// アイコンのインポートも追加
const importSection = `
import minecraftIcon from './minecraft/minecraft.png';
import minecraftInsetIcon from './minecraft/minecraft-small.png';
`;

if (!content.includes('minecraftIcon')) {
    // 最後のimport文の後に追加
    const lastImportMatch = content.match(/import[^;]+;(?![\s\S]*import)/);
    if (lastImportMatch) {
        const insertPos = lastImportMatch.index + lastImportMatch[0].length;
        content = content.slice(0, insertPos) + importSection + content.slice(insertPos);
    }
}

// ファイルを保存
fs.writeFileSync(extensionIndexPath, content);

// 拡張機能ファイルをdocsディレクトリにコピー
const extensionSource = path.join('scratch-extension', 'dist', 'minecraft-collaboration-extension.js');
const extensionDest = path.join('docs', 'minecraft-extension.js');

if (fs.existsSync(extensionSource) && fs.existsSync('docs')) {
    fs.copyFileSync(extensionSource, extensionDest);
}

// アイコンファイルを作成
const iconDir = path.join('scratch-gui', 'src', 'lib', 'libraries', 'extensions', 'minecraft');
if (!fs.existsSync(iconDir)) {
    fs.mkdirSync(iconDir, { recursive: true });
}

// 仮のアイコンファイルを作成（実際のアイコンは後で追加）
const iconPlaceholder = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64');
fs.writeFileSync(path.join(iconDir, 'minecraft.png'), iconPlaceholder);
fs.writeFileSync(path.join(iconDir, 'minecraft-small.png'), iconPlaceholder);

console.log('✅ 拡張機能のURLを更新しました');
console.log(`📝 GitHub Username と Repository Name を update-extension-url.js で設定してください`);
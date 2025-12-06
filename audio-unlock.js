/**
 * AudioContext Unlock Script
 *
 * ブラウザの自動再生ポリシーに対応するため、
 * ユーザー操作後にAudioContextを有効化するスクリプト
 */
(function() {
  'use strict';

  // AudioContextの状態を管理
  var audioUnlocked = false;
  var pendingContexts = [];
  var originalAudioContext = window.AudioContext || window.webkitAudioContext;

  // オーバーレイ要素を作成
  function createOverlay() {
    var overlay = document.createElement('div');
    overlay.id = 'audio-unlock-overlay';
    overlay.style.cssText = [
      'position: fixed',
      'top: 0',
      'left: 0',
      'width: 100%',
      'height: 100%',
      'background: rgba(77, 151, 255, 0.95)',
      'display: flex',
      'justify-content: center',
      'align-items: center',
      'z-index: 999999',
      'cursor: pointer',
      'font-family: "Helvetica Neue", Helvetica, Arial, sans-serif'
    ].join(';');

    var content = document.createElement('div');
    content.style.cssText = [
      'background: white',
      'padding: 50px 60px',
      'border-radius: 20px',
      'text-align: center',
      'box-shadow: 0 10px 40px rgba(0,0,0,0.3)',
      'max-width: 90%',
      'animation: fadeIn 0.3s ease-out'
    ].join(';');

    var icon = document.createElement('div');
    icon.style.cssText = 'font-size: 64px; margin-bottom: 20px;';
    icon.textContent = '\uD83D\uDD0A'; // スピーカーアイコン

    var title = document.createElement('h2');
    title.style.cssText = [
      'margin: 0 0 15px 0',
      'color: #333',
      'font-size: 28px',
      'font-weight: 600'
    ].join(';');
    title.textContent = 'Scratch を始める';

    var description = document.createElement('p');
    description.style.cssText = [
      'margin: 0 0 25px 0',
      'color: #666',
      'font-size: 16px',
      'line-height: 1.5'
    ].join(';');
    description.textContent = '音声機能を有効にするには、画面をクリックまたはタップしてください';

    var button = document.createElement('button');
    button.style.cssText = [
      'background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%)',
      'color: white',
      'border: none',
      'padding: 18px 50px',
      'font-size: 20px',
      'font-weight: 600',
      'border-radius: 30px',
      'cursor: pointer',
      'box-shadow: 0 4px 15px rgba(76, 175, 80, 0.4)',
      'transition: transform 0.2s, box-shadow 0.2s'
    ].join(';');
    button.textContent = 'クリックして開始';
    button.onmouseover = function() {
      this.style.transform = 'scale(1.05)';
      this.style.boxShadow = '0 6px 20px rgba(76, 175, 80, 0.5)';
    };
    button.onmouseout = function() {
      this.style.transform = 'scale(1)';
      this.style.boxShadow = '0 4px 15px rgba(76, 175, 80, 0.4)';
    };

    content.appendChild(icon);
    content.appendChild(title);
    content.appendChild(description);
    content.appendChild(button);
    overlay.appendChild(content);

    // アニメーション用のスタイルを追加
    var style = document.createElement('style');
    style.textContent = '@keyframes fadeIn { from { opacity: 0; transform: scale(0.9); } to { opacity: 1; transform: scale(1); } }';
    document.head.appendChild(style);

    return overlay;
  }

  // AudioContextを有効化
  function unlockAudio() {
    if (audioUnlocked) return;
    audioUnlocked = true;

    // 保留中のAudioContextをすべて再開
    pendingContexts.forEach(function(ctx) {
      if (ctx && ctx.state === 'suspended') {
        ctx.resume().catch(function(e) {
          console.warn('Failed to resume AudioContext:', e);
        });
      }
    });
    pendingContexts = [];

    // テスト用のAudioContextを作成して即座に再開
    try {
      var testCtx = new originalAudioContext();
      if (testCtx.state === 'suspended') {
        testCtx.resume();
      }
      // テスト用コンテキストを閉じる
      setTimeout(function() {
        testCtx.close().catch(function() {});
      }, 100);
    } catch (e) {
      console.warn('Failed to create test AudioContext:', e);
    }

    // オーバーレイを削除
    var overlay = document.getElementById('audio-unlock-overlay');
    if (overlay) {
      overlay.style.transition = 'opacity 0.3s ease-out';
      overlay.style.opacity = '0';
      setTimeout(function() {
        if (overlay.parentNode) {
          overlay.parentNode.removeChild(overlay);
        }
      }, 300);
    }

    console.log('Audio unlocked successfully');
  }

  // AudioContextコンストラクタをラップ
  if (originalAudioContext) {
    window.AudioContext = window.webkitAudioContext = function AudioContextWrapper(options) {
      var ctx = new originalAudioContext(options);

      // まだアンロックされていない場合は保留リストに追加
      if (!audioUnlocked) {
        pendingContexts.push(ctx);
      }

      return ctx;
    };

    // プロトタイプをコピー
    window.AudioContext.prototype = originalAudioContext.prototype;
  }

  // DOMContentLoadedでオーバーレイを表示
  function init() {
    // オーバーレイを作成して追加
    var overlay = createOverlay();
    document.body.appendChild(overlay);

    // クリック/タップイベントでアンロック
    var unlockEvents = ['click', 'touchstart', 'touchend', 'keydown'];

    function handleUnlock(e) {
      // オーバーレイ内のクリックのみを処理
      if (e.target.closest && e.target.closest('#audio-unlock-overlay')) {
        unlockAudio();
        // イベントリスナーを削除
        unlockEvents.forEach(function(eventName) {
          document.removeEventListener(eventName, handleUnlock, true);
        });
      }
    }

    unlockEvents.forEach(function(eventName) {
      document.addEventListener(eventName, handleUnlock, true);
    });

    // オーバーレイ自体にもクリックハンドラを追加
    overlay.addEventListener('click', function(e) {
      e.preventDefault();
      e.stopPropagation();
      unlockAudio();
    });
  }

  // DOM準備完了時に初期化
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    // すでにDOMが準備完了している場合
    setTimeout(init, 0);
  }
})();

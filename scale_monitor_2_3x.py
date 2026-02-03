# -*- coding: utf-8 -*-
"""
変数モニターのサイズを2倍から2.3倍に変更するスクリプト
"""

# gui.jsを読み込み
with open('gui.js', 'r', encoding='utf-8') as f:
    content = f.read()

# CSS変更リスト（2倍の値 → 2.3倍の値）
replacements = [
    # monitor-container: 基本フォントサイズ (0.75 * 2.3 = 1.725)
    (
        '.monitor_monitor-container_2aMFn {\\n    position: absolute;\\n    background: hsla(215, 100%, 95%, 1);\\n    z-index: 48;\\n    border: 1px solid hsla(0, 0%, 0%, 0.15);\\n    border-radius: calc(1rem / 2);\\n    font-family: \\"Helvetica Neue\\", Helvetica, Arial, sans-serif;\\n    font-size: 1.5rem;',
        '.monitor_monitor-container_2aMFn {\\n    position: absolute;\\n    background: hsla(215, 100%, 95%, 1);\\n    z-index: 48;\\n    border: 1px solid hsla(0, 0%, 0%, 0.15);\\n    border-radius: calc(1.15rem / 2);\\n    font-family: \\"Helvetica Neue\\", Helvetica, Arial, sans-serif;\\n    font-size: 1.725rem;'
    ),
    # default-monitor: パディング (3 * 2.3 = 6.9 ≈ 7)
    (
        '.monitor_default-monitor_3XnXc {\\n    display: flex;\\n    flex-direction: column;\\n    padding: 6px;\\n}',
        '.monitor_default-monitor_3XnXc {\\n    display: flex;\\n    flex-direction: column;\\n    padding: 7px;\\n}'
    ),
    # label: マージン (5 * 2.3 = 11.5 ≈ 12)
    (
        '.monitor_label_d5zL9 {\\n    font-weight: bold;\\n    text-align: center;\\n    margin: 0 10px;\\n}',
        '.monitor_label_d5zL9 {\\n    font-weight: bold;\\n    text-align: center;\\n    margin: 0 12px;\\n}'
    ),
    # value: 通常表示の値 (40 * 2.3 = 92, 5 * 2.3 = 11.5 ≈ 12, 2 * 2.3 = 4.6 ≈ 5)
    (
        '.monitor_value_ZEFKO {\\n    display: flex;\\n    justify-content: center;\\n    align-items: center;\\n    min-width: 80px;\\n    text-align: center;\\n    margin: 0 10px;\\n    border-radius: calc(1rem / 2);\\n    padding: 0 4px;',
        '.monitor_value_ZEFKO {\\n    display: flex;\\n    justify-content: center;\\n    align-items: center;\\n    min-width: 92px;\\n    text-align: center;\\n    margin: 0 12px;\\n    border-radius: calc(1.15rem / 2);\\n    padding: 0 5px;'
    ),
    # large-value: 大きな表示 (1.4 * 2.3 = 3.22, 3 * 2.3 = 6.9, 1 * 2.3 = 2.3)
    (
        '.monitor_large-value_FvLk2 {\\n    min-height: 2.8rem;\\n    min-width: 6rem;\\n    padding: 0.2rem 0.5rem;\\n    text-align: center;\\n    font-size: 2rem;',
        '.monitor_large-value_FvLk2 {\\n    min-height: 3.22rem;\\n    min-width: 6.9rem;\\n    padding: 0.23rem 0.575rem;\\n    text-align: center;\\n    font-size: 2.3rem;'
    ),
    # list-header: リストヘッダー
    (
        '.monitor_list-header_UVzDb {\\n    background: hsla(0, 100%, 100%, 1);\\n    border-bottom: 1px solid hsla(0, 0%, 0%, 0.15);\\n    text-align: center;\\n    padding: 6px;\\n    font-size: 1.5rem;',
        '.monitor_list-header_UVzDb {\\n    background: hsla(0, 100%, 100%, 1);\\n    border-bottom: 1px solid hsla(0, 0%, 0%, 0.15);\\n    text-align: center;\\n    padding: 7px;\\n    font-size: 1.725rem;'
    ),
    # list-body: リスト本体の高さ計算調整 (44 * 2.3 = 101.2 ≈ 101)
    (
        '.monitor_list-body_Co4zZ {\\n    background: hsla(215, 100%, 95%, 1);\\n    width: 100%;\\n    display: flex;\\n    flex-direction: column;\\n    overflow-x: hidden;\\n    height: calc(100% - 88px);\\n}',
        '.monitor_list-body_Co4zZ {\\n    background: hsla(215, 100%, 95%, 1);\\n    width: 100%;\\n    display: flex;\\n    flex-direction: column;\\n    overflow-x: hidden;\\n    height: calc(100% - 101px);\\n}'
    ),
    # list-row: リスト行 (2 * 2.3 = 4.6 ≈ 5)
    (
        '.monitor_list-row_U\\\\+dzt {\\n    display: flex;\\n    flex-direction: row;\\n    justify-content: space-around;\\n    align-items: center;\\n    padding: 4px;',
        '.monitor_list-row_U\\\\+dzt {\\n    display: flex;\\n    flex-direction: row;\\n    justify-content: space-around;\\n    align-items: center;\\n    padding: 5px;'
    ),
    # list-index: リストインデックス (3 * 2.3 = 6.9 ≈ 7)
    (
        '.monitor_list-index_1EskC {\\n    font-weight: bold;\\n    color: hsla(225, 15%, 40%, 1);\\n    margin: 0 6px;\\n}',
        '.monitor_list-index_1EskC {\\n    font-weight: bold;\\n    color: hsla(225, 15%, 40%, 1);\\n    margin: 0 7px;\\n}'
    ),
    # list-value: リスト値 (22 * 2.3 = 50.6 ≈ 51)
    (
        '.monitor_list-value_\\\\+RzZG {\\n    min-width: 80px;\\n    text-align: left;\\n    margin: 0 6px;\\n    border-radius: calc(1rem / 2);\\n    border: 1px solid hsla(0, 0%, 0%, 0.15);\\n    flex-grow: 1;\\n    height: 44px;\\n}',
        '.monitor_list-value_\\\\+RzZG {\\n    min-width: 92px;\\n    text-align: left;\\n    margin: 0 7px;\\n    border-radius: calc(1.15rem / 2);\\n    border: 1px solid hsla(0, 0%, 0%, 0.15);\\n    flex-grow: 1;\\n    height: 51px;\\n}'
    ),
    # list-footer: リストフッター
    (
        '.monitor_list-footer_gWxIK {\\n    background: hsla(0, 100%, 100%, 1);\\n    display: flex;\\n    flex-direction: row;\\n    justify-content: space-between;\\n    padding: 6px;\\n    font-size: 1.5rem;',
        '.monitor_list-footer_gWxIK {\\n    background: hsla(0, 100%, 100%, 1);\\n    display: flex;\\n    flex-direction: row;\\n    justify-content: space-between;\\n    padding: 7px;\\n    font-size: 1.725rem;'
    ),
    # list-empty: 空リスト (5 * 2.3 = 11.5 ≈ 12)
    (
        '.monitor_list-empty_n9iJc {\\n    text-align: center;\\n    width: 100%;\\n    padding: 10px;\\n}',
        '.monitor_list-empty_n9iJc {\\n    text-align: center;\\n    width: 100%;\\n    padding: 12px;\\n}'
    ),
    # value-inner: 値の内部
    (
        '.monitor_value-inner_rQVSL {\\n    padding: 6px 10px;\\n    min-height: 44px;',
        '.monitor_value-inner_rQVSL {\\n    padding: 7px 12px;\\n    min-height: 51px;'
    ),
    # list-input: リスト入力
    (
        '.monitor_list-input_gb-48 {\\n    padding: 6px 10px;\\n    border: 0;\\n    background: none;\\n    outline: none;\\n    font-size: 1.5rem;',
        '.monitor_list-input_gb-48 {\\n    padding: 7px 12px;\\n    border: 0;\\n    background: none;\\n    outline: none;\\n    font-size: 1.725rem;'
    ),
]

# 置換実行
count = 0
for old, new in replacements:
    if old in content:
        content = content.replace(old, new)
        count += 1
        print(f"OK: Pattern {count}")
    else:
        print(f"NOT FOUND: Pattern {count + 1}")

print(f"\nTotal replacements: {count}/{len(replacements)}")

# 保存
with open('gui.js', 'w', encoding='utf-8') as f:
    f.write(content)

print("Done!")

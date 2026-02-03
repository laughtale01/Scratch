#!/bin/bash
# laughtale-scratch 専用デプロイスクリプト
# このプロジェクトでは常に laughtale.education@gmail.com を使用

ACCOUNT="laughtale.education@gmail.com"
PROJECT="laughtale-scratch-bcc8a"

echo "=== laughtale-scratch デプロイ ==="
echo "アカウント: $ACCOUNT"
echo "プロジェクト: $PROJECT"
echo ""

# 引数に応じてデプロイ対象を変更
if [ "$1" == "functions" ]; then
    echo ">>> Cloud Functions のみデプロイ"
    firebase deploy --only functions --account "$ACCOUNT" --project "$PROJECT"
elif [ "$1" == "firestore" ]; then
    echo ">>> Firestore ルールのみデプロイ"
    firebase deploy --only firestore:rules --account "$ACCOUNT" --project "$PROJECT"
elif [ "$1" == "all" ]; then
    echo ">>> 全てデプロイ (Functions + Firestore)"
    firebase deploy --account "$ACCOUNT" --project "$PROJECT"
else
    echo "使用方法:"
    echo "  ./deploy.sh functions  - Cloud Functions をデプロイ"
    echo "  ./deploy.sh firestore  - Firestore ルールをデプロイ"
    echo "  ./deploy.sh all        - 全てデプロイ"
    echo ""
    echo ">>> デフォルト: Cloud Functions をデプロイ"
    firebase deploy --only functions --account "$ACCOUNT" --project "$PROJECT"
fi

# Gallery/Auth E2E Ownership & Rollback Agreement

## E2E Execution Owner

- Owner name: 未割当
- Backup owner: 未割当
- Planned execution date (JST): 未設定
- Planned execution environment: laughtale-scratch-bcc8a (production)

## Rollback Owner

- Owner name: 未割当
- Backup owner: 未割当
- Rollback decision authority: 未設定

## Rollback Procedure Agreement

- Target rollback scope:
  - gh-pages (GitHub Pages)
  - functions
  - firestore rules/indexes
  - storage rules
- Rollback trigger conditions:
  - ロール別スモークで重大不具合（ログイン不可/データ不整合/権限逸脱）が再現した場合
- Rollback command set:
  - `git revert <problem-commit>` または `git reset --hard <safe-commit>` は使用せず、`git revert` で `gh-pages` を戻す
  - `git push origin gh-pages`
  - `firebase deploy --project laughtale-scratch-bcc8a --only functions`
  - `firebase deploy --project laughtale-scratch-bcc8a --only "firestore:rules,firestore:indexes,storage"`
- Verification after rollback:
  - `admin.html` でユーザー一覧・教室表示・基本操作のスモークを実施

## Sign-off

- E2E owner sign-off: PENDING
- Rollback owner sign-off: PENDING
- Project lead sign-off: PENDING

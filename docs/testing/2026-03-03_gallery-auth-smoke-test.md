# Gallery/Auth Smoke Test (Manual)

## Preconditions

- Deploy target project: `laughtale-scratch-bcc8a`
- Updated files are deployed:
  - `gallery.html`
  - `firebase-integration.js`
  - `admin.js`
  - `admin-mobile.html`
  - `functions/index.js`
  - `firestore.rules`
  - `storage.rules`
- Test users:
  - `admin` role
  - `teacher` role (with classroom)
  - `student` role (same classroom as teacher)

## 0. Static precheck (local)

1. Run:
   - `powershell -ExecutionPolicy Bypass -File .\scripts\predeploy_gallery_auth.ps1` (recommended dry-run gate)
   - `powershell -ExecutionPolicy Bypass -File .\scripts\predeploy_gallery_auth.ps1 -RequireExecutionArtifacts` (strict dry-run gate with evidence check)
   - `powershell -ExecutionPolicy Bypass -File .\scripts\run_verification_suite.ps1` (recommended)
   - `powershell -ExecutionPolicy Bypass -File .\scripts\run_verification_suite.ps1 -IncludeExecutionArtifacts` (strict verification with evidence check)
   - `powershell -ExecutionPolicy Bypass -File .\scripts\generate_release_readiness_report.ps1` (readiness summary report)
   - `powershell -ExecutionPolicy Bypass -File .\scripts\verify_gallery_auth_static.ps1`
   - (optional single check) `powershell -ExecutionPolicy Bypass -File .\scripts\verify_firestore_indexes.ps1`
   - (optional single check) `powershell -ExecutionPolicy Bypass -File .\scripts\verify_firebase_config_wiring.ps1`
   - (optional single check) `powershell -ExecutionPolicy Bypass -File .\scripts\verify_rules_contract.ps1`
   - (optional single check) `powershell -ExecutionPolicy Bypass -File .\scripts\verify_powershell_scripts_syntax.ps1`
   - (optional single check) `powershell -ExecutionPolicy Bypass -File .\scripts\verify_html_id_uniqueness.ps1`
2. Confirm all checks are `[OK]` and final line is `Static verification: PASSED`.

Expected:

- No inline event attributes remain in target files.
- No `.onclick` / `.on...` DOM property assignments remain in target scripts.
- Login normalization helpers are present and used (`normalizeUserId` / `buildLoginEmail` / `userIdToEmail`).
- No raw `toLowerCase() + '@laughtale.local'` style concatenation remains in login scripts.
- Required Firestore composite indexes are defined in `firestore.indexes.json`.
- `firebase.json` correctly wires Firestore rules/indexes and Storage rules files.
- Firestore/Storage rules include required guard contracts for project update immutability and storage ownership scope.
- `scripts/*.ps1` has no parse-level syntax errors.
- `gallery.html` / `admin.html` / `admin-mobile.html` have no duplicate `id` values.
- JS syntax checks pass for admin/integration/functions files.
- `firebase.json` and `firestore.indexes.json` are valid JSON.
- Verification suite can emit a timestamped log under `docs/testing/logs/`.
- Predeploy dry-run command prints the exact deploy target set for `gallery-auth` scope.
- Strict gate fails when execution artifacts are unfilled.
- Readiness report is generated under `docs/testing/executions/release-readiness_*.md` and marks overall `GO/NO-GO`.

## 1. Admin flow

1. Login as admin from `index.html`.
1. Retry login with uppercase userId input (e.g. `TANAKA123`) using same account.
2. Open gallery from top menu.
3. Open `admin.html` dashboard and run `欠損ユーザー監査を実行`.
4. Confirm audit result summary appears in dashboard card.
3. Confirm submitted projects are listed.
4. Confirm classroom filter and sort work.
5. Open one project preview.
6. Click `Scratchで開く`.
7. Confirm new tab auto-loads the project.
8. Logout and confirm gallery state is cleared.

Expected:

- No permission error on gallery load.
- Login works regardless of userId letter case.
- Admin dashboard can run coverage audit and show missing-user count.
- Auto-load works once and URL param is removed.
- No previous user data remains after logout.

## 2. Teacher flow

1. Login as teacher.
2. Open gallery.
3. Confirm only own classroom submitted projects are shown.
4. Open preview and use `Scratchで開く`.

Expected:

- Projects from other classrooms are not visible.
- Open-in-Scratch works.

## 3. Student flow

1. Login as student.
2. Open gallery.
3. Confirm only own submitted projects are shown.
4. Submit/unsubmit from cloud modal and refresh gallery.

Expected:

- Visibility is limited to own submitted projects.
- Submit/unsubmit reflects in gallery list.

## 4. Admin classroom delete guard

1. Login as admin and open `admin.html`.
2. Try deleting a classroom that has students.
3. Try deleting a classroom that has projects.
4. Try deleting a classroom that has teacher/admin linked.

Expected:

- Delete is blocked with explicit reason in each case.

## 5. Functions validation checks

1. Call `createUser` with uppercase userId.
2. Call `createUsers` with:
   - invalid role
   - short password
   - missing classroom for non-admin
3. Call `saveVideoMetadata` with filePath not under `recordings/{callerUid}/`.
4. Call `getPublicVideos?limit=9999`.
5. Call `auditUserDocumentCoverage` as admin:
   - `maxUsers=2000`
   - `onlyLocalDomain=true`
   - `sampleSize=50`
6. Record coverage audit summary in:
   - `docs/testing/2026-03-03_user-coverage-audit-result-template.md`

Expected:

- `createUser/createUsers` enforce validation and normalize userId.
- `saveVideoMetadata` rejects invalid ownership (`permission-denied`).
- `getPublicVideos` is clamped (max 50 records).
- `auditUserDocumentCoverage` returns `totals.missingUserDocs` and `sampleMissing`.

## 6. Security sanity (quick)

1. In admin filter names, use special chars in display name/classroom name (`< > ' "`).
2. Reload admin pages.
3. Open mobile admin list view.

Expected:

- No script execution.
- Names render as plain text.

## 7. Admin desktop event wiring regression

1. Open `admin.html` (desktop width).
2. Open/close sidebar from:
   - mobile menu icon
   - sidebar overlay click
3. Switch tabs from:
   - sidebar menu
   - top tab buttons
4. In each modal (`create/edit user`, `reset password`, `create/edit classroom`, `project detail`, `confirm delete`):
   - open modal
   - close via `×`
   - close via cancel/close button
5. Execute action buttons:
   - `+ 新規ユーザー`
   - `+ 新規教室`
   - submit/save buttons in each modal
6. Verify filters trigger list updates:
   - user role/classroom/search
   - project classroom/user/status/search

Expected:

- All controls work with no inline handler dependency.
- No `.onclick = ...` assignment remains in key scripts (`gallery/admin/firebase-integration`).
- Console has no `is not defined` / `onclick` related error.
- Tab state, modal close, and filter behavior remain unchanged.

## 8. Fault injection

1. Execute:
   - `docs/testing/2026-03-03_gallery-auth-fault-injection-test.md`
2. Record results in:
   - `docs/testing/2026-03-03_gallery-auth-fault-injection-results-template.md`

Expected:

- Error handling paths are user-readable and recoverable via retry.
- No stale user/session data leaks across auth transitions.

## 9. E2E ownership and rollback agreement

1. Fill:
   - `docs/testing/2026-03-03_gallery-auth-e2e-ownership-template.md`
2. Confirm sign-off before production deploy.

Expected:

- E2E owner, schedule, and rollback owner are explicitly agreed.

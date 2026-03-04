# Gallery/Auth Smoke Test Results (Template)

## Meta

- Date:
- Tester:
- Environment:
- Deployed commit/hash:
- Verification log file:

## 0. Static precheck

- [ ] `scripts/verify_gallery_auth_static.ps1` passed
- [ ] `scripts/run_verification_suite.ps1` passed
- [ ] `scripts/predeploy_gallery_auth.ps1` dry-run passed
- [ ] `scripts/verify_firestore_indexes.ps1` passed (or included via static script)
- [ ] `scripts/verify_firebase_config_wiring.ps1` passed (or included via static script)
- [ ] `scripts/verify_rules_contract.ps1` passed (or included via static script)
- [ ] `scripts/verify_powershell_scripts_syntax.ps1` passed (or included via static script)
- [ ] `scripts/verify_html_id_uniqueness.ps1` passed (or included via static script)
- Notes:

## 1. Admin flow

- [ ] Login (admin)
- [ ] Admin dashboard coverage audit run
- [ ] Gallery load
- [ ] Filter/sort
- [ ] Preview open
- [ ] `Scratchで開く` auto-load
- [ ] Logout clears state
- Notes:

## 2. Teacher flow

- [ ] Login (teacher)
- [ ] Gallery shows only own classroom submitted projects
- [ ] `Scratchで開く` works
- Notes:

## 3. Student flow

- [ ] Login (student)
- [ ] Gallery shows only own submitted projects
- [ ] submit/unsubmit reflects in gallery
- Notes:

## 4. Admin classroom delete guard

- [ ] blocked when students exist
- [ ] blocked when projects exist
- [ ] blocked when teacher/admin linked
- Notes:

## 5. Functions validation

- [ ] `createUser` userId normalization
- [ ] `createUsers` invalid role rejected
- [ ] `createUsers` short password rejected
- [ ] `createUsers` missing classroom rejected
- [ ] `saveVideoMetadata` ownership check rejected
- [ ] `getPublicVideos` limit clamped
- [ ] `auditUserDocumentCoverage` returned missing-user metrics
- Notes:

## 6. Security sanity

- [ ] special chars render as plain text
- [ ] no unexpected script execution
- Notes:

## Final judgement

- Result: PASS / FAIL
- Blocking issues:
- Follow-up tasks:

# Proposal: Add SCC Task and Scan Schema i18n Support

## Change ID
`add-scc-i18n`

## Summary
Add missing internationalization (i18n) entries for SCC (Source Code Check) task and scan schema related actions in the auth module's i18n files.

## Why
The `getRedirectInformation` method in `RbacPermissionApplyService` uses i18n to display action names for various resource types. Currently, SCC task and SCC scan schema related actions are missing from the i18n files, which could cause display issues or fallback to key names when users interact with SCC-related permissions. This creates an inconsistent user experience across different locales.

## What Changes

### Missing i18n Keys
The following 12 action keys are missing from the i18n files under `support-files/i18n/auth/`:

**SCC Scan Schema (2 actions)**:
- `scc_scan_schema_create`
- `scc_scan_schema_list`

**SCC Task (10 actions)**:
- `scc_task_create`
- `scc_task_delete`
- `scc_task_edit`
- `scc_task_enable`
- `scc_task_execute`
- `scc_task_list`
- `scc_task_manage`
- `scc_task_manage-defect`
- `scc_task_view`
- `scc_task_view-defect`

### Proposed Changes
Add the missing i18n entries to all three language files:
- `support-files/i18n/auth/message_zh_CN.properties` (Simplified Chinese)
- `support-files/i18n/auth/message_en_US.properties` (English)
- `support-files/i18n/auth/message_ja_JP.properties` (Japanese)

The translations will follow the existing patterns for similar resources (e.g., `codecc_task_*`, `rule_*`).

### Files Modified
- `support-files/i18n/auth/message_zh_CN.properties` (+12 lines)
- `support-files/i18n/auth/message_en_US.properties` (+12 lines)
- `support-files/i18n/auth/message_ja_JP.properties` (+12 lines)

## Affected Components
- **i18n Files**: `support-files/i18n/auth/message_*.properties`
- **Auth Module**: Uses these i18n keys in `RbacPermissionApplyService.getRedirectInformation()`

## Scope
This is a localization enhancement that does not change any business logic or API behavior. It only adds missing translation entries.

## Dependencies
None. This change is self-contained.

## Risks
- **Low Risk**: Only adding missing i18n entries
- No breaking changes
- No code logic changes

## Alternatives Considered
None. This is a straightforward i18n completion task.

## Open Questions
None.

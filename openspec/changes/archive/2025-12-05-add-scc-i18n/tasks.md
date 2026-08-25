# Tasks: Add SCC Task and Scan Schema i18n Support

## Overview
Add missing i18n entries for SCC task and scan schema actions.

## Task List

### 1. Add i18n entries to Chinese language file
- **File**: `support-files/i18n/auth/message_zh_CN.properties`
- **Location**: After `rule_list.actionName` (around line 178)
- **Entries to add**:
  - `scc_scan_schema_create.actionName=SCC扫描方案创建`
  - `scc_scan_schema_list.actionName=SCC扫描方案列表`
  - `scc_task_create.actionName=创建SCC任务`
  - `scc_task_delete.actionName=删除SCC任务`
  - `scc_task_edit.actionName=编辑SCC任务`
  - `scc_task_enable.actionName=禁用/启用SCC任务`
  - `scc_task_execute.actionName=执行SCC任务`
  - `scc_task_list.actionName=SCC任务列表`
  - `scc_task_manage.actionName=SCC任务权限管理`
  - `scc_task_manage-defect.actionName=管理SCC任务告警`
  - `scc_task_view.actionName=查看SCC任务`
  - `scc_task_view-defect.actionName=SCC任务告警查看`

### 2. Add i18n entries to English language file
- **File**: `support-files/i18n/auth/message_en_US.properties`
- **Location**: After `rule_list.actionName` (around line 177)
- **Entries to add**:
  - `scc_scan_schema_create.actionName=SCC Scan Schema Create`
  - `scc_scan_schema_list.actionName=SCC Scan Schema List`
  - `scc_task_create.actionName=SCC Task Create`
  - `scc_task_delete.actionName=SCC Task Delete`
  - `scc_task_edit.actionName=SCC Task Edit`
  - `scc_task_enable.actionName=SCC Task Enable`
  - `scc_task_execute.actionName=SCC Task Execute`
  - `scc_task_list.actionName=SCC Task List`
  - `scc_task_manage.actionName=SCC Task Manage`
  - `scc_task_manage-defect.actionName=Manage SCC Task Defect`
  - `scc_task_view.actionName=SCC Task View`
  - `scc_task_view-defect.actionName=View SCC Task Defect`

### 3. Add i18n entries to Japanese language file
- **File**: `support-files/i18n/auth/message_ja_JP.properties`
- **Location**: After `rule_list.actionName` (around line 178)
- **Entries to add**:
  - `scc_scan_schema_create.actionName=SCC スキャン方案作成`
  - `scc_scan_schema_list.actionName=SCC スキャン方案リスト`
  - `scc_task_create.actionName=SCC タスクを作成`
  - `scc_task_delete.actionName=SCC タスクを削除`
  - `scc_task_edit.actionName=SCC タスクを編集`
  - `scc_task_enable.actionName=SCC タスクを無効化/有効化`
  - `scc_task_execute.actionName=SCC タスクを実行`
  - `scc_task_list.actionName=SCC タスクリスト`
  - `scc_task_manage.actionName=SCC タスク権限管理`
  - `scc_task_manage-defect.actionName=SCC タスクアラートを管理`
  - `scc_task_view.actionName=SCC タスクを閲覧`
  - `scc_task_view-defect.actionName=SCC タスクアラート閲覧`

### 4. Validation
- **Action**: Verify entries are properly formatted
- **Checks**:
  - All keys follow the pattern `{resource}_{action}.actionName`
  - Chinese translations are natural and consistent with existing entries
  - English translations use title case
  - Japanese translations use appropriate kanji/katakana
  - No duplicate keys
  - Proper line endings and encoding (UTF-8)

## Dependencies
- None (all tasks can be completed in parallel)

## Verification
- Review the diff to ensure all 12 entries × 3 languages = 36 lines are added correctly
- Verify alphabetical/logical ordering is maintained
- Check that formatting matches existing entries (spacing, etc.)

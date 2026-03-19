# Spec: Auth Module i18n

## ADDED Requirements

### Requirement: SCC Scan Schema i18n Support
**ID**: `auth-i18n-scc-scan-schema`  
**Priority**: Medium  
**Status**: Proposed

The auth module SHALL support internationalization for SCC scan schema related actions.

#### Scenario: Display SCC scan schema action names in Chinese
**Given** a user with Chinese locale setting  
**When** the system displays SCC scan schema permission information  
**Then** the following action names SHALL be displayed in Chinese:
- "SCC扫描方案创建" for `scc_scan_schema_create`
- "SCC扫描方案列表" for `scc_scan_schema_list`

#### Scenario: Display SCC scan schema action names in English
**Given** a user with English locale setting  
**When** the system displays SCC scan schema permission information  
**Then** the following action names SHALL be displayed in English:
- "SCC Scan Schema Create" for `scc_scan_schema_create`
- "SCC Scan Schema List" for `scc_scan_schema_list`

#### Scenario: Display SCC scan schema action names in Japanese
**Given** a user with Japanese locale setting  
**When** the system displays SCC scan schema permission information  
**Then** the following action names SHALL be displayed in Japanese:
- "SCC スキャン方案作成" for `scc_scan_schema_create`
- "SCC スキャン方案リスト" for `scc_scan_schema_list`

---

### Requirement: SCC Task i18n Support
**ID**: `auth-i18n-scc-task`  
**Priority**: Medium  
**Status**: Proposed

The auth module SHALL support internationalization for SCC task related actions.

#### Scenario: Display SCC task CRUD action names in Chinese
**Given** a user with Chinese locale setting  
**When** the system displays SCC task permission information  
**Then** the following action names SHALL be displayed in Chinese:
- "创建SCC任务" for `scc_task_create`
- "删除SCC任务" for `scc_task_delete`
- "编辑SCC任务" for `scc_task_edit`
- "禁用/启用SCC任务" for `scc_task_enable`

#### Scenario: Display SCC task execution action names in Chinese
**Given** a user with Chinese locale setting  
**When** the system displays SCC task execution permissions  
**Then** the following action names SHALL be displayed in Chinese:
- "执行SCC任务" for `scc_task_execute`
- "SCC任务列表" for `scc_task_list`

#### Scenario: Display SCC task management action names in Chinese
**Given** a user with Chinese locale setting  
**When** the system displays SCC task management permissions  
**Then** the following action names SHALL be displayed in Chinese:
- "SCC任务权限管理" for `scc_task_manage`
- "查看SCC任务" for `scc_task_view`

#### Scenario: Display SCC task defect action names in Chinese
**Given** a user with Chinese locale setting  
**When** the system displays SCC task defect permissions  
**Then** the following action names SHALL be displayed in Chinese:
- "管理SCC任务告警" for `scc_task_manage-defect`
- "SCC任务告警查看" for `scc_task_view-defect`

#### Scenario: Display SCC task action names in English
**Given** a user with English locale setting  
**When** the system displays SCC task permission information  
**Then** action names SHALL be displayed using title case English:
- "SCC Task Create", "SCC Task Delete", "SCC Task Edit", "SCC Task Enable"
- "SCC Task Execute", "SCC Task List", "SCC Task Manage", "SCC Task View"
- "Manage SCC Task Defect", "View SCC Task Defect"

#### Scenario: Display SCC task action names in Japanese
**Given** a user with Japanese locale setting  
**When** the system displays SCC task permission information  
**Then** action names SHALL be displayed in Japanese:
- "SCC タスクを作成", "SCC タスクを削除", "SCC タスクを編集", "SCC タスクを無効化/有効化"
- "SCC タスクを実行", "SCC タスクリスト", "SCC タスク権限管理", "SCC タスクを閲覧"
- "SCC タスクアラートを管理", "SCC タスクアラート閲覧"

---

### Requirement: i18n File Consistency
**ID**: `auth-i18n-consistency`  
**Priority**: High  
**Status**: Proposed

All i18n property files SHALL maintain consistent key naming and formatting.

#### Scenario: Consistent key naming pattern
**Given** new action i18n keys are added  
**When** the keys are defined  
**Then** they SHALL follow the pattern `{resourceType}_{actionName}.actionName`  
**And** action names SHALL use underscores for word separation in keys  
**And** display values SHALL use natural language appropriate for each locale

#### Scenario: Alphabetical ordering within categories
**Given** i18n entries are added to property files  
**When** entries are inserted  
**Then** they SHALL be placed in logical grouping (e.g., after `rule_*` entries, before `env_node_*` entries)  
**And** maintain alphabetical order within resource type groups

#### Scenario: UTF-8 encoding for all languages
**Given** i18n files contain multi-language text  
**When** files are saved  
**Then** they SHALL use UTF-8 encoding  
**And** special characters SHALL be properly rendered in Chinese, Japanese, and English

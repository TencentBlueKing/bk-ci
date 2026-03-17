# 规格说明：Auth 模块国际化

## 新增需求（ADDED Requirements）

### 需求：SCC 扫描方案国际化支持
**需求 ID**：`auth-i18n-scc-scan-schema`  
**优先级**：中  
**状态**：提议中（Proposed）

Auth 模块**必须**支持 SCC 扫描方案相关操作的国际化。

#### 场景：在中文环境显示 SCC 扫描方案操作名称
**假设**（Given）用户的语言设置为简体中文  
**当**（When）系统显示 SCC 扫描方案权限信息时  
**那么**（Then）以下操作名称**必须**以中文显示：
- `scc_scan_schema_create` 显示为 "SCC扫描方案创建"
- `scc_scan_schema_list` 显示为 "SCC扫描方案列表"

#### 场景：在英文环境显示 SCC 扫描方案操作名称
**假设**（Given）用户的语言设置为英文  
**当**（When）系统显示 SCC 扫描方案权限信息时  
**那么**（Then）以下操作名称**必须**以英文显示：
- `scc_scan_schema_create` 显示为 "SCC Scan Schema Create"
- `scc_scan_schema_list` 显示为 "SCC Scan Schema List"

#### 场景：在日文环境显示 SCC 扫描方案操作名称
**假设**（Given）用户的语言设置为日文  
**当**（When）系统显示 SCC 扫描方案权限信息时  
**那么**（Then）以下操作名称**必须**以日文显示：
- `scc_scan_schema_create` 显示为 "SCC スキャン方案作成"
- `scc_scan_schema_list` 显示为 "SCC スキャン方案リスト"

---

### 需求：SCC 任务国际化支持
**需求 ID**：`auth-i18n-scc-task`  
**优先级**：中  
**状态**：提议中（Proposed）

Auth 模块**必须**支持 SCC 任务相关操作的国际化。

#### 场景：在中文环境显示 SCC 任务 CRUD 操作名称
**假设**（Given）用户的语言设置为简体中文  
**当**（When）系统显示 SCC 任务权限信息时  
**那么**（Then）以下操作名称**必须**以中文显示：
- `scc_task_create` 显示为 "创建SCC任务"
- `scc_task_delete` 显示为 "删除SCC任务"
- `scc_task_edit` 显示为 "编辑SCC任务"
- `scc_task_enable` 显示为 "禁用/启用SCC任务"

#### 场景：在中文环境显示 SCC 任务执行操作名称
**假设**（Given）用户的语言设置为简体中文  
**当**（When）系统显示 SCC 任务执行权限时  
**那么**（Then）以下操作名称**必须**以中文显示：
- `scc_task_execute` 显示为 "执行SCC任务"
- `scc_task_list` 显示为 "SCC任务列表"

#### 场景：在中文环境显示 SCC 任务管理操作名称
**假设**（Given）用户的语言设置为简体中文  
**当**（When）系统显示 SCC 任务管理权限时  
**那么**（Then）以下操作名称**必须**以中文显示：
- `scc_task_manage` 显示为 "SCC任务权限管理"
- `scc_task_view` 显示为 "查看SCC任务"

#### 场景：在中文环境显示 SCC 任务告警操作名称
**假设**（Given）用户的语言设置为简体中文  
**当**（When）系统显示 SCC 任务告警权限时  
**那么**（Then）以下操作名称**必须**以中文显示：
- `scc_task_manage-defect` 显示为 "管理SCC任务告警"
- `scc_task_view-defect` 显示为 "SCC任务告警查看"

#### 场景：在英文环境显示 SCC 任务操作名称
**假设**（Given）用户的语言设置为英文  
**当**（When）系统显示 SCC 任务权限信息时  
**那么**（Then）操作名称**必须**使用标题大小写（Title Case）的英文显示：
- `scc_task_create` → "SCC Task Create"
- `scc_task_delete` → "SCC Task Delete"
- `scc_task_edit` → "SCC Task Edit"
- `scc_task_enable` → "SCC Task Enable"
- `scc_task_execute` → "SCC Task Execute"
- `scc_task_list` → "SCC Task List"
- `scc_task_manage` → "SCC Task Manage"
- `scc_task_view` → "SCC Task View"
- `scc_task_manage-defect` → "Manage SCC Task Defect"
- `scc_task_view-defect` → "View SCC Task Defect"

#### 场景：在日文环境显示 SCC 任务操作名称
**假设**（Given）用户的语言设置为日文  
**当**（When）系统显示 SCC 任务权限信息时  
**那么**（Then）操作名称**必须**以日文显示：
- `scc_task_create` → "SCC タスクを作成"
- `scc_task_delete` → "SCC タスクを削除"
- `scc_task_edit` → "SCC タスクを編集"
- `scc_task_enable` → "SCC タスクを無効化/有効化"
- `scc_task_execute` → "SCC タスクを実行"
- `scc_task_list` → "SCC タスクリスト"
- `scc_task_manage` → "SCC タスク権限管理"
- `scc_task_view` → "SCC タスクを閲覧"
- `scc_task_manage-defect` → "SCC タスクアラートを管理"
- `scc_task_view-defect` → "SCC タスクアラート閲覧"

---

### 需求：国际化文件一致性
**需求 ID**：`auth-i18n-consistency`  
**优先级**：高  
**状态**：提议中（Proposed）

所有国际化属性文件**必须**保持一致的键命名和格式。

#### 场景：一致的键命名模式
**假设**（Given）添加新的操作国际化键  
**当**（When）定义这些键时  
**那么**（Then）它们**必须**遵循 `{resourceType}_{actionName}.actionName` 模式  
**并且**（And）操作名称在键中**必须**使用下划线分隔单词  
**并且**（And）显示值**必须**使用适合各语言环境的自然语言

#### 场景：类别内的字母顺序排列
**假设**（Given）国际化条目被添加到属性文件中  
**当**（When）插入条目时  
**那么**（Then）它们**必须**放置在逻辑分组中（例如，在 `rule_*` 条目之后，`env_node_*` 条目之前）  
**并且**（And）在资源类型组内保持字母顺序

#### 场景：所有语言使用 UTF-8 编码
**假设**（Given）国际化文件包含多语言文本  
**当**（When）保存文件时  
**那么**（Then）它们**必须**使用 UTF-8 编码  
**并且**（And）特殊字符**必须**在中文、日文和英文中正确渲染

#### 场景：翻译质量保证
**假设**（Given）为操作添加国际化翻译  
**当**（When）编写翻译内容时  
**那么**（Then）翻译**必须**：
- 准确反映操作的含义
- 使用该语言的自然表达方式
- 与同类资源的翻译风格保持一致
- 避免使用机器翻译的生硬表达

**例如**（Examples）：
- ✅ 好的中文："创建SCC任务"（自然、简洁）
- ❌ 不好的中文："SCC任务创建"（受英文语序影响）
- ✅ 好的英文："SCC Task Create"（标题大小写）
- ❌ 不好的英文："Create scc task"（大小写不一致）

---

## 技术约束

### 约束 1：文件位置固定
所有 auth 模块的国际化文件**必须**位于 `support-files/i18n/auth/` 目录下。

### 约束 2：键名不可更改
一旦国际化键被系统使用，其键名**不得**更改，以避免破坏现有功能。

### 约束 3：三语言同步
添加新的国际化键时，**必须**同时在中文、英文、日文三个文件中添加相应条目。

---

## 验收标准

本变更**必须**满足以下条件才能被接受：

1. ✅ **完整性**：12 个操作 × 3 种语言 = 36 条国际化条目全部添加
2. ✅ **正确性**：所有翻译准确无误，符合语言习惯
3. ✅ **一致性**：键名格式、排列顺序与现有条目保持一致
4. ✅ **可测试性**：可通过调用 `getRedirectInformation` API 验证
5. ✅ **可维护性**：条目位置合理，易于后续查找和修改
6. ✅ **编码正确**：UTF-8 编码，无乱码

---

## 参考资料

### 相关代码位置
- **使用方**：`src/backend/ci/core/auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/provider/rbac/service/RbacPermissionApplyService.kt` 第 477-528 行
- **资源类型定义**：`src/backend/ci/core/common/common-auth/common-auth-api/src/main/kotlin/com/tencent/devops/common/auth/api/AuthResourceType.kt` 第 69-70 行

### 现有类似资源
可参考以下已有的国际化条目格式：
- `codecc_task_create.actionName`（CodeCC 任务相关）
- `rule_create.actionName`（质量红线规则相关）
- `pipeline_create.actionName`（流水线相关）

### 国际化机制说明
系统通过 `I18nUtil.getCodeLanMessage()` 方法读取国际化文件，键名格式为：
```kotlin
messageCode = resourceType + AuthI18nConstants.RESOURCE_TYPE_NAME_SUFFIX
// 或
messageCode = "${action}${ACTION_NAME_SUFFIX}"
```

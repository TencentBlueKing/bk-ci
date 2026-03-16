# 提案：添加 SCC 任务和扫描方案的国际化支持

## 变更 ID
`add-scc-i18n`

## 概述
在 auth 模块的国际化文件中添加缺失的 SCC（源代码检查）任务和扫描方案相关操作的国际化条目。

## 背景
`RbacPermissionApplyService` 中的 `getRedirectInformation` 方法使用国际化（i18n）来显示各种资源类型的操作名称。目前，SCC 任务和 SCC 扫描方案相关操作在国际化文件中缺失，这可能在用户与 SCC 相关权限交互时导致显示问题。

## 问题陈述
在 `support-files/i18n/auth/` 目录下的国际化文件中缺失以下操作键：

### SCC 扫描方案相关（2个）
- `scc_scan_schema_create` - SCC扫描方案创建
- `scc_scan_schema_list` - SCC扫描方案列表

### SCC 任务相关（10个）
- `scc_task_create` - 创建SCC任务
- `scc_task_delete` - 删除SCC任务
- `scc_task_edit` - 编辑SCC任务
- `scc_task_enable` - 禁用/启用SCC任务
- `scc_task_execute` - 执行SCC任务
- `scc_task_list` - SCC任务列表
- `scc_task_manage` - SCC任务权限管理
- `scc_task_manage-defect` - 管理SCC任务告警
- `scc_task_view` - 查看SCC任务
- `scc_task_view-defect` - SCC任务告警查看

## 建议方案
在所有三个语言文件中添加缺失的国际化条目：
- `message_zh_CN.properties`（简体中文）
- `message_en_US.properties`（英文）
- `message_ja_JP.properties`（日文）

翻译将遵循类似资源的现有模式（例如 `codecc_task_*`、`rule_*`）。

## 受影响的组件
- **国际化文件**：`support-files/i18n/auth/message_*.properties`
- **Auth 模块**：在 `RbacPermissionApplyService.getRedirectInformation()` 中使用这些国际化键

## 范围
这是一个本地化增强，不改变任何业务逻辑或 API 行为。仅添加缺失的翻译条目。

## 依赖关系
无。此变更是独立的。

## 风险评估
- **风险等级**：低
- 仅添加缺失的国际化条目
- 无破坏性变更
- 无代码逻辑变更

## 考虑的替代方案
无。这是一个直接的国际化完善任务。

## 待解决问题
无。

## 预期收益
- 用户在查看 SCC 相关权限时能看到正确的中文/英文/日文操作名称
- 提升系统的国际化完整性
- 与现有资源类型的国际化保持一致

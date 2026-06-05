## 背景

当前 `codecc_rule_set` 已接入 RBAC 资源类型，但权限粒度仍停留在项目管理员兜底：规则集相关可操作性主要通过
“是否为项目管理员”来判定，无法按具体规则集资源进行授权。这种模型过粗，无法满足将规则集编辑与权限管理下放到
资源拥有者的诉求。

本次需要把 `codecc_rule_set` 补齐为真正可用的资源级权限模型，为后续外部平台按标准鉴权接口调用提供基础。
需求边界已经明确：只新增 `edit` 与 `manage` 两个实例级动作，只保留 `owner` 一个资源级用户组，不在本变更中
实现 CodeCC 业务侧调用改造。

## 变更内容

### 1. 权限模型补齐
- 为 `codecc_rule_set` 新增两个资源实例级操作：
  - `codecc_rule_set_edit`
  - `codecc_rule_set_manage`
- 保留现有项目级入口操作：
  - `codecc_rule_set_create`
  - `codecc_rule_set_list`
- `edit` / `manage` 挂靠在 `codecc_rule_set` 资源维度，而非 `project` 资源维度。

### 2. 资源组与默认组调整
- 为 `codecc_rule_set` 新增资源级“拥有者”用户组（组 code 沿用通用 `manager` 语义）。
- “拥有者”组承载：
  - `codecc_rule_set_edit`
  - `codecc_rule_set_manage`
- `resourceType = project` 的默认管理员组补充：
  - `codecc_rule_set_edit`
  - `codecc_rule_set_manage`
- 不调整 `ci_manager` 默认组。

### 3. 历史数据迁移
- 存量 `codecc_rule_set` 需要补迁移数据，为每个规则集创建“拥有者”组并写入成员。
- 规则集拥有者信息通过回调接口获取，不在本变更中实现业务侧拥有者推导逻辑。
- 项目权限重置统一使用以下参数：
  - `migrateResource = true`
  - `filterResourceTypes = ["codecc_rule_set"]`
- 本次不传 `filterActions`，按“已有资源类型完整补齐资源级权限模型”的方式执行重置。

### 4. 配置与脚本交付
- 补齐 IAM RBAC 配置、初始化 SQL 与国际化文案。
- 除修改 `5001_ci_auth-init_dml_mysql.sql` 外，还需要额外提供一份专项增量 DML 脚本，形式上类似
  `creative_stream_dml.sql`，用于线上单独执行本次权限模型变更。

## 能力变更

### 修改的能力
- `auth-resource-type`：将 `codecc_rule_set` 从“项目管理员兜底”补齐为“支持资源级拥有者授权”的规则集权限模型。

## 影响范围

- 影响模块：`auth` 模块的 RBAC 资源模型、权限迁移与初始化脚本。
- 影响配置：
  - `support-files/bkiam-rbac/0005_action_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0006_group_20221213_iam-rbac.json`
  - `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`
  - `support-files/i18n/auth/message_zh_CN.properties`
  - `support-files/i18n/auth/message_en_US.properties`
  - `support-files/i18n/auth/message_ja_JP.properties`
- 影响迁移链路：
  - `OpAuthMigrateResource#resetProjectPermissions`
  - `PermissionMigrateService` 及其 RBAC 实现
- 影响交付物：需新增一份 `codecc_rule_set` 专项 DML 脚本，用于线上增量执行。
- 不包含内容：不在本变更中改造 CodeCC 或其他平台的业务调用方，只负责提供可被调用的权限模型与迁移基础。

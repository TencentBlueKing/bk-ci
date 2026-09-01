## 1. 权限模型定义

- [x] 1.1 修改 `support-files/bkiam-rbac/0005_action_20221213_iam-rbac.json`，新增
  `codecc_rule_set_edit`
- [x] 1.2 修改 `support-files/bkiam-rbac/0005_action_20221213_iam-rbac.json`，新增
  `codecc_rule_set_manage`
- [x] 1.3 确认两个动作均挂在 `codecc_rule_set` 资源维度，而非 `project`
- [x] 1.4 确认动作依赖关系与现有 `project_visit` / 资源实例选择器设计一致

## 2. 权限分组配置

- [x] 2.1 修改 `support-files/bkiam-rbac/0006_group_20221213_iam-rbac.json`，在 `CodeCC规则集`
  分组下追加 `codecc_rule_set_edit`
- [x] 2.2 修改 `support-files/bkiam-rbac/0006_group_20221213_iam-rbac.json`，在 `CodeCC规则集`
  分组下追加 `codecc_rule_set_manage`
- [x] 2.3 为 `codecc_rule_set` 新增资源级 `owner` 组配置，组语义为 `manager`
- [x] 2.4 明确本次不新增 editor / viewer / executor 组

## 3. 初始化 SQL

- [x] 3.1 修改 `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`，新增
  `codecc_rule_set_edit` 动作记录
- [x] 3.2 修改 `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`，新增
  `codecc_rule_set_manage` 动作记录
- [x] 3.3 修改 `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`，更新
  `resourceType = project` 的管理员默认组权限，追加上述两个动作
- [x] 3.4 修改 `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`，新增
  `codecc_rule_set` 资源级 owner 组配置
- [x] 3.5 本次不新增额外的 project 级迁移组配置；若现有迁移框架技术上必须依赖模板配置，仅补齐创建
  `codecc_rule_set` owner 组所需的最小化模板数据
- [x] 3.6 明确本次不修改 `ci_manager` 相关默认组

## 4. 增量 DML 脚本

- [x] 4.1 新增一份专项 DML 脚本，形式上类似 `creative_stream_dml.sql`
- [x] 4.2 在脚本中补齐 `codecc_rule_set_edit` 与 `codecc_rule_set_manage` 动作数据
- [x] 4.3 在脚本中补齐项目管理员组对上述两个动作的授权
- [x] 4.4 在脚本中补齐 `codecc_rule_set` owner 组配置
- [x] 4.5 确保脚本可幂等执行，并适用于线上增量变更

## 5. 迁移与重置链路

- [x] 5.1 确认 `resetProjectPermissions` 本次使用固定参数：
  - `migrateResource = true`
  - `filterResourceTypes = ["codecc_rule_set"]`
- [x] 5.2 明确本次不传 `filterActions`
- [x] 5.3 明确回调契约：每个 `codecc_rule_set` 返回单个 owner 的 `userId`
- [x] 5.4 在迁移过程中为每个存量规则集创建或修复 owner 组
- [x] 5.5 将回调返回的 owner 写入对应 owner 组成员
- [x] 5.6 对 owner 缺失、owner 非法、回调失败等场景记录失败明细，并保证单个规则集失败不阻断其他资源迁移

## 6. 国际化与对外可见性

- [x] 6.1 修改 `support-files/i18n/auth/message_zh_CN.properties`，新增中文文案
- [x] 6.2 修改 `support-files/i18n/auth/message_en_US.properties`，新增英文文案
- [x] 6.3 修改 `support-files/i18n/auth/message_ja_JP.properties`，新增日文文案
- [x] 6.4 补齐 `codecc_rule_set_edit`、`codecc_rule_set_manage` 与 owner 组相关展示文案

## 7. 验证

- [x] 7.1 验证 IAM JSON 配置格式正确
- [ ] 7.2 验证初始化 SQL 与专项 DML 脚本语法正确且可幂等执行
- [x] 7.3 抽样验证项目管理员组已具备 `codecc_rule_set_edit` / `codecc_rule_set_manage`
- [x] 7.4 抽样验证存量 `codecc_rule_set` 已生成 owner 组且成员来自回调接口
- [x] 7.5 验证本次未误修改 `ci_manager` 默认组
- [x] 7.6 验证本次仅交付权限模型与迁移基础，不包含业务侧鉴权调用改造

## Dependencies

- 任务 1.x 依赖现有 `codecc_rule_set` 资源类型已存在
- 任务 2.x 依赖任务 1.x 完成后的 action 定义
- 任务 3.x 依赖任务 1.x / 2.x 完成后的动作与分组模型
- 任务 4.x 与任务 3.x 同步推进，但内容需与初始化 SQL 保持一致
- 任务 5.x 依赖任务 1.x ~ 4.x 提供完整的资源级权限模型
- 任务 6.x 可与任务 1.x ~ 4.x 并行执行

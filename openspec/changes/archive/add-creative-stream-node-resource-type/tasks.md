## 1. 后端代码变更

- [x] 1.1 在 `AuthResourceType.kt` 枚举中新增 `CREATIVE_STREAM_NODE("creative_stream_node")` 资源类型
- [x] 1.2 在 `ResourceTypeId.kt` 中新增 `const val CREATIVE_STREAM_NODE = "creative_stream_node"` 常量
- [x] 1.3 在 `ActionId.kt` 中新增 `CREATIVE_STREAM_NODE_VIEW` 和 `CREATIVE_STREAM_NODE_EDIT` 常量

## 2. IAM RBAC 配置（按依赖顺序）

- [x] 2.1 修改 `0003_resource_20221223_iam-rbac.json`，新增 creative_stream_node 资源类型定义
- [x] 2.2 修改 `0004_instance-views_20221213_iam-rbac.json`，新增 creative_stream_node_instance 实例选择器
- [x] 2.3 修改 `0005_action_20221213_iam-rbac.json`，新增 2 个创作流节点操作权限
- [x] 2.4 修改 `0006_group_20221213_iam-rbac.json`，在「创作流」分组中追加创作流节点操作
- [x] 2.5 修改 `0007_create-related_20221213_iam-rbac.json`，新增创作流节点创建关联权限

## 3. 数据库初始化脚本

- [x] 3.1 修改 `5001_ci_auth-init_dml_mysql.sql`，新增 T_AUTH_RESOURCE_TYPE 记录 (ID=23)
- [x] 3.2 修改 `5001_ci_auth-init_dml_mysql.sql`，新增 T_AUTH_ACTION 记录（2 条操作）
- [x] 3.3 修改 `5001_ci_auth-init_dml_mysql.sql`，新增 1 个创作流节点资源级组配置 (ID=74, manager)
- [x] 3.4 修改 `5001_ci_auth-init_dml_mysql.sql`，更新项目级组 ID=1 (管理员)，添加 creative_stream_node view + edit 权限
- [x] 3.5 修改 `5001_ci_auth-init_dml_mysql.sql`，更新项目级组 ID=2 (开发人员)，添加 creative_stream_node view 权限
- [x] 3.6 修改 `5001_ci_auth-init_dml_mysql.sql`，更新项目级组 ID=3 (运维人员)，添加 creative_stream_node view 权限

## 4. 增量 DML 脚本

- [x] 4.1 创建 `specs/auth-resource-type/creative_stream_node_dml.sql`，编写内部线上增量变更脚本

## 5. 国际化支持

- [x] 5.1 修改 `support-files/i18n/auth/message_zh_CN.properties`，新增中文翻译
- [x] 5.2 修改 `support-files/i18n/auth/message_en_US.properties`，新增英文翻译
- [x] 5.3 修改 `support-files/i18n/auth/message_ja_JP.properties`，新增日文翻译

## 6. 验证

- [x] 6.1 验证 JSON 文件格式正确性
- [x] 6.2 验证 SQL 脚本语法正确性
- [x] 6.3 编译后端代码确保无错误

## Dependencies

- 任务 2.x 必须按顺序执行（0003 → 0004 → 0005 → 0006 → 0007）
- 任务 3.x 依赖任务 2.x 完成后的操作 ID 定义
- 任务 5.x 可与其他任务并行执行
- 本变更依赖 `2025-12-16-add-creative-stream-resource-type` 已完成（creative_stream 已接入）

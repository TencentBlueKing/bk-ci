## 1. 后端代码变更

- [x] 1.1 在 `AuthResourceType.kt` 枚举中新增 `CREATIVE_STREAM("creative_stream")` 资源类型

## 2. IAM RBAC 配置（按依赖顺序）

- [x] 2.1 修改 `0003_resource_20221223_iam-rbac.json`，新增 creative_stream 资源类型定义
- [x] 2.2 修改 `0004_instance-views_20221213_iam-rbac.json`，新增 creative_stream_instance 实例选择器
- [x] 2.3 修改 `0005_action_20221213_iam-rbac.json`，新增 10 个创作流操作权限
- [x] 2.4 修改 `0006_group_20221213_iam-rbac.json`，新增「创作流」权限分组
- [x] 2.5 修改 `0007_create-related_20221213_iam-rbac.json`，新增创作流创建关联权限

## 3. 数据库初始化脚本

- [x] 3.1 修改 `5001_ci_auth-init_dml_mysql.sql`，新增 T_AUTH_RESOURCE_TYPE 记录
- [x] 3.2 修改 `5001_ci_auth-init_dml_mysql.sql`，新增 T_AUTH_ACTION 记录（10 条操作）
- [x] 3.3 修改 `5001_ci_auth-init_dml_mysql.sql`，调整项目级组（ID 1-7）的 AUTHORIZATION_SCOPES，添加 creative_stream 相关权限
- [x] 3.4 修改 `5001_ci_auth-init_dml_mysql.sql`，新增 4 个创作流资源级组配置（manager/editor/executor/viewer）

## 4. 国际化支持

- [x] 4.1 修改 `support-files/i18n/auth/message_zh_CN.properties`，新增中文翻译
- [x] 4.2 修改 `support-files/i18n/auth/message_en_US.properties`，新增英文翻译
- [x] 4.3 修改 `support-files/i18n/auth/message_ja_JP.properties`，新增日文翻译

## 5. 验证

- [x] 5.1 验证 JSON 文件格式正确性
- [x] 5.2 验证 SQL 脚本语法正确性
- [x] 5.3 编译后端代码确保无错误

## Dependencies

- 任务 2.x 必须按顺序执行（0003 → 0004 → 0005 → 0006 → 0007）
- 任务 3.x 依赖任务 2.x 完成后的操作 ID 定义
- 任务 4.x 可与其他任务并行执行

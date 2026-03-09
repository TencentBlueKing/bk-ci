# Change: 新增 Creative Stream 资源类型接入权限中心

## Why

业务需要新增「创作流」(Creative Stream) 资源类型，用于管理创作流相关的权限控制。创作流的权限模型与流水线 (Pipeline) 类似，包括创建、查看、编辑、删除、执行等操作。

**与流水线的区别**: 创作流没有"组"的概念，即不需要 `creative_stream_group` 资源类型（流水线有 `pipeline` 和 `pipeline_group` 两个资源类型）。

## What Changes

### 1. 后端代码变更
- **AuthResourceType 枚举**: 新增 `CREATIVE_STREAM("creative_stream")` 资源类型
- **AuthPermission 映射**: 复用现有权限操作（create/view/edit/delete/execute/list/share/download/manage）

### 2. IAM RBAC 配置变更（按依赖顺序）
- **0003_resource**: 新增 `creative_stream` 资源类型定义
- **0004_instance-views**: 新增 `creative_stream_instance` 实例选择器
- **0005_action**: 新增创作流相关的 10 个操作权限
  - `creative_stream_create` - 创建创作流
  - `creative_stream_view` - 查看创作流
  - `creative_stream_edit` - 编辑创作流
  - `creative_stream_delete` - 删除创作流
  - `creative_stream_execute` - 执行创作流
  - `creative_stream_list` - 创作流列表
  - `creative_stream_share` - 分享创作流
  - `creative_stream_download` - 下载创作流
  - `creative_stream_manage` - 管理创作流
  - `creative_stream_archive` - 归档创作流
- **0006_group**: 新增「创作流」权限分组
- **0007_create-related**: 新增创作流创建关联权限

### 3. 数据库初始化脚本
- **T_AUTH_RESOURCE_TYPE**: 新增 `creative_stream` 资源类型记录
- **T_AUTH_ACTION**: 新增 10 条创作流操作记录

### 4. 国际化文件
- 新增创作流相关的中英日文翻译

## Impact

- **Affected specs**: auth-resource-type (新增)
- **Affected code**:
  - `src/backend/ci/core/common/common-auth/common-auth-api/src/main/kotlin/com/tencent/devops/common/auth/api/AuthResourceType.kt`
  - `support-files/bkiam-rbac/0003_resource_20221223_iam-rbac.json`
  - `support-files/bkiam-rbac/0004_instance-views_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0005_action_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0006_group_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0007_create-related_20221213_iam-rbac.json`
  - `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`
  - `support-files/i18n/auth/message_zh_CN.properties`
  - `support-files/i18n/auth/message_en_US.properties`
  - `support-files/i18n/auth/message_ja_JP.properties`

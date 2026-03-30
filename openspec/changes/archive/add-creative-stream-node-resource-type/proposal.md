# Change: 新增 Creative Stream Node 资源类型接入权限中心

## Why

业务需要新增「创作流节点」(Creative Stream Node) 资源类型，用于管理创作流节点相关的权限控制。创作流节点的权限模型较为简单，仅包含查看和编辑两个操作，资源级别仅有拥有者一个用户组。

项目级别用户组对创作流节点的权限分配参考环境节点 (env_node) 的模式。

## What Changes

### 1. 后端代码变更
- **AuthResourceType 枚举**: 新增 `CREATIVE_STREAM_NODE("creative_stream_node")` 资源类型
- **ResourceTypeId 常量**: 新增 `CREATIVE_STREAM_NODE = "creative_stream_node"`
- **ActionId 常量**: 新增 `CREATIVE_STREAM_NODE_VIEW` 和 `CREATIVE_STREAM_NODE_EDIT`

### 2. IAM RBAC 配置变更（按依赖顺序）
- **0003_resource**: 新增 `creative_stream_node` 资源类型定义
- **0004_instance-views**: 新增 `creative_stream_node_instance` 实例选择器
- **0005_action**: 新增创作流节点相关的 2 个操作权限
  - `creative_stream_node_view` - 查看创作流节点
  - `creative_stream_node_edit` - 编辑创作流节点
- **0006_group**: 新增独立的「创作流节点」权限分组，与「创作流」同级
- **0007_create-related**: 新增创作流节点创建关联权限

### 3. 数据库初始化脚本
- **T_AUTH_RESOURCE_TYPE**: 新增 `creative_stream_node` 资源类型记录 (ID=23)
- **T_AUTH_ACTION**: 新增 2 条创作流节点操作记录
- **T_AUTH_RESOURCE_GROUP_CONFIG**: 新增 1 个资源级用户组 (manager)
- 更新项目级用户组 (ID 1-7) 的权限，参考 env_node 的分配模式

### 4. 国际化文件
- 新增创作流节点相关的中英日文翻译

## Impact

- **Affected specs**: auth-resource-type (新增)
- **Affected code**:
  - `src/backend/ci/core/common/common-auth/common-auth-api/src/main/kotlin/com/tencent/devops/common/auth/api/AuthResourceType.kt`
  - `src/backend/ci/core/common/common-auth/common-auth-api/src/main/kotlin/com/tencent/devops/common/auth/api/ResourceTypeId.kt`
  - `src/backend/ci/core/common/common-auth/common-auth-api/src/main/kotlin/com/tencent/devops/common/auth/api/ActionId.kt`
  - `support-files/bkiam-rbac/0003_resource_20221223_iam-rbac.json`
  - `support-files/bkiam-rbac/0004_instance-views_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0005_action_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0006_group_20221213_iam-rbac.json`
  - `support-files/bkiam-rbac/0007_create-related_20221213_iam-rbac.json`
  - `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql`
  - `support-files/i18n/auth/message_zh_CN.properties`
  - `support-files/i18n/auth/message_en_US.properties`
  - `support-files/i18n/auth/message_ja_JP.properties`

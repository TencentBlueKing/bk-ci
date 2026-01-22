---
name: permission-model-change-guide
description: IAM RBAC 权限模型变更规则，涵盖资源类型定义、操作权限配置、权限迁移脚本、IAM 回调实现。当用户修改权限模型、添加新资源类型、配置操作权限或编写权限迁移脚本时使用。
---

# IAM 权限中心资源类型接入最佳实践指南

## 概述

本指南基于 `creative_stream` 资源类型接入权限中心的实践经验总结，提供一套可复用的标准流程，帮助团队成员快速完成新资源类型的权限接入。

## 接入流程总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         IAM 资源类型接入流程                                   │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────────────────┐  │
│  │ 1.需求   │───▶│ 2.后端   │───▶│ 3.IAM    │───▶│ 4.数据库配置         │  │
│  │   分析   │    │   枚举   │    │   配置   │    │  (SQL 或 API 二选一) │  │
│  └──────────┘    └──────────┘    └──────────┘    └──────────────────────┘  │
│                                                                             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐                              │
│  │ 5.国际化 │───▶│ 6.用户组 │───▶│ 7.验证   │                              │
│  │   配置   │    │   配置   │    │   测试   │                              │
│  └──────────┘    └──────────┘    └──────────┘                              │
│                                                                             │
│  ⚡ 推荐：使用 API 接口替代 SQL 脚本，更简单、更安全                          │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 第一步：需求分析与规划

### 1.1 确定资源类型信息

| 项目 | 说明 | 示例 |
|------|------|------|
| 资源类型 ID | 全局唯一标识，使用 snake_case | `creative_stream` |
| 中文名称 | 用于 UI 显示 | 创作流 |
| 英文名称 | 用于 UI 显示和日志 | Creative Stream |
| 父资源 | 通常为 `project` | project |

### 1.2 确定权限操作列表

参考已有资源类型（如 `pipeline`）设计操作列表：

| 操作类型 | 命名规范 | 说明 | 关联资源类型 |
|----------|----------|------|--------------|
| create | `{resource}_create` | 创建资源 | **project**（特殊） |
| list | `{resource}_list` | 列表查看 | {resource} |
| view | `{resource}_view` | 查看详情 | {resource} |
| edit | `{resource}_edit` | 编辑资源 | {resource} |
| delete | `{resource}_delete` | 删除资源 | {resource} |
| execute | `{resource}_execute` | 执行资源 | {resource} |
| manage | `{resource}_manage` | 权限管理 | {resource} |
| 其他 | 按需定义 | 如 download、share、archive | {resource} |

> **注意**: `create` 操作的 `related_resource_type` 必须是 `project`，因为创建时资源还不存在。

### 1.3 设计权限依赖关系

```
project_visit (基础权限)
    │
    ├── {resource}_create ──────────────────────────────┐
    │                                                   │
    └── {resource}_list                                 │
            │                                           │
            └── {resource}_view                         │
                    │                                   │
                    ├── {resource}_edit ────────────────┤
                    │       │                           │
                    │       ├── {resource}_manage       │
                    │       └── {resource}_archive      │
                    │                                   │
                    ├── {resource}_delete               │
                    ├── {resource}_execute              │
                    ├── {resource}_download             │
                    └── {resource}_share                │
```

---

## 第二步：后端枚举定义

### 2.1 修改 `AuthResourceType.kt`

**文件路径**: `src/backend/ci/core/common/common-auth/common-auth-api/src/main/kotlin/com/tencent/devops/common/auth/api/AuthResourceType.kt`

```kotlin
enum class AuthResourceType(val value: String) {
    // ... 已有枚举 ...
    
    PIPELINE_DEFAULT("pipeline"),
    PIPELINE_GROUP("pipeline_group"),
    PIPELINE_TEMPLATE("pipeline_template"),
    CREATIVE_STREAM("creative_stream"),  // 新增：创作流类型
    
    // ... 其他枚举 ...
}
```

**命名规范**:
- 枚举名：大写下划线 `CREATIVE_STREAM`
- value：小写下划线 `creative_stream`

---

## 第三步：IAM RBAC 配置文件

需要修改 `support-files/bkiam-rbac/` 目录下的 5 个 JSON 文件：

### 3.1 资源类型定义 (`0003_resource_*.json`)

```json
{
  "operation": "upsert_resource_type",
  "data": {
    "id": "creative_stream",
    "name": "创作流",
    "name_en": "Creative Stream",
    "parents": [
      {
        "system_id": "bk_ci_rbac",
        "id": "project"
      }
    ],
    "provider_config": {
      "path": "/api/open/auth/resource/instances/list?x-devops-project-id=rbac-project"
    },
    "version": 1
  }
}
```

### 3.2 实例选择器 (`0004_instance-views_*.json`)

```json
{
  "operation": "upsert_instance_selection",
  "data": {
    "id": "creative_stream_instance",
    "name": "创作流",
    "name_en": "Creative Stream",
    "resource_type_chain": [
      {
        "system_id": "bk_ci_rbac",
        "id": "project"
      },
      {
        "system_id": "bk_ci_rbac",
        "id": "creative_stream"
      }
    ]
  }
}
```

### 3.3 操作定义 (`0005_action_*.json`)

每个操作需要定义：

```json
{
  "operation": "upsert_action",
  "data": {
    "id": "creative_stream_view",
    "name": "查看创作流",
    "name_en": "Creative Stream View",
    "type": "view",
    "related_resource_types": [
      {
        "system_id": "bk_ci_rbac",
        "id": "creative_stream",
        "selection_mode": "instance",
        "related_instance_selections": [
          {
            "system_id": "bk_ci_rbac",
            "id": "creative_stream_instance"
          }
        ]
      }
    ],
    "related_actions": ["project_visit", "creative_stream_list"],
    "version": 1
  }
}
```

**关键字段说明**:

| 字段 | 说明 |
|------|------|
| `type` | 操作类型：view/edit/delete/create/execute |
| `related_resource_types` | 操作关联的资源类型 |
| `selection_mode` | 选择模式：instance（实例级）/ all（全部） |
| `related_actions` | 依赖的前置操作 |

**create 操作的特殊配置**:

```json
{
  "id": "creative_stream_create",
  "related_resource_types": [
    {
      "system_id": "bk_ci_rbac",
      "id": "project",  // 关联 project 而非 creative_stream
      "selection_mode": "instance",
      "related_instance_selections": [
        {
          "system_id": "bk_ci_rbac",
          "id": "project_instance"
        }
      ]
    }
  ],
  "related_actions": ["project_visit"]
}
```

### 3.4 权限分组 (`0006_group_*.json`)

将所有操作归入一个分组，便于 IAM 界面展示：

```json
{
  "operation": "upsert_action_groups",
  "data": {
    "action_groups": [
      {
        "name": "创作流",
        "name_en": "Creative Stream",
        "actions": [
          {"id": "creative_stream_create"},
          {"id": "creative_stream_list"},
          {"id": "creative_stream_view"},
          {"id": "creative_stream_edit"},
          {"id": "creative_stream_delete"},
          {"id": "creative_stream_execute"},
          {"id": "creative_stream_download"},
          {"id": "creative_stream_share"},
          {"id": "creative_stream_manage"},
          {"id": "creative_stream_archive"}
        ]
      }
    ]
  }
}
```

### 3.5 资源创建者关联操作 (`0007_create-related_*.json`)

定义创建资源后自动授予创建者的权限：

```json
{
  "id": "creative_stream",
  "actions": [
    {"id": "creative_stream_list", "required": false},
    {"id": "creative_stream_view", "required": false},
    {"id": "creative_stream_edit", "required": false},
    {"id": "creative_stream_delete", "required": false},
    {"id": "creative_stream_execute", "required": false},
    {"id": "creative_stream_download", "required": false},
    {"id": "creative_stream_share", "required": false},
    {"id": "creative_stream_manage", "required": false},
    {"id": "creative_stream_archive", "required": false}
  ]
}
```

---

## 第四步：数据库 DML 脚本

### 4.1 脚本类型与用途

| 脚本类型 | 文件位置 | 用途 | 是否必须 |
|----------|----------|------|----------|
| **初始化脚本** | `support-files/sql/5001_init_dml/5001_ci_auth-init_dml_mysql.sql` | 开源社区部署时的数据初始化 | ✅ **必须** |
| **增量脚本** | `openspec/changes/{change-id}/specs/auth-resource-type/xxx_dml.sql` | 内部线上已有数据的增量变更 | 内部使用 |
| **API 接口** | `/api/op/auth/resourceTypeConfig/*` | 内部线上已有数据的运行时变更 | 内部使用 |

> **重要**: 
> - **初始化脚本是必须的**，用于开源社区新部署时初始化权限数据
> - 增量脚本和 API 接口二选一，用于内部线上环境的数据变更

### 4.2 表结构说明

| 表名 | 说明 |
|------|------|
| `T_AUTH_RESOURCE_TYPE` | 资源类型定义 |
| `T_AUTH_ACTION` | 操作定义 |
| `T_AUTH_RESOURCE_GROUP_CONFIG` | 用户组配置（资源级 + 项目级） |

### 4.3 新增资源类型

```sql
REPLACE INTO T_AUTH_RESOURCE_TYPE (
    `ID`, RESOURCE_TYPE, NAME, ENGLISH_NAME, `DESC`, ENGLISH_DESC,
    PARENT, `SYSTEM`, CREATE_USER, CREATE_TIME, UPDATE_USER, UPDATE_TIME, `DELETE`
) VALUES (
    22,  -- 查询现有最大 ID + 1
    'creative_stream',
    '创作流',
    'Creative Stream',
    '创作流',
    'Creative Stream',
    'project',
    'bk_ci_rbac',
    'system',
    NOW(),
    'system',
    NOW(),
    0
);
```

### 4.4 新增操作定义

```sql
REPLACE INTO T_AUTH_ACTION(
    `ACTION`, RESOURCE_TYPE, RELATED_RESOURCE_TYPE, ACTION_NAME,
    ENGLISH_NAME, CREATE_USER, CREATE_TIME, UPDATE_TIME, `DELETE`, ACTION_TYPE
) VALUES
    ('creative_stream_view', 'creative_stream', 'creative_stream',
     '查看创作流', 'Creative Stream View', 'system', NOW(), NOW(), 0, 'view'),
    ('creative_stream_create', 'creative_stream', 'project',  -- 注意：关联 project
     '创建创作流', 'Creative Stream Create', 'system', NOW(), NOW(), 0, 'create'),
    -- ... 其他操作
;
```

### 4.5 新增资源级用户组

资源级用户组用于单个资源实例的权限管理：

```sql
-- 拥有者组 (全部权限)
REPLACE INTO T_AUTH_RESOURCE_GROUP_CONFIG(
    `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `CREATE_MODE`, `GROUP_TYPE`,
    `DESCRIPTION`, `AUTHORIZATION_SCOPES`, `ACTIONS`
) VALUES (
    70,  -- 查询现有最大 ID + 1
    'creative_stream',
    'manager',
    '拥有者',
    0,
    0,
    '创作流拥有者，可以管理当前创作流的权限',
    '[授权范围 JSON]',
    '["creative_stream_view","creative_stream_edit",...]'
);
```

**资源级用户组标准配置**:

| 组代码 | 组名 | 典型权限 |
|--------|------|----------|
| manager | 拥有者 | 全部权限（除 create） |
| editor | 编辑者 | view + edit + execute + list + download + share |
| executor | 执行者 | view + execute + list + download + share |
| viewer | 查看者 | view + list + download + share |

### 4.6 更新项目级用户组

为现有项目级用户组添加新资源的权限：

```sql
-- 使用 JSON_ARRAY_APPEND 追加权限
UPDATE T_AUTH_RESOURCE_GROUP_CONFIG
SET AUTHORIZATION_SCOPES = JSON_ARRAY_APPEND(
    AUTHORIZATION_SCOPES,
    '$',
    JSON_OBJECT(
        'system', '#system#',
        'actions', JSON_ARRAY(
            JSON_OBJECT('id', 'creative_stream_list'),
            JSON_OBJECT('id', 'creative_stream_download'),
            JSON_OBJECT('id', 'creative_stream_share')
        ),
        'resources', JSON_ARRAY(
            JSON_OBJECT(
                'system', '#system#',
                'type', 'creative_stream',
                'paths', JSON_ARRAY(
                    JSON_ARRAY(
                        JSON_OBJECT('system', '#system#', 'type', 'project', 
                                    'id', '#projectId#', 'name', '#projectName#')
                    )
                )
            )
        )
    )
)
WHERE ID = 2;  -- developer 组
```

**项目级用户组权限分配建议**:

| ID | 组名 | 建议权限 |
|----|------|----------|
| 1 | 管理员 (manager) | create + 全部资源操作 |
| 2 | 开发人员 (developer) | create + list + download + share |
| 3 | 运维人员 (maintainer) | create + list + download + share |
| 4 | 产品人员 (pm) | list |
| 5 | 测试人员 (tester) | create + list + download + share |
| 6 | 质管人员 (qc) | list |
| 7 | 访客 (visitor) | list |

---

## 第五步：使用 API 接口配置数据（内部线上变更）

> **适用场景**: 内部线上环境已有数据的增量变更，可替代增量 SQL 脚本，更简单、更安全、更不易出错。
> 
> **注意**: API 接口不能替代初始化脚本，初始化脚本用于开源社区新部署。

### 5.1 API 接口概览

**接口路径**: `/api/op/auth/resourceTypeConfig`

| 功能 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取资源类型列表 | GET | `/resourceTypes` | 查询所有资源类型 |
| 创建资源类型 | POST | `/resourceTypes` | 新增资源类型 |
| 获取操作列表 | GET | `/actions` | 查询操作定义 |
| 批量创建操作 | POST | `/actions/batch` | 批量新增操作 |
| 获取用户组配置 | GET | `/groupConfigs` | 查询用户组配置 |
| 批量创建用户组 | POST | `/groupConfigs/batch` | 批量新增资源级用户组 |
| 追加新权限块 | PUT | `/groupConfigs/{id}/appendActions` | 新增一个完整的资源类型权限块 |
| **追加到已有块** | PUT | `/groupConfigs/{id}/appendActionsToExistingScope` | **在已有权限块的 actions 中追加** |
| **智能追加** | PUT | `/groupConfigs/{id}/smartAppendActions` | **自动判断：存在则追加，不存在则新建** |
| 批量追加（新建块） | POST | `/groupConfigs/batchAppendActions` | 批量新增权限块 |
| **批量智能追加** | POST | `/groupConfigs/batchSmartAppendActions` | **批量自动判断追加方式** |
| 一键创建完整配置 | POST | `/resourceTypes/full` | 一次性创建资源类型+操作+用户组 |

### 5.2 追加 Actions 的三种方式

#### 方式一：追加新权限块（appendActions）

适用场景：为用户组添加一个**全新资源类型**的权限

```bash
# 在 AUTHORIZATION_SCOPES 数组末尾新增一个完整的权限块
curl -X PUT \
  "http://devops.example.com/api/op/auth/resourceTypeConfig/groupConfigs/1/appendActions\
?resourceType=creative_stream" \
  -H "Content-Type: application/json" \
  -d '["creative_stream_create", "creative_stream_list"]'
```

#### 方式二：追加到已有权限块（appendActionsToExistingScope）⭐

适用场景：某个资源类型的权限块**已存在**，只需要在其 actions 数组中追加新的 action

```bash
# 在已有的 project 权限块的 actions 数组中追加 creative_stream_create
curl -X PUT \
  "http://devops.example.com/api/op/auth/resourceTypeConfig/groupConfigs/1/\
appendActionsToExistingScope?targetResourceType=project" \
  -H "Content-Type: application/json" \
  -d '["creative_stream_create"]'
```

**示例效果**：
```json
// 修改前
{
  "system": "#system#",
  "actions": [{"id": "project_visit"}, {"id": "project_edit"}],
  "resources": [{"type": "project", ...}]
}

// 修改后（追加了 creative_stream_create）
{
  "system": "#system#",
  "actions": [
    {"id": "project_visit"}, {"id": "project_edit"}, {"id": "creative_stream_create"}
  ],
  "resources": [{"type": "project", ...}]
}
```

#### 方式三：智能追加（smartAppendActions）⭐⭐

适用场景：**不确定权限块是否存在**，让系统自动判断
- 如果目标资源类型的权限块已存在 → 追加到该块的 actions 中
- 如果不存在 → 创建新的权限块

```bash
# 智能追加：系统自动判断是追加到已有块还是新建块
curl -X PUT \
  "http://devops.example.com/api/op/auth/resourceTypeConfig/groupConfigs/1/\
smartAppendActions?resourceType=project" \
  -H "Content-Type: application/json" \
  -d '["creative_stream_create"]'
```

### 5.3 批量操作示例

#### 5.3.1 创建资源类型

```bash
curl -X POST "http://devops.example.com/api/op/auth/resourceTypeConfig/resourceTypes" \
  -H "Content-Type: application/json" \
  -d '{
    "resourceType": "creative_stream",
    "name": "创作流",
    "englishName": "Creative Stream",
    "desc": "创作流",
    "englishDesc": "Creative Stream",
    "parent": "project",
    "system": "bk_ci_rbac"
  }'
```

#### 5.3.2 批量创建操作

```bash
curl -X POST "http://devops.example.com/api/op/auth/resourceTypeConfig/actions/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "action": "creative_stream_create",
      "resourceType": "creative_stream",
      "relatedResourceType": "project",
      "actionName": "创建创作流",
      "englishName": "Creative Stream Create",
      "actionType": "create"
    },
    {
      "action": "creative_stream_list",
      "resourceType": "creative_stream",
      "relatedResourceType": "creative_stream",
      "actionName": "创作流列表",
      "englishName": "Creative Stream List",
      "actionType": "list"
    },
    {
      "action": "creative_stream_view",
      "resourceType": "creative_stream",
      "relatedResourceType": "creative_stream",
      "actionName": "查看创作流",
      "englishName": "Creative Stream View",
      "actionType": "view"
    }
  ]'
```

#### 5.3.3 批量创建资源级用户组

```bash
curl -X POST \
  "http://devops.example.com/api/op/auth/resourceTypeConfig/groupConfigs/batch" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "resourceType": "creative_stream",
      "groupCode": "manager",
      "groupName": "拥有者",
      "description": "创作流拥有者，可以管理当前创作流的权限",
      "createMode": false,
      "groupType": 0,
      "actions": [
        "creative_stream_view", "creative_stream_edit", "creative_stream_delete",
        "creative_stream_execute", "creative_stream_download", "creative_stream_share",
        "creative_stream_manage", "creative_stream_archive"
      ],
      "authorizationScopes": "[授权范围JSON]"
    },
    {
      "resourceType": "creative_stream",
      "groupCode": "editor",
      "groupName": "编辑者",
      "description": "创作流编辑者",
      "createMode": false,
      "groupType": 0,
      "actions": [
        "creative_stream_view", "creative_stream_edit", "creative_stream_execute",
        "creative_stream_download", "creative_stream_share"
      ],
      "authorizationScopes": "[授权范围JSON]"
    }
  ]'
```

#### 5.3.4 批量智能追加到项目级用户组

```bash
# 使用 batchSmartAppendActions：自动判断追加方式
curl -X POST \
  "http://devops.example.com/api/op/auth/resourceTypeConfig/groupConfigs/batchSmartAppendActions" \
  -H "Content-Type: application/json" \
  -d '[
    {
      "groupConfigId": 1,
      "resourceType": "project",
      "actions": ["creative_stream_create"]
    },
    {
      "groupConfigId": 2,
      "resourceType": "project",
      "actions": ["creative_stream_create"]
    },
    {
      "groupConfigId": 7,
      "resourceType": "creative_stream",
      "actions": ["creative_stream_list"]
    }
  ]'
```

> **说明**: 
> - ID 1、2 的 `project` 权限块已存在，会在其 actions 中追加 `creative_stream_create`
> - ID 7 的 `creative_stream` 权限块不存在，会新建一个权限块

### 5.4 API vs 增量 SQL 脚本对比

> **注意**: 以下对比仅针对内部线上环境的增量变更，不涉及初始化脚本。

| 维度 | API 接口 | 增量 SQL 脚本 |
|------|----------|---------------|
| **易用性** | 简单，无需了解表结构 | 需熟悉表结构和 JSON 格式 |
| **ID 管理** | 自动分配，无冲突风险 | 需手动查询并分配 |
| **幂等性** | 接口内置处理 | 需使用 REPLACE INTO |
| **验证** | 接口层参数校验 | 无校验，易出错 |
| **事务** | 自动事务管理 | 需手动管理 |
| **适用场景** | 内部线上运行时配置 | 内部线上版本升级 |

### 5.5 请求/响应 DTO 说明

#### ResourceTypeCreateRequest

```kotlin
data class ResourceTypeCreateRequest(
    val resourceType: String,    // 资源类型代码
    val name: String,            // 中文名称
    val englishName: String,     // 英文名称
    val desc: String? = null,    // 中文描述
    val englishDesc: String? = null,  // 英文描述
    val parent: String? = "project",  // 父资源类型
    val system: String? = "bk_ci_rbac"  // 系统标识
)
```

#### ActionCreateRequest

```kotlin
data class ActionCreateRequest(
    val action: String,              // 操作代码
    val resourceType: String,        // 所属资源类型
    val relatedResourceType: String, // 关联资源类型（create 操作为 project）
    val actionName: String,          // 中文名称
    val englishName: String,         // 英文名称
    val actionType: String           // 操作类型：create/list/view/edit/delete/execute
)
```

#### ResourceGroupConfigCreateRequest

```kotlin
data class ResourceGroupConfigCreateRequest(
    val resourceType: String,        // 资源类型
    val groupCode: String,           // 组代码
    val groupName: String,           // 组名称
    val description: String? = null, // 描述
    val createMode: Boolean = false, // 创建模式
    val groupType: Int = 0,          // 组类型
    val actions: List<String>,       // 操作列表
    val authorizationScopes: String? = null  // 授权范围 JSON
)
```

#### ProjectGroupConfigUpdateRequest

```kotlin
data class ProjectGroupConfigUpdateRequest(
    val groupConfigId: Long,     // 项目级用户组配置 ID (1-7)
    val resourceType: String,    // 资源类型
    val actions: List<String>    // 要追加的操作列表
)
```

---

## 第六步：国际化配置

### 6.1 文件位置

`support-files/i18n/auth/` 目录下的三个文件：

- `message_zh_CN.properties` (中文)
- `message_en_US.properties` (英文)
- `message_ja_JP.properties` (日文)

### 6.2 配置内容

```properties
# 资源类型
creative_stream.resourceType.name=创作流
creative_stream.resourceType.desc=创作流

# 操作名称
creative_stream_create.actionName=创建创作流
creative_stream_list.actionName=创作流列表
creative_stream_view.actionName=查看创作流
creative_stream_edit.actionName=编辑创作流
creative_stream_delete.actionName=删除创作流
creative_stream_execute.actionName=执行创作流
creative_stream_download.actionName=下载创作流制品
creative_stream_share.actionName=分享创作流制品
creative_stream_manage.actionName=创作流权限管理
creative_stream_archive.actionName=归档创作流

# 资源级用户组
creative_stream.manager.authResourceGroupConfig.groupName=拥有者
creative_stream.manager.authResourceGroupConfig.description=创作流拥有者，可以管理当前创作流的权限
creative_stream.editor.authResourceGroupConfig.groupName=编辑者
creative_stream.editor.authResourceGroupConfig.description=创作流编辑者，拥有当前创作流除了权限管理之外的所有权限
creative_stream.executor.authResourceGroupConfig.groupName=执行者
creative_stream.executor.authResourceGroupConfig.description=创作流执行者，可以查看和执行创作流，下载或分享制品
creative_stream.viewer.authResourceGroupConfig.groupName=查看者
creative_stream.viewer.authResourceGroupConfig.description=创作流查看者，可以查看创作流，下载或分享制品
```

---

## 第七步：同步初始化脚本（必须）

### 7.1 更新 `5001_ci_auth-init_dml_mysql.sql`

**这是必须步骤**，初始化脚本用于开源社区新部署时的数据初始化。

将变更内容同步到初始化脚本，使用 `REPLACE INTO` 语法确保幂等性。

**注意事项**:
- 初始化脚本使用完整的 JSON 字符串，不使用 `JSON_ARRAY_APPEND`
- 确保 ID 不与现有数据冲突
- 使用 `REPLACE INTO` 而非 `INSERT` 保证幂等
- **必须同步所有变更**，包括：
  - 新增资源类型
  - 新增操作定义
  - 新增资源级用户组
  - 更新项目级用户组（直接修改完整 JSON）

---

## 第八步：验证清单

### 8.1 文件修改清单

| 文件 | 修改内容 | 是否必须 |
|------|----------|----------|
| `AuthResourceType.kt` | 新增枚举值 | ✅ 必须 |
| `0003_resource_*.json` | 新增资源类型 | ✅ 必须 |
| `0004_instance-views_*.json` | 新增实例选择器 | ✅ 必须 |
| `0005_action_*.json` | 新增所有操作定义 | ✅ 必须 |
| `0006_group_*.json` | 新增权限分组 | ✅ 必须 |
| `0007_create-related_*.json` | 新增创建者关联操作 | ✅ 必须 |
| `message_zh_CN.properties` | 中文国际化 | ✅ 必须 |
| `message_en_US.properties` | 英文国际化 | ✅ 必须 |
| `message_ja_JP.properties` | 日文国际化 | ✅ 必须 |
| `5001_ci_auth-init_dml_mysql.sql` | 开源社区部署初始化数据 | ✅ **必须** |
| 增量 SQL 脚本 | 内部线上增量变更 | 内部使用（与 API 二选一） |

### 8.2 验证要点

- [ ] 资源类型 ID 全局唯一
- [ ] 操作 ID 命名规范：`{resource}_{action}`
- [ ] `create` 操作关联 `project` 资源类型
- [ ] 所有操作都有正确的 `related_actions` 依赖
- [ ] 用户组 ID 不与现有数据冲突（使用 API 自动处理）
- [ ] 三语国际化配置完整
- [ ] 项目级所有用户组（ID 1-7）都已更新
- [ ] 资源级用户组权限矩阵合理

### 8.3 API 接口验证

使用 API 接口配置后，可通过以下接口验证：

```bash
# 验证资源类型
curl "http://devops.example.com/api/op/auth/resourceTypeConfig/resourceTypes/creative_stream"

# 验证操作列表
curl "http://devops.example.com/api/op/auth/resourceTypeConfig/actions?resourceType=creative_stream"

# 验证用户组配置
curl \
  "http://devops.example.com/api/op/auth/resourceTypeConfig/groupConfigs?resourceType=creative_stream"
```

---

## 常见问题与解决方案

### Q1: 如何确定新资源类型的 ID？

查询现有最大 ID：
```sql
SELECT MAX(ID) FROM T_AUTH_RESOURCE_TYPE;
SELECT MAX(ID) FROM T_AUTH_RESOURCE_GROUP_CONFIG;
```

### Q2: 权限依赖关系如何设计？

遵循最小权限原则：
- `view` 依赖 `list`
- `edit/delete/execute` 依赖 `view`
- `manage` 依赖 `edit`
- 所有操作依赖 `project_visit`

### Q3: 为什么 create 操作关联 project？

因为创建资源时，资源实例还不存在，无法基于资源实例授权，所以需要基于项目授权。


### Q4: AUTHORIZATION_SCOPES JSON 结构？

```json
[
  {
    "system": "#system#",
    "actions": [{"id": "action_id"}],
    "resources": [
      {
        "system": "#system#",
        "type": "resource_type",
        "paths": [[
          {"system": "#system#", "type": "project", "id": "#projectId#", "name": "#projectName#"},
          {"system": "#system#", "type": "resource_type", "id": "#resourceCode#", "name": "#resourceName#"}
        ]]
      }
    ]
  }
]
```

---

## 参考资料

- 蓝鲸权限中心文档
- 已有资源类型实现：`pipeline`、`credential`、`environment` 等
- 本次 `creative_stream` 接入归档：`openspec/changes/archive/2025-12-16-add-creative-stream-resource-type/`

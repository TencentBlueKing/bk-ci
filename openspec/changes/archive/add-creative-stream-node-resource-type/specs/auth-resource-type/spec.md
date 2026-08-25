## ADDED Requirements

### Requirement: Creative Stream Node 资源类型定义

系统 SHALL 支持 `creative_stream_node` 资源类型，用于创作流节点的权限管理。

#### Scenario: 资源类型注册
- **WHEN** 系统启动时
- **THEN** `creative_stream_node` 资源类型在 IAM 权限中心正确注册
- **AND** 资源类型的父资源为 `project`

#### Scenario: 枚举定义
- **WHEN** 后端代码引用创作流节点资源类型时
- **THEN** 可通过 `AuthResourceType.CREATIVE_STREAM_NODE` 获取
- **AND** 其 value 值为 `"creative_stream_node"`

---

### Requirement: Creative Stream Node 权限操作定义

系统 SHALL 为创作流节点资源类型提供 2 个权限操作：查看和编辑。

#### Scenario: 查看操作
- **WHEN** 用户需要查看创作流节点时
- **THEN** 系统校验 `creative_stream_node_view` 权限
- **AND** 该操作关联资源类型为 `creative_stream_node`
- **AND** 依赖 `project_visit` 权限

#### Scenario: 编辑操作
- **WHEN** 用户需要编辑创作流节点时
- **THEN** 系统校验 `creative_stream_node_edit` 权限
- **AND** 该操作关联资源类型为 `creative_stream_node`
- **AND** 依赖 `project_visit` 和 `creative_stream_node_view` 权限

---

### Requirement: Creative Stream Node 实例选择器

系统 SHALL 提供 `creative_stream_node_instance` 实例选择器，用于 IAM 权限配置时选择具体的创作流节点实例。

#### Scenario: 实例选择器定义
- **WHEN** 在 IAM 配置权限策略时
- **THEN** 可通过 `creative_stream_node_instance` 选择器选择创作流节点实例
- **AND** 资源类型链为 `project` → `creative_stream_node`

---

### Requirement: Creative Stream Node 资源级用户组

系统 SHALL 为创作流节点资源类型提供「拥有者」一个资源级用户组。

#### Scenario: 拥有者组
- **WHEN** 创作流节点资源被创建时
- **THEN** 自动创建「拥有者 (manager)」用户组
- **AND** 该组拥有 `creative_stream_node_view` 和 `creative_stream_node_edit` 权限

---

### Requirement: Creative Stream Node 项目级用户组权限（参考 env_node）

系统 SHALL 在项目级用户组中为创作流节点配置权限，分配模式参考环境节点 (env_node)。

#### Scenario: 管理员权限
- **WHEN** 用户属于项目「管理员 (manager)」组时
- **THEN** 该用户对项目下所有创作流节点拥有 `creative_stream_node_view` 和 `creative_stream_node_edit` 权限

#### Scenario: 开发人员权限
- **WHEN** 用户属于项目「开发人员 (developer)」组时
- **THEN** 该用户对项目下所有创作流节点拥有 `creative_stream_node_view` 权限

#### Scenario: 运维人员权限
- **WHEN** 用户属于项目「运维人员 (maintainer)」组时
- **THEN** 该用户对项目下所有创作流节点拥有 `creative_stream_node_view` 权限

#### Scenario: 产品人员无权限
- **WHEN** 用户属于项目「产品人员 (pm)」组时
- **THEN** 该用户对创作流节点无任何默认权限

#### Scenario: 测试人员无权限
- **WHEN** 用户属于项目「测试人员 (tester)」组时
- **THEN** 该用户对创作流节点无任何默认权限

#### Scenario: 质管人员无权限
- **WHEN** 用户属于项目「质管人员 (qc)」组时
- **THEN** 该用户对创作流节点无任何默认权限

#### Scenario: 访客无权限
- **WHEN** 用户属于项目「访客 (visitor)」组时
- **THEN** 该用户对创作流节点无任何默认权限

---

### Requirement: Creative Stream Node 权限分组

系统 SHALL 在 IAM 权限中心新增独立的「创作流节点」权限分组，与「创作流」分组同级。

#### Scenario: 权限分组展示
- **WHEN** 用户在 IAM 权限中心查看权限列表时
- **THEN** 可看到独立的「创作流节点」分组
- **AND** 该分组与「创作流」分组同级
- **AND** 分组包含 `creative_stream_node_view` 和 `creative_stream_node_edit` 两个操作

---

### Requirement: Creative Stream Node 国际化支持

系统 SHALL 为创作流节点资源类型和操作提供中英日三语国际化支持。

#### Scenario: 中文显示
- **WHEN** 系统语言为中文时
- **THEN** 资源类型显示为「创作流节点」
- **AND** 操作显示为「查看创作流节点」「编辑创作流节点」

#### Scenario: 英文显示
- **WHEN** 系统语言为英文时
- **THEN** 资源类型显示为「Creative Stream Node」
- **AND** 操作显示为「Creative Stream Node View」「Creative Stream Node Edit」

#### Scenario: 日文显示
- **WHEN** 系统语言为日文时
- **THEN** 资源类型显示为对应日文翻译

---

## 数据变更说明

### 数据库: devops_auth

---

### 一、新增数据 (INSERT/REPLACE)

#### 1. T_AUTH_RESOURCE_TYPE 表

| 变更类型 | ID | RESOURCE_TYPE | NAME | PARENT | 说明 |
|----------|-----|---------------|------|--------|------|
| **新增** | 23 | creative_stream_node | 创作流节点 | project | 新增创作流节点资源类型 |

#### 2. T_AUTH_ACTION 表

| 变更类型 | ACTION | RESOURCE_TYPE | ACTION_NAME | ACTION_TYPE | RELATED_RESOURCE_TYPE |
|----------|--------|---------------|-------------|-------------|----------------------|
| **新增** | creative_stream_node_view | creative_stream_node | 查看创作流节点 | view | creative_stream_node |
| **新增** | creative_stream_node_edit | creative_stream_node | 编辑创作流节点 | edit | creative_stream_node |

#### 3. T_AUTH_RESOURCE_GROUP_CONFIG 表 - 创作流节点资源级用户组

| 变更类型 | ID | RESOURCE_TYPE | GROUP_CODE | GROUP_NAME | 权限操作数 |
|----------|-----|---------------|------------|------------|------------|
| **新增** | 74 | creative_stream_node | manager | 拥有者 | 2 |

---

### 二、更新数据 (UPDATE)

#### T_AUTH_RESOURCE_GROUP_CONFIG 表 - 项目级默认用户组

| ID | GROUP_CODE | GROUP_NAME | 变更内容 |
|----|------------|------------|----------|
| 1 | manager | 管理员 | AUTHORIZATION_SCOPES 新增创作流节点 view + edit 权限 |
| 2 | developer | 开发人员 | AUTHORIZATION_SCOPES 新增创作流节点 view 权限 |
| 3 | maintainer | 运维人员 | AUTHORIZATION_SCOPES 新增创作流节点 view 权限 |

> 注意：PM (4)、QA (5)、QC (6)、访客 (7) 不分配创作流节点权限。

---

### 三、与环境节点 (env_node) 对比

| 特性 | env_node (线上) | creative_stream_node (新增) |
|------|-----------------|---------------------------|
| 资源类型 ID | 7 | 23 |
| 资源类型代码 | env_node | creative_stream_node |
| 中文名称 | 节点 | 创作流节点 |
| 操作数量 | 6 | 2 |
| 资源级用户组数量 | 2 (manager/user) | 1 (manager) |
| 项目级管理员权限 | create+view+edit+delete+list+use | view+edit |
| 项目级开发/运维权限 | create+view+list | view |
| 项目级 PM 权限 | list | (无) |

---

### 四、权限矩阵

#### 创作流节点资源级用户组权限

| 权限操作 | 拥有者 (74) |
|----------|:-----------:|
| creative_stream_node_view | ✅ |
| creative_stream_node_edit | ✅ |

#### 项目级默认用户组的创作流节点权限

| 权限操作 | 管理员 (1) | 开发人员 (2) | 运维人员 (3) | 产品人员 (4) | 测试人员 (5) | 质管人员 (6) | 访客 (7) |
|----------|:----------:|:------------:|:------------:|:------------:|:------------:|:------------:|:--------:|
| creative_stream_node_view | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| creative_stream_node_edit | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

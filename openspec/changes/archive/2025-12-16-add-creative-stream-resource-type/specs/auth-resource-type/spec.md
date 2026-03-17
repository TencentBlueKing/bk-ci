## ADDED Requirements

### Requirement: Creative Stream 资源类型定义

系统 SHALL 支持 `creative_stream` 资源类型，用于创作流的权限管理。

#### Scenario: 资源类型注册
- **WHEN** 系统启动时
- **THEN** `creative_stream` 资源类型在 IAM 权限中心正确注册
- **AND** 资源类型的父资源为 `project`

#### Scenario: 枚举定义
- **WHEN** 后端代码引用创作流资源类型时
- **THEN** 可通过 `AuthResourceType.CREATIVE_STREAM` 获取
- **AND** 其 value 值为 `"creative_stream"`

---

### Requirement: Creative Stream 权限操作定义

系统 SHALL 为创作流资源类型提供与流水线一致的 10 个权限操作。

#### Scenario: 创建操作
- **WHEN** 用户需要创建创作流时
- **THEN** 系统校验 `creative_stream_create` 权限
- **AND** 该操作关联资源类型为 `project`
- **AND** 依赖 `project_visit` 权限

#### Scenario: 查看操作
- **WHEN** 用户需要查看创作流时
- **THEN** 系统校验 `creative_stream_view` 权限
- **AND** 该操作关联资源类型为 `creative_stream`
- **AND** 依赖 `project_visit` 和 `creative_stream_list` 权限

#### Scenario: 编辑操作
- **WHEN** 用户需要编辑创作流时
- **THEN** 系统校验 `creative_stream_edit` 权限
- **AND** 依赖 `creative_stream_view` 权限

#### Scenario: 删除操作
- **WHEN** 用户需要删除创作流时
- **THEN** 系统校验 `creative_stream_delete` 权限
- **AND** 依赖 `creative_stream_view` 权限

#### Scenario: 执行操作
- **WHEN** 用户需要执行创作流时
- **THEN** 系统校验 `creative_stream_execute` 权限
- **AND** 依赖 `creative_stream_view` 权限

#### Scenario: 列表操作
- **WHEN** 用户需要查看创作流列表时
- **THEN** 系统校验 `creative_stream_list` 权限
- **AND** 依赖 `project_visit` 权限

#### Scenario: 分享操作
- **WHEN** 用户需要分享创作流时
- **THEN** 系统校验 `creative_stream_share` 权限
- **AND** 依赖 `creative_stream_view` 权限

#### Scenario: 下载操作
- **WHEN** 用户需要下载创作流时
- **THEN** 系统校验 `creative_stream_download` 权限
- **AND** 依赖 `creative_stream_view` 权限

#### Scenario: 管理操作
- **WHEN** 用户需要管理创作流权限时
- **THEN** 系统校验 `creative_stream_manage` 权限
- **AND** 依赖 `creative_stream_edit` 权限

#### Scenario: 归档操作
- **WHEN** 用户需要归档创作流时
- **THEN** 系统校验 `creative_stream_archive` 权限
- **AND** 依赖 `creative_stream_edit` 权限

---

### Requirement: Creative Stream 实例选择器

系统 SHALL 提供 `creative_stream_instance` 实例选择器，用于 IAM 权限配置时选择具体的创作流实例。

#### Scenario: 实例选择器定义
- **WHEN** 在 IAM 配置权限策略时
- **THEN** 可通过 `creative_stream_instance` 选择器选择创作流实例
- **AND** 资源类型链为 `project` → `creative_stream`

---

### Requirement: Creative Stream 权限分组

系统 SHALL 在 IAM 权限中心提供「创作流」权限分组，包含所有创作流相关操作。

#### Scenario: 权限分组展示
- **WHEN** 用户在 IAM 权限中心查看权限列表时
- **THEN** 可看到「创作流」分组
- **AND** 分组包含 10 个创作流操作权限

---

### Requirement: Creative Stream 国际化支持

系统 SHALL 为创作流资源类型和操作提供中英日三语国际化支持。

#### Scenario: 中文显示
- **WHEN** 系统语言为中文时
- **THEN** 资源类型显示为「创作流」
- **AND** 操作显示为「创建创作流」「查看创作流」等

#### Scenario: 英文显示
- **WHEN** 系统语言为英文时
- **THEN** 资源类型显示为「Creative Stream」
- **AND** 操作显示为「Creative Stream Create」「Creative Stream View」等

#### Scenario: 日文显示
- **WHEN** 系统语言为日文时
- **THEN** 资源类型显示为「クリエイティブストリーム」
- **AND** 操作显示为对应日文翻译

---

## 数据变更说明

### 数据库: devops_auth

#### 变更文件
- [creative_stream_dml.sql](./creative_stream_dml.sql)

---

### 一、新增数据 (INSERT/REPLACE)

#### 1. T_AUTH_RESOURCE_TYPE 表

| 变更类型 | ID | RESOURCE_TYPE | NAME | PARENT | 说明 |
|----------|-----|---------------|------|--------|------|
| **新增** | 22 | creative_stream | 创作流 | project | 新增创作流资源类型 |

#### 2. T_AUTH_ACTION 表

| 变更类型 | ACTION | RESOURCE_TYPE | ACTION_NAME | ACTION_TYPE | RELATED_RESOURCE_TYPE |
|----------|--------|---------------|-------------|-------------|----------------------|
| **新增** | creative_stream_view | creative_stream | 查看创作流 | view | creative_stream |
| **新增** | creative_stream_edit | creative_stream | 编辑创作流 | edit | creative_stream |
| **新增** | creative_stream_delete | creative_stream | 删除创作流 | delete | creative_stream |
| **新增** | creative_stream_create | creative_stream | 创建创作流 | create | project |
| **新增** | creative_stream_execute | creative_stream | 执行创作流 | execute | creative_stream |
| **新增** | creative_stream_download | creative_stream | 下载制品 | execute | creative_stream |
| **新增** | creative_stream_share | creative_stream | 分享制品 | execute | creative_stream |
| **新增** | creative_stream_list | creative_stream | 创作流列表 | view | creative_stream |
| **新增** | creative_stream_manage | creative_stream | 创作流权限管理 | edit | creative_stream |
| **新增** | creative_stream_archive | creative_stream | 归档创作流 | edit | creative_stream |

#### 3. T_AUTH_RESOURCE_GROUP_CONFIG 表 - 创作流资源级用户组

| 变更类型 | ID | RESOURCE_TYPE | GROUP_CODE | GROUP_NAME | 权限操作数 |
|----------|-----|---------------|------------|------------|------------|
| **新增** | 70 | creative_stream | manager | 拥有者 | 9 |
| **新增** | 71 | creative_stream | editor | 编辑者 | 6 |
| **新增** | 72 | creative_stream | executor | 执行者 | 5 |
| **新增** | 73 | creative_stream | viewer | 查看者 | 4 |

---

### 二、更新数据 (UPDATE)

#### T_AUTH_RESOURCE_GROUP_CONFIG 表 - 项目级默认用户组

| ID | GROUP_CODE | GROUP_NAME | 变更内容 |
|----|------------|------------|----------|
| 1 | manager | 管理员 | ACTIONS 新增 `creative_stream_create`, `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流全部权限 |
| 2 | developer | 开发人员 | ACTIONS 新增 `creative_stream_create`, `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流列表/下载/分享权限 |
| 3 | maintainer | 运维人员 | ACTIONS 新增 `creative_stream_create`, `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流列表/下载/分享权限 |
| 4 | pm | 产品人员 | ACTIONS 新增 `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流列表权限 |
| 5 | tester | 测试人员 | ACTIONS 新增 `creative_stream_create`, `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流列表/下载/分享权限 |
| 6 | qc | 质管人员 | ACTIONS 新增 `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流列表权限 |
| 7 | visitor | 访客 | ACTIONS 新增 `creative_stream_list`<br>AUTHORIZATION_SCOPES 新增创作流列表权限 |

---

### 三、与流水线 (Pipeline) 对比

| 特性 | Pipeline (线上) | Creative Stream (新增) |
|------|-----------------|------------------------|
| 资源类型 ID | 1 | 22 |
| 资源类型代码 | pipeline | creative_stream |
| 中文名称 | 流水线 | 创作流 |
| 操作数量 | 10 | 10 |
| 资源级用户组数量 | 4 | 4 |
| 资源级用户组 ID 范围 | 8-11 | 70-73 |
| 项目级"任意资源"用户组 | ✅ (34-37) | ❌ (不需要) |
| **有组概念** | ✅ (pipeline_group, ID=2) | ❌ (无) |

---

### 四、权限矩阵

#### 创作流资源级用户组权限

| 权限操作 | 拥有者 (70) | 编辑者 (71) | 执行者 (72) | 查看者 (73) |
|----------|:-----------:|:-----------:|:-----------:|:-----------:|
| creative_stream_view | ✅ | ✅ | ✅ | ✅ |
| creative_stream_list | ✅ | ✅ | ✅ | ✅ |
| creative_stream_download | ✅ | ✅ | ✅ | ✅ |
| creative_stream_share | ✅ | ✅ | ✅ | ✅ |
| creative_stream_execute | ✅ | ✅ | ✅ | ❌ |
| creative_stream_edit | ✅ | ✅ | ❌ | ❌ |
| creative_stream_delete | ✅ | ❌ | ❌ | ❌ |
| creative_stream_manage | ✅ | ❌ | ❌ | ❌ |
| creative_stream_archive | ✅ | ❌ | ❌ | ❌ |

#### 项目级默认用户组的创作流权限

| 权限操作 | 管理员 (1) | 开发人员 (2) | 运维人员 (3) | 产品人员 (4) | 测试人员 (5) | 质管人员 (6) | 访客 (7) |
|----------|:----------:|:------------:|:------------:|:------------:|:------------:|:------------:|:--------:|
| creative_stream_create | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| creative_stream_list | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| creative_stream_view | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| creative_stream_edit | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| creative_stream_delete | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| creative_stream_execute | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| creative_stream_download | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| creative_stream_share | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| creative_stream_manage | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| creative_stream_archive | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

## Context

BK-CI 权限系统基于 IAM RBAC 模型运行。`codecc_rule_set` 资源类型已经存在，且当前具备项目级入口动作
`codecc_rule_set_create` 与 `codecc_rule_set_list`，但尚未形成完整的资源级授权闭环：

- 缺少 `codecc_rule_set_edit` 与 `codecc_rule_set_manage` 两个实例级动作
- 缺少 `codecc_rule_set` 的资源级默认用户组配置
- 存量规则集没有 owner 组迁移机制
- 规则集相关可操作性仍依赖“项目管理员”粗粒度判断

这次变更只处理 Auth / IAM 侧的权限模型与迁移闭环，不处理 CodeCC 业务方如何发起鉴权调用。

### 约束条件
1. 不新增新的资源类型，继续使用现有 `codecc_rule_set`
2. 动作 ID 必须遵循 `{resource_type}_{action}` 命名，即 `codecc_rule_set_edit`、
   `codecc_rule_set_manage`
3. 只新增一个资源级 `owner` 组，不扩展 editor / viewer / executor
4. 默认组只调整 `resourceType = project` 的管理员组，不调整 `ci_manager`
5. 存量资源重置统一使用：
   - `migrateResource = true`
   - `filterResourceTypes = ["codecc_rule_set"]`
6. 需要交付一份可单独执行的增量 DML 脚本，不能只依赖全量初始化脚本

## Goals / Non-Goals

### Goals
- 为 `codecc_rule_set` 补齐资源实例级 `edit` / `manage` 权限
- 为每个 `codecc_rule_set` 资源建立唯一的 `owner` 组
- 让项目管理员默认拥有项目内所有规则集的 `edit` / `manage` 权限
- 通过权限重置与回调接口，把存量规则集迁移到新的资源级 owner 模型
- 交付初始化 SQL 与专项 DML 脚本，满足新装与线上增量两种落地方式

### Non-Goals
- 不新增 `codecc_rule_set_view`、`delete`、`share` 等其他实例级动作
- 不为 `codecc_rule_set` 增加 editor / viewer / executor 等其他资源级组
- 不改造 CodeCC 平台或其他平台的业务侧鉴权调用
- 不引入新的资源类型别名或重命名现有 `codecc_rule_set`

## Decisions

### 决策 1：继续保留 create/list 为项目级入口权限，edit/manage 作为资源级权限
- 方案：
  - `codecc_rule_set_create`、`codecc_rule_set_list` 继续挂在 `project` 维度
  - `codecc_rule_set_edit`、`codecc_rule_set_manage` 挂在 `codecc_rule_set` 资源维度
- 原因：
  - create/list 本质是“进入规则集域”或“创建新规则集”的入口权限，和已有模型一致
  - edit/manage 才是具体规则集实例的可授权能力

### 决策 2：资源级只提供 owner 一个组
- 方案：新增 `codecc_rule_set` 的 `manager` 组，业务语义为 owner。
- 权限范围：
  - `codecc_rule_set_edit`
  - `codecc_rule_set_manage`
- 原因：
  - 当前需求只要求把权限从项目管理员收敛到资源拥有者，不需要进一步拆分角色
  - 单组模型能降低迁移复杂度，也更符合现阶段的使用诉求

### 决策 3：项目管理员组直接补齐 edit/manage 兜底权限
- 方案：在 `resourceType = project` 的默认管理员组中增加：
  - `codecc_rule_set_edit`
  - `codecc_rule_set_manage`
- 原因：
  - 保持项目管理员对项目内资源的运维兜底能力，避免新增模型后出现权限倒退
  - 用户已明确 `ci_manager` 不在本次范围内

### 决策 4：存量资源按“已有资源类型完整补齐 RBAC 能力”进行重置
- 方案：统一通过 `resetProjectPermissions` 执行，参数固定为：
  - `migrateResource = true`
  - `filterResourceTypes = ["codecc_rule_set"]`
- 不传 `filterActions`
- 原因：
  - 本次不仅是新增动作，还包含资源组创建、成员补齐和存量资源迁移
  - 只传 `filterActions` 会更偏向“补动作”，不足以表达“补齐整套资源级模型”

### 决策 5：owner 信息通过回调接口获取
- 方案：迁移时通过回调接口获取每个 `codecc_rule_set` 的 owner，作为 owner 组成员来源。
- 回调契约假设：
  - 每个规则集返回单个 owner
  - owner 标识使用可直接写入成员关系的 `userId`
  - 若后续业务侧需要多 owner，视为后续增强，不在本次范围
- 原因：
  - owner 真值来自业务侧，Auth 不应自行猜测或固化业务字段含义
  - 可以复用现有迁移链路，把 owner 组创建与成员回填纳入一次重置流程

### 决策 6：同时维护两类 SQL 交付物
- 方案：
  - 修改 `5001_ci_auth-init_dml_mysql.sql`，保证新装环境初始化可用
  - 新增一份专项增量 DML 脚本，形式上类似 `creative_stream_dml.sql`
- 原因：
  - 新装与升级的执行路径不同，缺一不可
  - 线上权限模型变更通常需要单独审阅和执行增量脚本

## Permission Model

### 项目级权限
| Action | Resource Type | 用途 |
|--------|---------------|------|
| `codecc_rule_set_create` | `project` | 创建规则集入口 |
| `codecc_rule_set_list` | `project` | 规则集列表入口 |

### 资源级权限
| Action | Resource Type | 用途 |
|--------|---------------|------|
| `codecc_rule_set_edit` | `codecc_rule_set` | 编辑规则集内容 |
| `codecc_rule_set_manage` | `codecc_rule_set` | 管理规则集权限与管理类操作 |

### 资源级用户组
| Resource Type | Group Code | 展示语义 | Actions |
|---------------|------------|----------|---------|
| `codecc_rule_set` | `manager` | Owner | `codecc_rule_set_edit`, `codecc_rule_set_manage` |

## Migration Design

### 重置入口
对受影响项目执行：

```text
migrateResource = true
filterResourceTypes = ["codecc_rule_set"]
```

语义上将本次变更视为：`codecc_rule_set` 这类已有资源需要补齐完整的资源级权限能力，而非仅追加动作。

### 迁移流程
```text
遍历项目
  -> resetProjectPermissions
    -> 重置项目管理员默认组权限
    -> 迁移 codecc_rule_set 资源
      -> 调用回调接口获取资源 owner
      -> 创建/修复 codecc_rule_set owner 组
      -> 写入 owner 组成员
      -> 绑定 edit/manage 权限
```

### 回调要求
- 输入至少能定位项目与规则集资源
- 输出包含单个 owner 的 `userId`
- 回调不可用、owner 缺失、owner 非法时，需要记录失败明细，避免静默跳过
- 单个规则集迁移失败不应阻断其他规则集迁移；失败资源需支持后续补偿重跑

### 幂等要求
- 已存在的 action、group config、group member 需支持重复执行
- 重复重置时，不应产生重复 group config 或重复成员
- 增量 DML 脚本需使用可幂等的写入方式

## Data / Config Changes

### IAM RBAC 配置
- `0005_action_20221213_iam-rbac.json`
  - 新增 `codecc_rule_set_edit`
  - 新增 `codecc_rule_set_manage`
- `0006_group_20221213_iam-rbac.json`
  - 在 `CodeCC规则集` 分组下追加上述两个动作

### 初始化 SQL
- `5001_ci_auth-init_dml_mysql.sql`
  - 新增 `T_AUTH_ACTION` 两条记录
  - 更新 `project` 管理员默认组的 `AUTHORIZATION_SCOPES` / `ACTIONS`
  - 新增 `codecc_rule_set` 的资源级 owner 组配置
  - 本次需求口径不新增额外的 project 级迁移组配置；若现有迁移框架技术上必须依赖模板配置，
    该配置仅作为实现细节用于创建 `codecc_rule_set` owner 组，不改变权限范围，也不涉及 `ci_manager`

### 增量 DML 脚本
- 新增一份专项脚本，建议命名体现 `codecc_rule_set` 与权限补齐用途
- 脚本内容至少覆盖：
  - 两条 action 数据
  - 项目管理员组补权
  - `codecc_rule_set` owner 组配置
  - 若迁移框架技术上必须依赖模板配置，则补齐与 owner 组创建直接相关的最小化模板数据

### 国际化
- `message_zh_CN.properties`
- `message_en_US.properties`
- `message_ja_JP.properties`

至少补齐：
- `codecc_rule_set_edit.actionName`
- `codecc_rule_set_manage.actionName`
- `codecc_rule_set` owner 组相关文案

## Risks / Trade-offs

- [风险] 只补 action 不补 group config，导致外部平台可看到动作但无法真正做实例授权
  Mitigation：把资源级 owner 组配置列为本次必做项，并纳入专项 DML 脚本
- [风险] 只迁移 project 默认组，不迁移存量资源 owner，导致历史规则集仍依赖项目管理员
  Mitigation：固定使用 `migrateResource = true` + `filterResourceTypes = ["codecc_rule_set"]`
- [风险] 回调接口拿不到 owner 或返回离职用户，导致 owner 组不完整
  Mitigation：记录失败明细并支持补偿重跑，不在 Auth 侧自行兜底推断业务 owner
- [风险] 只维护初始化 SQL，遗漏线上增量执行脚本
  Mitigation：把专项 DML 脚本列为独立交付物和独立任务
- [取舍] 只保留 owner 单组，牺牲更细粒度的编辑/管理角色拆分
  Mitigation：先满足当前诉求，未来若出现明确场景再扩展 editor/viewer

## Migration Plan

1. 先补齐 RBAC action、分组、初始化 SQL 与 i18n。
2. 编写专项 DML 脚本，覆盖线上增量执行所需数据变更。
3. 确认回调接口可稳定返回 `codecc_rule_set` owner 信息。
4. 按项目执行 `resetProjectPermissions`，参数固定为：
   - `migrateResource = true`
   - `filterResourceTypes = ["codecc_rule_set"]`
5. 抽样验证：
   - 项目管理员默认拥有规则集 edit/manage
   - 存量规则集已生成 owner 组
   - owner 组成员与回调返回一致

### Rollback
- 回滚专项 DML 脚本写入的数据
- 删除 IAM 中新增的 `codecc_rule_set_edit`、`codecc_rule_set_manage` 配置
- 回滚初始化 SQL 与相关代码改动
- 必要时重新执行一次权限重置，恢复到旧模型

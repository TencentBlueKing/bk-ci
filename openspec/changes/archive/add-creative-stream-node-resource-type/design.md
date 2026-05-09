## Context

BK-CI 权限系统基于 RBAC 模型，通过 IAM (权限中心) 进行权限管理。新增资源类型需要遵循严格的文件依赖顺序和命名规范。

创作流 (creative_stream) 资源类型已在 `2025-12-16-add-creative-stream-resource-type` 变更中完成接入。本次新增的是创作流的子资源——「创作流节点」(creative_stream_node)。

### 约束条件
1. 必须按 0003 → 0004 → 0005 → 0006 → 0007 的顺序定义
2. 所有资源类型的父资源为 `project`
3. 操作 ID 格式: `{resource_type}_{action}`
4. 实例选择器 ID 格式: `{resource_type}_instance`

## Goals / Non-Goals

### Goals
- 新增 `creative_stream_node` 资源类型，仅包含查看和编辑两个操作
- 资源级别仅有「拥有者」一个用户组
- 项目级用户组权限分配参考 `env_node` 的模式
- 在 IAM 权限中心正确注册资源和操作
- 提供中英日三语国际化支持

### Non-Goals
- 不修改现有创作流 (creative_stream) 权限逻辑
- 不涉及创作流节点业务逻辑实现
- 不涉及前端 UI 变更
- 不包含 create / delete / execute / list 等操作（仅 view 和 edit）

## Decisions

### 1. 资源类型 ID 命名
- **Decision**: 使用 `creative_stream_node` (下划线分隔)
- **Rationale**: 与现有资源类型命名风格一致（如 `env_node`, `pipeline_group`）

### 2. 权限操作设计
- **Decision**: 仅包含 2 个操作：`creative_stream_node_view` 和 `creative_stream_node_edit`
- **Rationale**: 业务需求明确指出仅需查看和编辑两个操作

### 3. 权限依赖关系
- **Decision**: `creative_stream_node_edit` 依赖 `creative_stream_node_view`，`creative_stream_node_view` 依赖 `project_visit`
- **Rationale**: 遵循最小权限原则，编辑前需先有查看权限

### 4. 资源级用户组
- **Decision**: 仅设置 1 个「拥有者 (manager)」组，拥有 view + edit 权限
- **Rationale**: 业务需求明确指出只需要一个拥有者组

### 5. 项目级用户组权限分配（参考 env_node 模式）
- **Decision**: 参考 env_node 在各项目级组中的权限分配模式
- **Rationale**: 用户明确要求参考环境节点的项目级权限分配

| ID | 角色 | env_node 权限 | creative_stream_node 权限（类比） |
|----|------|---------------|-----------------------------------|
| 1 | 管理员 (manager) | create + view + edit + delete + list + use | view + edit |
| 2 | 开发人员 (developer) | create + view + list | view |
| 3 | 运维人员 (maintainer) | create + view + list | view |
| 4 | 产品人员 (pm) | list | (无) |
| 5 | 测试人员 (tester) | create + list | (无) |
| 6 | 质管人员 (qc) | (无) | (无) |
| 7 | 访客 (visitor) | (无) | (无) |

> 说明：创作流节点不含 create/delete/list/use 操作，因此参照 env_node 模式后：
> - 管理员：拥有 view + edit 全部权限
> - 开发人员/运维人员：仅拥有 view 权限（对应 env_node 中的 view）
> - 产品人员/测试人员/质管/访客：无权限（env_node 中 pm 仅有 list，tester 仅有 create+list，均无对应操作）

### 6. 数据库 ID 分配
- **Decision**: `T_AUTH_RESOURCE_TYPE` 使用 ID=23，`T_AUTH_RESOURCE_GROUP_CONFIG` 使用 ID=74
- **Rationale**: 现有最大值分别为 22 (creative_stream) 和 69 (creative_stream viewer)，分配下一个可用 ID

### 7. 权限分组归属
- **Decision**: 在 0006_group 中新增独立的「创作流节点」分组，与「创作流」同级
- **Rationale**: 创作流节点是独立的资源类型，应拥有独立的权限分组

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| IAM 配置文件格式错误导致权限注册失败 | 参考现有 env_node 配置，使用相同结构 |
| 数据库初始化脚本与现有数据冲突 | 使用 REPLACE INTO 语句，支持幂等执行 |
| 国际化翻译缺失导致界面显示异常 | 同时更新 zh_CN、en_US、ja_JP 三个文件 |

## Migration Plan

1. **开发阶段**: 按 tasks.md 顺序完成代码变更
2. **测试阶段**: 在测试环境验证权限注册和校验
3. **部署阶段**:
   - 执行数据库初始化脚本
   - 向 IAM 注册新资源类型和操作
   - 部署后端服务

### Rollback
- 删除 T_AUTH_RESOURCE_TYPE 和 T_AUTH_ACTION 中的新增记录
- 在 IAM 中删除 creative_stream_node 相关配置
- 回滚代码变更

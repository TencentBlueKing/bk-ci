## Context

BK-CI 权限系统基于 RBAC 模型，通过 IAM (权限中心) 进行权限管理。新增资源类型需要遵循严格的文件依赖顺序和命名规范。

### 约束条件
1. 必须按 0003 → 0004 → 0005 → 0006 → 0007 的顺序定义
2. 所有资源类型的父资源为 `project`
3. 操作 ID 格式: `{resource_type}_{action}`
4. 实例选择器 ID 格式: `{resource_type}_instance`

## Goals / Non-Goals

### Goals
- 新增 `creative_stream` 资源类型，权限模型与 `pipeline` 完全一致
- 支持创作流的完整权限控制（创建、查看、编辑、删除、执行等）
- 在 IAM 权限中心正确注册资源和操作
- 提供中英日三语国际化支持

### Non-Goals
- 不修改现有流水线权限逻辑
- 不涉及创作流业务逻辑实现
- 不涉及前端 UI 变更
- **不包含创作流组 (creative_stream_group) 资源类型** - 创作流没有流水线组的概念

## Decisions

### 1. 资源类型 ID 命名
- **Decision**: 使用 `creative_stream` (下划线分隔)
- **Rationale**: 与现有资源类型命名风格一致（如 `pipeline_group`, `env_node`）

### 2. 权限操作复用
- **Decision**: 复用 Pipeline 的 10 个操作类型
- **Rationale**: 需求明确指出权限模型与流水线一致
- **操作列表**: create, view, edit, delete, execute, list, share, download, manage, archive

### 3. 权限分组归属
- **Decision**: 在 0006_group 中新增独立的「创作流」分组
- **Rationale**: 创作流是独立业务模块，不应与流水线混在一起

### 4. 数据库 ID 分配
- **Decision**: 在 T_AUTH_RESOURCE_TYPE 中使用下一个可用 ID (需查询当前最大值)
- **Rationale**: 避免 ID 冲突

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| IAM 配置文件格式错误导致权限注册失败 | 参考现有 pipeline 配置，使用相同结构 |
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
- 在 IAM 中删除 creative_stream 相关配置
- 回滚代码变更

## Open Questions

1. 创作流是否需要支持「归档」操作？（假设需要，与 pipeline 保持一致）

## Clarifications

1. **创作流没有"组"的概念** - 与流水线不同，创作流不需要 `creative_stream_group` 资源类型。流水线有 `pipeline` 和 `pipeline_group` 两个资源类型，而创作流只有 `creative_stream` 一个资源类型。

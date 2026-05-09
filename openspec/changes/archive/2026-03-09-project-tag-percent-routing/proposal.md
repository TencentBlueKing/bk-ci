## Why

当前 `setTagByProject` 仅支持按单个 `projectId` 切换 consul tag，发布灰度版本后需要人工逐项目操作，发布效率低且回滚窗口长。  
需要提供按比例逐步放量的路由能力（如 1% -> 3% -> 5% -> 10% -> 100%），让灰度发布具备可控、可观测、可回退的节奏。

## What Changes

- 新增基于项目集合的百分比路由切换能力，将项目流量按配置比例从 `rbac` 迁移到 `rbac-gray`。
- 支持分阶段放量策略配置，允许按预设比例序列逐步推进到 100%。
- 支持强制路由白名单（持久化到 Redis）：指定项目无论哈希结果如何，必须被切换到目标 tag。
- 支持路由排除名单/黑名单（持久化到 Redis）：指定项目无论哈希结果如何，不参与本次放量。
- 提供白名单/黑名单独立管理接口（增删查），放量时自动读取 Redis 中的当前名单。
- 提供批量执行与幂等保障，避免重复执行导致项目路由异常。
- 提供阶段结果统计（目标项目数、实际切换数、失败数）与可追踪操作记录。
- 调整 `ProjectPercentageRoutingRequest`：`channelCode` 保持字符串类型，并将默认值设为 `BS`。
- 保留并兼容现有按单项目切换接口，作为应急修复与回滚补充手段。

## Capabilities

### New Capabilities
- `project-tag-percentage-routing`: 支持按项目百分比进行灰度路由放量与最终全量切换。

### Modified Capabilities
- 无

## Impact

- 影响模块：`project` 模块的运维接口与路由规则服务（`api-project-op`、`biz-project`）。
- 影响 API：新增运维侧批量放量接口与白名单/黑名单管理接口（增删查）；保留现有 `setTagByProject`。
- 影响请求模型：`ProjectPercentageRoutingRequest.channelCode` 保持字符串，未传时按默认值 `BS` 处理。
- 影响数据与状态：白名单/黑名单持久化到 Redis Set；放量执行结果通过接口响应返回。
- 影响运维流程：发布后由“逐项目手工切换”升级为“按比例阶段化切换”。

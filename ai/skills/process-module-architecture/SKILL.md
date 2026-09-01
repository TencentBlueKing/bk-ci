---
name: process-module-architecture
description: 处理 BK-CI Process 流水线核心模块时使用，例如流水线 CRUD、构建启动、运行时状态、引擎控制、事件驱动和数据库持久化。当用户要改流水线主链路而不是外围支撑模块时优先使用。
---

# Process 模块架构

## 适用场景

- 修改流水线创建、更新、删除和版本管理
- 处理构建启动、运行时状态和执行推进
- 修改引擎 Control、事件驱动或数据库持久化逻辑
- 排查流水线主链路中的核心行为

## 不适用场景

- 只是修改模板、YAML 转换或插件定义
- 只是修改调度、Agent、Worker 执行器
- 只是修改权限平台或支撑模块
- 只是修改某个外围服务的集成点

## 快速指导

1. 这个 skill 关注的是“流水线主链路如何被保存、触发、推进和结束”。
2. Process 是 BK-CI 的核心编排模块，读它时先分成入口层、服务层、引擎层、数据层。
3. 主入口只做模块路由，深入时直接进入现有参考文档：
   - API 层：`reference/1-api-layer.md`
   - Service 层：`reference/2-service-layer.md`
   - 引擎控制：`reference/3-engine-control.md`
   - DAO 与数据库：`reference/4-dao-database.md`
   - 事件驱动：`reference/5-event-driven.md`
4. 如果你改的是模型结构本身，联动看 `pipeline-model-architecture`。
5. 如果你改的是模板或 YAML 输入输出，联动看 `pipeline-template-module` 或 `yaml-pipeline-transfer`。
6. 如果你改的是执行资源派发或构建机侧执行，切到 `dispatch-module-architecture`、`agent-module-architecture`、`worker-module-architecture`。

## 高信号规则

- Process 的核心不是单个接口，而是“流水线数据 + 运行时状态 + 推进控制”的组合
- CRUD、构建启动、控制器、事件、持久化必须按一条主链去理解
- 主链路改动往往会联动权限、模型、模板、调度和日志
- 排查问题时先分清是定义期问题还是运行期问题

## 关键陷阱

- 只看某个 Resource 或 Service，不看整条主链
- 把模型问题、执行问题、事件问题混在一起
- 改了运行时状态流转，但没同步事件或持久化
- 把外围集成点的问题误判成 Process 内核问题

## 延伸阅读

- API 层：`reference/1-api-layer.md`
- Service 层：`reference/2-service-layer.md`
- 引擎控制：`reference/3-engine-control.md`
- DAO 与数据库：`reference/4-dao-database.md`
- 事件驱动：`reference/5-event-driven.md`
- 如果你在改流水线模型：再看 `pipeline-model-architecture`

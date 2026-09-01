---
name: 00-bkci-global-architecture
description: 用于跨模块开发、排查链路归属、判断某个需求应该落在哪个 BK-CI 模块，或需要快速理解流水线全链路协作时使用。单模块修改时优先读取对应模块 skill，而不是停留在这里。
---

# BK-CI 全局架构指南

## 适用场景

- 需求横跨多个模块，暂时不清楚该从哪里下手
- 需要理解流水线从触发到执行结束的主链路
- 需要判断某段逻辑属于 `Process`、`Dispatch`、`Agent`、`Worker` 还是其他模块
- 需要先选 skill，再深入具体实现

## 不适用场景

- 已经明确只改单个模块内部逻辑
- 只是前端页面、数据库脚本或某个工具函数的小改动
- 已经知道目标模块，只需要其局部规范

## 快速指导

1. 先用五层视图建立系统位置感：
   - 用户层：Web、OpenAPI、Webhook、第三方系统
   - 网关层：OpenResty，负责路由、认证、限流、服务发现
   - 微服务层：Project、Process、Auth、Repository、Store、Dispatch 等
   - 构建机层：Agent 负责进程与调度，Worker 负责任务执行
   - 资源层：MySQL、Redis、RabbitMQ、ES、文件存储
2. 流水线主链路按这个顺序理解：`触发 -> Process 编排 -> Dispatch 分配 -> Agent 领取 -> Worker 执行 -> 状态回传`
3. 先按模块边界选 skill，再深入实现：
   - 流水线模型、执行链路、状态机：`process-module-architecture`
   - 权限、RBAC、IAM：`auth-module-architecture`
   - 代码库、Webhook、PAC 接入：`repository-module-architecture`
   - 商店、模板、插件发布：`store-module-architecture`
   - 调度、构建资源、BuildLess：`dispatch-module-architecture`
   - 构建机 Agent：`agent-module-architecture`
   - Worker 执行器：`worker-module-architecture`
   - 后端 API 分层规范：`backend-microservice-development`
   - API 设计：`api-interface-design`
   - 数据库设计：`database-design`
4. 快速判断问题归属：
   - “谁来编排和推进流水线状态” -> `Process`
   - “执行资源怎么分配、容器怎么起” -> `Dispatch`
   - “构建机怎么常驻、怎么领任务、怎么升级” -> `Agent`
   - “插件任务怎么执行、怎么回传日志和状态” -> `Worker`
   - “接口该怎么分层、命名和落目录” -> `backend-microservice-development`
   - “表怎么设计、脚本怎么管理、索引怎么建” -> `database-design`

## 高信号规则

- 这个 skill 的职责是导航和归属判断，不是详细设计手册
- 对单模块问题，越快切到对应 skill，整体上下文效率越高
- 全局系统图和公共主链路以本 skill 为权威来源，不在各模块重复展开

## 关键陷阱

- 不要把这个 skill 当成详细设计文档。它只负责导航和主链路判断。
- 不要在每个模块 skill 中重复一遍全局系统图，公共全景以本 skill 为准。
- 对单模块修改，尽快切到对应模块 skill，避免把上下文浪费在全局说明上。

## 延伸阅读

- 单模块开发前，直接打开目标模块的 skill
- 需要具体实现细节时，不再继续扩展本文件，而是把内容放回对应模块 skill 或 `reference/`

---
name: agent-module-architecture
description: 处理 BK-CI Agent 构建机侧能力时使用，例如守护进程、心跳、Ask 轮询、任务拉起、升级更新和与 Dispatch/Worker 的协作。当用户要改构建机宿主侧行为而不是 Worker 执行细节时优先使用。
---

# Agent 模块架构

## 适用场景

- 修改 Agent 启动、守护和进程管理
- 处理心跳、Ask 轮询、任务拉起或任务结束流程
- 修改 Agent 升级、组件升级和构建机环境管理
- 排查 Agent 与 Dispatch、Worker 的交互问题

## 不适用场景

- 只是修改 Worker 内部的任务执行
- 只是修改插件定义、`task.json` 或插件发布
- 只是修改 Dispatch 的调度配额和队列策略
- 只是修改流水线业务逻辑

## 快速指导

1. 这个 skill 关注的是“构建机宿主侧如何保持在线、接任务、拉起执行器”。
2. Agent 主要解决守护、心跳、升级和任务编排，不负责插件本体定义。
3. 先按问题类型进入对应参考文档：
   - 运行形态与升级：`reference/1-runtime-upgrade.md`
   - Ask、心跳与任务拉起：`reference/2-ask-heartbeat-build.md`
   - 平台支持、运维与排查：`reference/3-platform-ops.md`
4. 如果问题已经进入任务执行细节，切到 `worker-module-architecture`。
5. 如果问题在构建机选择、排队或资源配额，切到 `dispatch-module-architecture`。

## 高信号规则

- Agent 是构建机侧编排者，Worker 是执行者
- 守护、心跳、Ask、升级是理解 Agent 的四个核心切面
- Agent 改动通常会影响稳定性、在线率和任务领取能力
- 涉及进程管理和升级时，要优先考虑跨平台差异

## 关键陷阱

- 把 Worker 细节问题误判成 Agent 问题
- 只看单次请求，不看守护和轮询全链路
- 升级逻辑没考虑回滚、并发和运行中任务
- 忽略 Linux / Windows / macOS 的行为差异

## 延伸阅读

- 运行形态与升级：`reference/1-runtime-upgrade.md`
- Ask、心跳与任务拉起：`reference/2-ask-heartbeat-build.md`
- 平台支持、运维与排查：`reference/3-platform-ops.md`
- 如果你在改 Worker 执行：再看 `worker-module-architecture`
- 如果你在改调度与构建机选择：再看 `dispatch-module-architecture`

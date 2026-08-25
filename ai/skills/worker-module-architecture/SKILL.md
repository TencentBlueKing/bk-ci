---
name: worker-module-architecture
description: 处理 BK-CI Worker 执行器时使用，例如任务领取与执行、插件运行、日志上报、环境变量传递和制品归档。当用户要改 Worker 执行链路而不是插件定义或调度策略时优先使用。
---

# Worker 模块架构

## 适用场景

- 修改 Worker 启动和执行流程
- 处理任务领取、任务工厂、任务执行器
- 排查插件在 Worker 内部的下载、执行和回传问题
- 修改日志上报、环境变量、归档和 API 调用
- 优化 Worker 生命周期或执行稳定性

## 不适用场景

- 只是开发插件本体或改 `task.json`
- 只是修改 Agent 守护、心跳和升级逻辑
- 只是修改 Dispatch 的调度、配额或队列策略
- 只是修改流水线模型定义

## 快速指导

1. 这个 skill 关注的是“任务到了构建机以后，Worker 如何真正执行”。
2. Worker 是执行器，不是调度器，也不是插件定义中心。
3. 先按问题类型进入对应参考文档：
   - 生命周期与执行主链路：`reference/1-runtime-flow.md`
   - 插件执行、日志与制品：`reference/2-task-plugin-log-artifact.md`
   - 环境变量、API 与排查：`reference/3-env-api-debug.md`
4. 如果现象是“插件定义不对”，切到 `pipeline-plugin-development`。
5. 如果现象是“根本没被派到这台机器执行”，切到 `dispatch-module-architecture` 或 `agent-module-architecture`。

## 高信号规则

- Worker 的主职责是执行，不负责资源调度决策
- 任务执行、日志、变量、归档是同一条运行链上的不同环节
- 插件问题和 Worker 问题经常混淆，先判断契约层还是执行层
- 执行链路改动要优先考虑幂等、失败恢复和日志可诊断性

## 关键陷阱

- 把调度问题误判成 Worker 问题
- 只改执行逻辑，不同步看变量、日志和结果回传
- 输出变量和日志混用，导致调试与契约边界不清
- 在任务工厂或执行器里堆过多分支，后续难扩展

## 延伸阅读

- 生命周期与执行主链路：`reference/1-runtime-flow.md`
- 插件执行、日志与制品：`reference/2-task-plugin-log-artifact.md`
- 环境变量、API 与排查：`reference/3-env-api-debug.md`
- 如果你在开发插件：再看 `pipeline-plugin-development`
- 如果你在改 Agent 守护或升级：再看 `agent-module-architecture`
- 如果你在改调度：再看 `dispatch-module-architecture`

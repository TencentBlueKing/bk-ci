---
name: dispatch-module-architecture
description: 处理 BK-CI 构建调度时使用，例如构建机选择、队列推进、配额控制、第三方构建机调度、Docker/Kubernetes 调度。当用户要改任务分配策略而不是 Agent/Worker 执行细节时优先使用。
---

# Dispatch 模块架构

## 适用场景

- 修改第三方构建机、Docker 或 Kubernetes 调度逻辑
- 处理队列、配额、资源分配和构建机选择
- 排查任务排队、派发失败或调度不均衡
- 扩展新的调度类型或构建资源类型

## 不适用场景

- 只是修改 Agent 守护、心跳或升级
- 只是修改 Worker 执行、日志或变量回传
- 只是开发插件或改 `task.json`
- 只是修改流水线业务编排

## 快速指导

1. 这个 skill 关注的是“任务如何被分配到合适的执行环境”。
2. Dispatch 的核心不是执行任务，而是做选择、排队、限流和推进。
3. 先按问题类型进入对应参考文档：
   - 模块结构与核心对象：`reference/1-dispatch-foundation.md`
   - 队列、配额与调度策略：`reference/2-queue-quota-scheduling.md`
   - 资源类型扩展与排查：`reference/3-resource-extension-debug.md`
4. 如果问题已经到达构建机并开始执行，切到 `agent-module-architecture` 或 `worker-module-architecture`。
5. 如果问题是插件本身不兼容运行环境，切到 `pipeline-plugin-development`。

## 高信号规则

- Dispatch 解决的是“在哪里跑”和“什么时候跑”
- 队列、配额、资源类型和监听推进是同一套调度系统的不同面
- 调度问题要先区分资源不足、策略不匹配还是执行侧异常
- 新调度类型落地时，数据模型、监听链路和资源约束要一起看

## 关键陷阱

- 把 Agent / Worker 执行失败误判成 Dispatch 问题
- 只看单个服务方法，不看监听、队列和配额的完整推进链
- 新增调度类型时只补入口，不补限流、状态流转和清理逻辑
- 调度问题排查只盯代码，不先确认资源池和配额状态

## 延伸阅读

- 模块结构与核心对象：`reference/1-dispatch-foundation.md`
- 队列、配额与调度策略：`reference/2-queue-quota-scheduling.md`
- 资源类型扩展与排查：`reference/3-resource-extension-debug.md`
- 如果你在改构建机宿主行为：再看 `agent-module-architecture`
- 如果你在改任务执行：再看 `worker-module-architecture`

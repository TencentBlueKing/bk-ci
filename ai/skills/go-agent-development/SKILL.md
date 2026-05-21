---
name: go-agent-development
description: 编写 BK-CI Go Agent 代码时使用，例如 Agent API 调用、任务处理、并发模式、错误处理、日志记录和宿主侧工具开发。当用户要写 Go 构建机侧代码而不是后端 Kotlin 服务时优先使用。
---

# Go Agent 开发

## 适用场景

- 编写或修改 Go Agent 代码
- 处理 Agent 与后端的 API 通信
- 实现任务处理、并发执行、错误恢复和日志记录
- 开发构建机侧辅助工具

## 不适用场景

- 后端 Kotlin / Java 微服务开发
- Dispatch 调度策略设计
- Worker 任务执行细节

## 快速指导

1. 这个 skill 关注的是 Go 侧开发习惯和实现方式，不替代 `agent-module-architecture` 的架构视角。
2. 写 Go Agent 代码时优先保证稳定性：错误处理、日志、重试、清理和并发边界要先想清楚。
3. 进程、任务、网络调用、goroutine 生命周期要一起看，不要只改局部函数。
4. 如果问题是 Agent 整体运行形态、Ask、升级或宿主侧链路，联动看 `agent-module-architecture`。
5. 如果问题是调度资源选择，联动看 `dispatch-module-architecture`。

## 高信号规则

- Go Agent 代码更偏宿主侧稳定性和长期运行能力
- 网络调用和后台 goroutine 一旦失控，影响通常比业务逻辑错误更大
- 日志要能帮助定位问题，而不是只留下表面现象

## 关键陷阱

- 只改 happy path，不补错误恢复和资源清理
- goroutine 启动容易，退出和异常恢复没管
- 把宿主侧问题和调度 / Worker 问题混在一起

## 延伸阅读

- 如果你在看 Agent 架构：再看 `agent-module-architecture`
- 如果你在看调度边界：再看 `dispatch-module-architecture`

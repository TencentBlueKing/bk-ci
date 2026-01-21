---
name: 00-bkci-global-architecture
description: BK-CI 全局架构指南，以流水线为核心的模块协作全景图，涵盖完整执行流程、模块依赖关系、数据流向、核心概念。当用户需要理解系统架构、进行跨模块开发、了解模块间协作或规划架构设计时优先阅读。
# 结构化元数据（支持智能压缩和渐进式加载）
core_modules:
  - Process (流水线核心)
  - Auth (权限认证)
  - Project (项目管理)
  - Store (研发商店)
  - Dispatch (构建调度)
related_skills:
  - process-module-architecture
  - auth-module-architecture
  - dispatch-module-architecture
  - worker-module-architecture
  - agent-module-architecture
token_estimate: 45000
---

# BK-CI 全局架构指南

<!-- ═══════════════════════════════════════════════════════════════════════════
     🚀 快速参考区（放在最前面 - 解决 Lost-in-Middle 问题）
     ═══════════════════════════════════════════════════════════════════════════ -->

## Quick Reference

### 系统分层（5 层）

```
用户层 → 网关层 → 微服务层 → 构建机层 → 资源层
```

### 核心模块速查

| 模块 | 职责 | 深入阅读 |
|------|------|----------|
| **Process** | 流水线编排与执行（核心） | `process-module-architecture` |
| **Auth** | 权限认证 RBAC | `auth-module-architecture` |
| **Dispatch** | 构建机分配调度 | `dispatch-module-architecture` |
| **Store** | 研发商店/插件管理 | `store-module-architecture` |
| **Worker** | 任务执行器 | `worker-module-architecture` |
| **Agent** | 构建机代理 (Go) | `agent-module-architecture` |

### 流水线执行核心流程

```
触发 → Process引擎 → Dispatch分配 → Agent领取 → Worker执行 → 状态回传
```

### 关键代码入口

| 功能 | 入口类 |
|------|--------|
| 手动构建 | `PipelineBuildFacadeService.buildManualStartup()` |
| 引擎调度 | `StageControl` / `ContainerControl` / `TaskControl` |
| Worker 主循环 | `Runner.loopPickup()` |
| 任务完成处理 | `PipelineBuildTaskService.finishTask()` |

---

## When to Use ✅

- 首次接触 BK-CI 项目，需要建立全局视图
- 进行**跨模块开发**，需要理解模块间协作
- 调试复杂问题，需要追踪完整调用链路
- 规划架构设计或重构方案
- 不确定某个功能应该在哪个模块实现

## When NOT to Use ❌

- 只修改单个模块内部逻辑（直接阅读对应模块 Skill）
- 简单的 Bug 修复（不涉及跨模块调用）
- 前端 UI 调整（阅读 `frontend-vue-development`）
- 数据库表变更（阅读 `database-script-management`）

---

## 一、系统分层架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           用户层 (User Layer)                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Web 前端   │  │  OpenAPI    │  │  Webhook    │  │  第三方系统集成     │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼────────────────┼────────────────────┼────────────┘
          ▼                ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         网关层 (Gateway Layer)                               │
│                    OpenResty (Nginx + Lua)                                   │
│  • 路由转发 • 身份认证 • 限流熔断 • 服务发现(Consul)                         │
└─────────────────────────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       微服务层 (Microservice Layer)                          │
│  核心: Project | Process | Auth | Store | Repository                         │
│  调度: Dispatch | Environment | Ticket                                       │
│  支撑: Artifactory | Log | Notify | Quality | Metrics                        │
│  开放: OpenAPI | WebSocket                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        构建机层 (Build Machine Layer)                        │
│     Agent (Go) ──▶ Worker (Kotlin)                                           │
│     进程守护        任务执行、插件运行、日志上报                              │
└─────────────────────────────────────────────────────────────────────────────┘
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        资源层 (Resource Layer)                               │
│     MySQL | Redis | RabbitMQ | ElasticSearch | 文件存储                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 模块职责速查表

| 模块 | 核心职责 | 关键能力 |
|------|----------|----------|
| **Project** | 项目管理 | 项目创建、成员管理、服务开通、路由分片 |
| **Process** | 流水线编排与执行 | 模型定义、构建调度、事件驱动、状态管理 |
| **Auth** | 权限认证 | RBAC、资源授权、用户组、OAuth2 |
| **Store** | 研发商店 | 插件管理、模板市场、版本发布、统计分析 |
| **Repository** | 代码库管理 | Git/SVN/P4 对接、Webhook、PAC |
| **Dispatch** | 构建调度 | 构建机分配、Docker 调度、配额管理 |
| **Environment** | 构建机环境 | 节点管理、Agent 安装、环境变量 |
| **Ticket** | 凭证管理 | 密码/SSH/Token 存储、加密解密 |
| **Artifactory** | 制品库 | 文件存储、版本管理、跨项目共享 |
| **Log** | 日志服务 | 构建日志收集、存储、检索 |
| **Notify** | 通知服务 | 邮件/企微/RTX 通知 |
| **Quality** | 质量红线 | 指标定义、准入准出、拦截规则 |
| **Agent** | 构建机代理 | 进程管理、任务调度、自动升级 |
| **Worker** | 任务执行器 | 插件执行、脚本运行、日志上报 |

---

## 二、流水线执行全流程（核心）

> **这是 BK-CI 最核心的流程，涉及几乎所有模块的协作。**

### 2.1 执行流程总览图

```
用户触发
    │
    ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ PipelineBuild   │────▶│ PipelineBuild   │────▶│ PipelineRuntime │
│ FacadeService   │     │ Service         │     │ Service         │
│ (权限校验)      │     │ (拦截器链)      │     │ (创建记录)      │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                                                         ▼
                                              ┌─────────────────────┐
                                              │ PipelineBuildStart  │
                                              │ Event → RabbitMQ    │
                                              └──────────┬──────────┘
                                                         │
    ┌────────────────────────────────────────────────────┘
    ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ BuildStart      │────▶│ StageControl    │────▶│ Container       │
│ Control         │     │ (责任链)        │     │ Control         │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                              ┌───────────────────────────┤
                              ▼                           ▼
                   ┌─────────────────┐         ┌─────────────────┐
                   │ Dispatch        │         │ TaskControl     │
                   │ (构建机分配)    │         │ (引擎侧Task)    │
                   └────────┬────────┘         └─────────────────┘
                            ▼
                   ┌─────────────────┐
                   │ Agent (Go)      │
                   │ (领取任务)      │
                   └────────┬────────┘
                            ▼
                   ┌─────────────────┐
                   │ Worker (Kotlin) │
                   │ (执行Task)      │
                   └────────┬────────┘
                            │ completeTask API
                            ▼
                   ┌─────────────────┐
                   │ 状态逐层回传    │
                   │ Task→Job→Stage  │
                   │ →Pipeline       │
                   └────────┬────────┘
                            ▼
                   ┌─────────────────┐
                   │ BuildEndControl │
                   │ (完成处理)      │
                   └────────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Notify    │     │   Metrics   │     │  WebSocket  │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 2.2 四个阶段详解

#### 阶段一：触发构建

| 触发类型 | 入口 | 说明 |
|----------|------|------|
| 手动触发 | Web/API | 用户点击"执行"按钮 |
| 定时触发 | Cron | `TimerTriggerElement` 定时调度 |
| 代码变更 | Webhook | Git Push/MR/Tag 事件 |
| 远程触发 | OpenAPI | 第三方系统调用 |

**核心入口**：`PipelineBuildFacadeService.buildManualStartup()`

#### 阶段二：引擎调度

引擎通过**责任链模式**和**事件驱动**，按 Stage → Job → Task 层级调度。

**Stage 责任链**（StageControl）:
1. `CheckInterruptStageCmd` → 检查快速失败
2. `CheckConditionalSkipStageCmd` → 条件跳过
3. `CheckPauseReviewStageCmd` → 暂停/审核
4. `StartContainerStageCmd` → **下发 Container 事件**
5. `UpdateStateForStageCmdFinally` → 更新状态

**Container 责任链**（ContainerControl）:
1. `CheckDependOnContainerCmd` → Job 依赖检查
2. `CheckMutexContainerCmd` → 互斥组检查
3. `StartActionTaskContainerCmd` → **启动 Task 执行**

#### 阶段三：任务执行

**Agent (Go)** - 构建机代理：
```go
for {
    result := api.AskBuild()  // 向 Dispatch 请求任务
    if result.HasBuild {
        go runBuild(result.BuildInfo)  // 异步执行
    }
    time.Sleep(interval)
}
```

**Worker (Kotlin)** - 任务执行器：
```kotlin
loop@ while (true) {
    val buildTask = EngineService.claimTask()  // 领取任务
    when (buildTask.status) {
        DO -> {
            val task = TaskFactory.create(buildTask.type)
            task.run(buildTask, buildVariables, workspace)
            EngineService.completeTask(result)  // 上报结果
        }
        WAIT -> Thread.sleep(sleepMills)
        END -> break@loop
    }
}
```

#### 阶段四：状态回传

```
Task 完成 → PipelineBuildTaskService.finishTask()
         → 发送 PipelineBuildContainerEvent
         → Job 完成检查 → Stage 完成检查 → Pipeline 完成
         → Notify/Metrics/WebSocket
```

---

## 三、Job 执行机制（重要）

### 3.1 系统 Task

**重要概念**：启动构建机本身就是一个 Task！

```
┌─────────────────────────────────────────────────────────────────┐
│           T_PIPELINE_BUILD_TASK 表数据示例                       │
├──────────────┬────────────────────┬──────────────────┬──────────┤
│ TASK_ID      │ TASK_NAME          │ TASK_ATOM        │ TASK_SEQ │
├──────────────┼────────────────────┼──────────────────┼──────────┤
│ startVM-1    │ Prepare_Job#1      │ dispatchVMStart  │ 1  系统  │
│ e-xxx-1      │ Bash               │ (空)             │ 2  用户  │
│ e-xxx-2      │ 代码拉取           │ (空)             │ 3  用户  │
│ end-1000     │ Wait_Finish_Job#1  │ (空)             │ 1000 系统│
│ stopVM-1001  │ Clean_Job#1        │ dispatchVMStop   │ 1001 系统│
└──────────────┴────────────────────┴──────────────────┴──────────┘
```

### 3.2 Worker 与服务端通信

| 接口 | 说明 |
|------|------|
| `jobStarted` | Worker 启动后调用，返回 BuildVariables |
| `claimTask` | 领取待执行 Task |
| `completeTask` | 上报 Task 结果，触发下一个 Task 调度 |
| `heartbeat` | 心跳保活 |
| `jobEnd` | Job 完成，触发资源释放 |

### 3.3 非阻塞事件驱动

```
BK-CI 事件驱动方式（✅ 采用）：

Process 引擎
    │
    ├── 发送调度请求（PipelineAgentStartupEvent）
    ├── 立即返回，处理其他事件（其他流水线、其他 Job）
    │
... 时间流逝，构建机启动中 ...
    │
Worker 启动后
    ├── 调用 jobStarted API
    ├── 服务端发送 PipelineBuildContainerEvent
    │
引擎收到事件
    └── 继续执行该 Job 的后续 Task
```

**优势**：单个引擎实例可并发处理大量构建，不阻塞。

---

## 四、服务依赖关系

```
                              ┌──────────┐
                              │ Project  │ ◀──── 所有服务的基础依赖
                              └────┬─────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
    ┌──────────┐             ┌──────────┐             ┌──────────┐
    │   Auth   │◀────────────│ Process  │────────────▶│  Store   │
    └────┬─────┘             └────┬─────┘             └──────────┘
         │           ┌────────────┼────────────┐
         │           ▼            ▼            ▼
         │     ┌──────────┐ ┌──────────┐ ┌──────────┐
         │     │Repository│ │ Dispatch │ │  Ticket  │
         │     └──────────┘ └────┬─────┘ └──────────┘
         │                       │
         │                       ▼
         │                ┌──────────┐
         │                │Environment│
         │                └────┬─────┘
         │                     ▼
         │              ┌────────────┐
         │              │Agent/Worker│
         │              └─────┬──────┘
         │                    ▼
         │     ┌──────────────────────────────────┐
         │     │  Log | Artifactory | Notify     │
         │     └──────────────────────────────────┘
         │                    │
         └───────────────────▶│
                              ▼
                       ┌──────────────┐
                       │   Metrics    │
                       └──────────────┘
```

---

## 五、核心事件类型

| 事件类型 | 触发时机 | 处理器 |
|----------|----------|--------|
| `PipelineBuildStartEvent` | 构建启动 | StageControl |
| `PipelineBuildStageEvent` | Stage 状态变更 | StageControl |
| `PipelineBuildContainerEvent` | Job 状态变更 | ContainerControl |
| `PipelineBuildTaskFinishEvent` | 任务完成 | TaskControl |
| `PipelineBuildFinishEvent` | 构建完成 | BuildFinishListener |
| `PipelineBuildCancelEvent` | 构建取消 | CancelControl |
| `PipelineAgentStartupEvent` | 构建机启动 | DispatchListener |

---

## 六、流水线模型结构

```
Model (流水线模型)
├── name: String
├── stages: List<Stage>
│   └── Stage
│       ├── id: String (s-xxx)
│       ├── checkIn/checkOut: StagePauseCheck
│       └── containers: List<Container>
│           └── Container (Job)
│               ├── id: String (c-xxx)
│               ├── @type: vmBuild | normal | trigger
│               ├── dispatchType: DispatchType
│               └── elements: List<Element>
│                   └── Element (Task)
│                       ├── id: String (t-xxx)
│                       ├── @type: marketBuild | linuxScript | ...
│                       └── data: Map
├── triggers: List<Trigger>
├── params: List<BuildFormProperty>
└── setting: PipelineSetting
```

---

## 七、数据库核心表

```
T_PROJECT
    │
    ├── T_PIPELINE_INFO (流水线信息)
    │       │
    │       ├── T_PIPELINE_RESOURCE (编排模型)
    │       ├── T_PIPELINE_SETTING (配置)
    │       └── T_PIPELINE_BUILD_HISTORY (构建历史)
    │               │
    │               ├── T_PIPELINE_BUILD_STAGE
    │               ├── T_PIPELINE_BUILD_CONTAINER
    │               ├── T_PIPELINE_BUILD_TASK
    │               └── T_PIPELINE_BUILD_VAR
    │
    ├── T_REPOSITORY (代码库)
    └── T_AUTH_RESOURCE (权限资源)
```

---

## 八、技术栈

| 层次 | 技术 |
|------|------|
| **后端** | Kotlin/Java, Spring Boot 3, Gradle |
| **数据库** | MySQL + JOOQ |
| **缓存/锁** | Redis |
| **消息队列** | RabbitMQ |
| **日志检索** | ElasticSearch |
| **服务发现** | Consul |
| **前端** | Vue 2.7, Vuex, bk-magic-vue |
| **Agent** | Go 1.19+ |
| **Worker** | Kotlin (JVM) |

---

## 九、Skill 导航索引

### 按模块查找

| 模块 | Skill |
|------|-------|
| 全局架构 | 00-bkci-global-architecture (本文档) |
| 后端开发 | backend-microservice-development |
| API 设计 | api-interface-design |
| 单元测试 | unit-testing |
| 前端开发 | frontend-vue-development |
| Agent 开发 | go-agent-development, agent-module-architecture |
| 数据库 | database-script-management, database-design |
| Process 模块 | pipeline-model-architecture, process-module-architecture 系列 |
| Auth 模块 | auth-module-architecture |
| Store 模块 | store-module-architecture |
| Dispatch 模块 | dispatch-module-architecture |
| Worker 模块 | worker-module-architecture |

### 按场景查找

| 场景 | 涉及 Skill |
|------|------------|
| 新增流水线功能 | pipeline-model-architecture, process-module-architecture 系列, event-driven-architecture |
| 开发新插件 | store-module-architecture, worker-module-architecture |
| 修改权限逻辑 | auth-module-architecture |
| 优化构建调度 | dispatch-module-architecture, environment-module-architecture, agent-module-architecture |
| 数据库表变更 | database-script-management, database-design |

---

<!-- ═══════════════════════════════════════════════════════════════════════════
     🎯 决策清单（放在最后 - 强化记忆）
     ═══════════════════════════════════════════════════════════════════════════ -->

## Checklist

开始跨模块开发前，请确认：

- [ ] **已理解流水线执行流程**：触发 → 调度 → 执行 → 回传
- [ ] **已确定涉及的模块**：Process? Dispatch? Store?
- [ ] **已阅读相关模块 Skill**：如 `process-module-architecture`
- [ ] **已了解事件驱动机制**：通过 RabbitMQ 异步通信
- [ ] **已确认数据流向**：哪些表会被读写？

### 核心理解要点

1. **Process 是核心** - 流水线编排、构建调度、状态管理都在这里
2. **事件驱动** - 通过 RabbitMQ 实现模块间解耦和异步处理
3. **双层构建** - Agent(Go) 负责进程管理，Worker(Kotlin) 负责任务执行
4. **Worker Pull 模式** - Worker 主动 `claimTask` 领取任务
5. **非阻塞等待** - 引擎发出调度请求后不阻塞，通过事件回调继续

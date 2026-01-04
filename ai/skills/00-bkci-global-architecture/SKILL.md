---
name: 00-bkci-global-architecture
description: BK-CI 全局架构指南，以流水线为核心的模块协作全景图，涵盖完整执行流程、模块依赖关系、数据流向、核心概念。当用户需要理解系统架构、进行跨模块开发、了解模块间协作或规划架构设计时优先阅读。
---

# BK-CI 全局架构指南

> **核心理念**: 以流水线（Pipeline）为核心，理解 BK-CI 所有模块如何协同工作，完成从代码提交到制品交付的完整 CI/CD 流程。

## 一、全局架构概览

### 1.1 系统分层架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           用户层 (User Layer)                                │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐ │
│  │  Web 前端   │  │  OpenAPI    │  │  Webhook    │  │  第三方系统集成     │ │
│  │  (14个应用) │  │  (开放接口) │  │  (代码触发) │  │  (蓝鲸/企业微信等) │ │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └──────────┬──────────┘ │
└─────────┼────────────────┼────────────────┼────────────────────┼────────────┘
          │                │                │                    │
          ▼                ▼                ▼                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         网关层 (Gateway Layer)                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                    OpenResty (Nginx + Lua)                              ││
│  │  • 路由转发 • 身份认证 • 限流熔断 • 服务发现(Consul)                    ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                       微服务层 (Microservice Layer)                          │
│                                                                              │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                        核心服务 (Core Services)                         ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ││
│  │  │ Project  │  │ Process  │  │   Auth   │  │  Store   │  │Repository│  ││
│  │  │ 项目管理 │  │ 流水线   │  │ 权限认证 │  │ 研发商店 │  │ 代码库   │  ││
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                        调度服务 (Dispatch Services)                     ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐                              ││
│  │  │ Dispatch │  │Environment│ │  Ticket  │                              ││
│  │  │ 构建调度 │  │ 构建机   │  │ 凭证管理 │                              ││
│  │  └──────────┘  └──────────┘  └──────────┘                              ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                        支撑服务 (Support Services)                      ││
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ││
│  │  │Artifactory│ │   Log    │  │  Notify  │  │ Quality  │  │ Metrics  │  ││
│  │  │ 制品库   │  │ 日志服务 │  │ 通知服务 │  │ 质量红线 │  │ 度量统计 │  ││
│  │  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘  ││
│  └─────────────────────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │                        开放服务 (Open Services)                         ││
│  │  ┌──────────┐  ┌──────────┐                                            ││
│  │  │ OpenAPI  │  │WebSocket │                                            ││
│  │  │ 开放接口 │  │ 实时推送 │                                            ││
│  │  └──────────┘  └──────────┘                                            ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        构建机层 (Build Machine Layer)                        │
│  ┌─────────────────────────────────────────────────────────────────────────┐│
│  │  ┌──────────────────────┐      ┌──────────────────────┐                ││
│  │  │    Agent (Go)        │      │    Worker (Kotlin)   │                ││
│  │  │  • 进程守护          │ ───▶ │  • 任务执行          │                ││
│  │  │  • 任务调度          │      │  • 插件运行          │                ││
│  │  │  • 自动升级          │      │  • 日志上报          │                ││
│  │  └──────────────────────┘      └──────────────────────┘                ││
│  └─────────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                        资源层 (Resource Layer)                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │  MySQL   │  │  Redis   │  │ RabbitMQ │  │   ES     │  │ 文件存储 │      │
│  │ 关系数据 │  │ 缓存/锁  │  │ 消息队列 │  │ 日志检索 │  │ 制品存储 │      │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  └──────────┘      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 1.2 模块职责速查表

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
| **Metrics** | 度量统计 | 构建统计、趋势分析、报表 |
| **OpenAPI** | 开放接口 | 第三方集成、API 网关 |
| **Agent** | 构建机代理 | 进程管理、任务调度、自动升级 |
| **Worker** | 任务执行器 | 插件执行、脚本运行、日志上报 |

## 二、流水线生命周期全景

### 2.1 流水线创建流程

```
用户创建流水线
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. 前端 (devops-pipeline)                                                    │
│    • 流水线编辑器 (Pipeline Editor)                                          │
│    • 可视化编排 Stage → Job → Task                                           │
│    • 选择插件 (从 Store 获取)                                                │
│    • 配置参数、触发器、通知                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
      │ 提交 Model JSON
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. Process 服务 (API 层)                                                     │
│    • PipelineResource.create() 接收请求                                      │
│    • 参数校验、权限检查 (调用 Auth)                                          │
│    • 项目校验 (调用 Project)                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. Process 服务 (Service 层)                                                 │
│    • PipelineRepositoryService.deployPipeline()                              │
│    • 解析 Model，校验插件 (调用 Store)                                       │
│    • 校验代码库 (调用 Repository)                                            │
│    • 校验凭证 (调用 Ticket)                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. Process 服务 (DAO 层)                                                     │
│    • 保存 T_PIPELINE_INFO (基本信息)                                         │
│    • 保存 T_PIPELINE_RESOURCE (编排模型)                                     │
│    • 保存 T_PIPELINE_SETTING (配置)                                          │
│    • 初始化 T_PIPELINE_BUILD_SUMMARY (构建摘要)                              │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 5. Auth 服务                                                                 │
│    • 创建流水线资源权限                                                      │
│    • 授予创建者管理员权限                                                    │
│    • 同步到 IAM (蓝鲸权限中心)                                               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 流水线执行全流程

这是 BK-CI 最核心的流程，涉及几乎所有模块的协作。下面从**触发 → 调度 → 执行 → 完成**四个阶段详细说明。

---

#### 2.2.1 阶段一：触发构建

**触发方式**

| 触发类型 | 入口 | 说明 |
|----------|------|------|
| 手动触发 | Web/API | 用户点击"执行"按钮 |
| 定时触发 | Cron | `TimerTriggerElement` 定时调度 |
| 代码变更触发 | Webhook | Git Push/MR/Tag 事件 |
| 远程触发 | OpenAPI | 第三方系统调用 |
| 子流水线调用 | Sub-Pipeline | 父流水线调用子流水线 |

**核心处理流程**

```
用户/系统触发
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. API 层入口                                                                │
│    PipelineBuildFacadeService.buildManualStartup()                          │
│    源码: biz-process/.../service/builds/PipelineBuildFacadeService.kt:390   │
│                                                                              │
│    主要职责:                                                                 │
│    ├── 权限校验: pipelinePermissionService.validPipelinePermission()        │
│    │   → 检查用户是否有 EXECUTE 权限                                        │
│    ├── 流水线校验: 检查是否锁定、版本是否最新                               │
│    ├── 触发器校验: 检查 ManualTriggerElement 是否启用                       │
│    ├── 参数解析: buildParamCompatibilityTransformer.parseTriggerParam()     │
│    └── 调用: pipelineBuildService.startPipeline()                           │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. 构建服务层                                                                │
│    PipelineBuildService.startPipeline()                                      │
│    源码: biz-base/.../service/pipeline/PipelineBuildService.kt:127          │
│                                                                              │
│    主要职责:                                                                 │
│    ├── 频率限制: simpleRateLimiter.acquire() 防止并发过高                   │
│    ├── 拦截器链: pipelineInterceptorChain.filter() 执行前置检查             │
│    │   └── 包括: 排队检查、并发组检查、配额检查等                           │
│    ├── 构建记录创建: pipelineRuntimeService.startBuild()                    │
│    │   ├── T_PIPELINE_BUILD_HISTORY (构建历史)                              │
│    │   ├── T_PIPELINE_BUILD_STAGE (Stage 状态)                              │
│    │   ├── T_PIPELINE_BUILD_CONTAINER (Job 状态)                            │
│    │   ├── T_PIPELINE_BUILD_TASK (Task 状态)                                │
│    │   └── T_PIPELINE_BUILD_VAR (构建变量)                                  │
│    └── 发送启动事件: PipelineBuildStartEvent → RabbitMQ                     │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
    RabbitMQ 消息队列
```

**关键源码位置**

| 功能 | 类名 | 文件路径 |
|------|------|----------|
| 手动构建入口 | `PipelineBuildFacadeService` | `biz-process/.../service/builds/PipelineBuildFacadeService.kt` |
| 构建启动核心 | `PipelineBuildService` | `biz-base/.../service/pipeline/PipelineBuildService.kt` |
| 运行时服务 | `PipelineRuntimeService` | `biz-base/.../engine/service/PipelineRuntimeService.kt` |
| Webhook 触发 | `WebhookTriggerBuildService` | `biz-process/.../trigger/scm/WebhookTriggerBuildService.kt` |

---

#### 2.2.2 阶段二：引擎调度（核心）

构建启动后，引擎通过**责任链模式**和**事件驱动**机制，按 Stage → Job → Task 层级调度执行。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. 构建启动控制器                                                            │
│    BuildStartControl.handle(PipelineBuildStartEvent)                         │
│    源码: biz-engine/.../control/BuildStartControl.kt:140                    │
│                                                                              │
│    主要职责:                                                                 │
│    ├── 获取锁: BuildIdLock 防止重复处理                                     │
│    ├── 状态检查: 确认构建未结束、未取消                                     │
│    ├── 构建模型初始化: buildModel() 准备执行上下文                          │
│    └── 发送 Stage 事件: PipelineBuildStageEvent → 第一个 Stage              │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 4. Stage 控制器（责任链模式）                                                │
│    StageControl.handle(PipelineBuildStageEvent)                              │
│    源码: biz-engine/.../control/StageControl.kt:93                          │
│                                                                              │
│    责任链命令（按顺序执行）:                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │ 1. CheckInterruptStageCmd      → 检查是否快速失败/中断              │  │
│    │ 2. CheckConditionalSkipStageCmd → 检查 Stage 条件跳过               │  │
│    │ 3. CheckPauseReviewStageCmd    → 检查 Stage 暂停/审核               │  │
│    │ 4. StartContainerStageCmd      → 下发 Container 事件（核心）        │  │
│    │ 5. UpdateStateForStageCmdFinally → 更新 Stage 状态                  │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│    StartContainerStageCmd 核心逻辑:                                          │
│    源码: biz-engine/.../command/stage/impl/StartContainerStageCmd.kt:68     │
│    ├── 遍历 Stage 下所有 Container（并行下发）                              │
│    ├── 发送 PipelineBuildContainerEvent 到每个 Job                          │
│    └── 同一 Stage 的多个 Job 并发执行                                       │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 并发下发多个 PipelineBuildContainerEvent
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 5. Container/Job 控制器（责任链模式）                                        │
│    ContainerControl.handle(PipelineBuildContainerEvent)                      │
│    源码: biz-engine/.../control/ContainerControl.kt:100                     │
│                                                                              │
│    责任链命令（按顺序执行）:                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │ 1. CheckDependOnContainerCmd       → 检查 Job 依赖                  │  │
│    │ 2. CheckConditionalSkipContainerCmd → 检查 Job 条件跳过             │  │
│    │ 3. CheckPauseContainerCmd          → 检查 Job 暂停                  │  │
│    │ 4. CheckMutexContainerCmd          → 检查 Job 互斥组                │  │
│    │ 5. CheckDispatchQueueContainerCmd  → 检查全局 Job 并发队列          │  │
│    │ 6. InitializeMatrixGroupStageCmd   → 矩阵运算生成 Container         │  │
│    │ 7. MatrixExecuteContainerCmd       → 矩阵执行                       │  │
│    │ 8. AgentReuseMutexCmd              → Agent 复用互斥                 │  │
│    │ 9. StartActionTaskContainerCmd     → 启动 Task 执行（核心）         │  │
│    │ 10. ContainerCmdLoop               → 循环消息处理                   │  │
│    │ 11. UpdateStateContainerCmdFinally → 更新 Job 状态                  │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│    Job 类型判断:                                                             │
│    ├── VMBuildContainer: 需要构建机 → 执行 startVM Task → 调用 Dispatch    │
│    ├── NormalContainer: 无编译环境 → 直接在引擎侧执行                       │
│    └── TriggerContainer: 触发器 → 特殊处理                                  │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 需要构建机时，执行 startVM Task
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 6. Dispatch 服务 - 构建机分配                                                │
│    DispatchService.dispatch()                                                │
│    源码: biz-dispatch/.../service/DispatchService.kt                        │
│                                                                              │
│    调度策略:                                                                 │
│    ├── 公共构建机 (DevCloud): 云端容器                                      │
│    ├── 第三方构建机 (ThirdParty): 用户自有机器                              │
│    └── Docker 构建 (Kubernetes): K8s Pod                                    │
│                                                                              │
│    调度流程:                                                                 │
│    ├── 检查配额: T_DISPATCH_QUOTA_PROJECT                                   │
│    ├── 查询可用构建机: Environment 服务                                     │
│    │   └── T_ENVIRONMENT_THIRDPARTY_AGENT                                   │
│    ├── 创建调度记录: T_DISPATCH_THIRDPARTY_AGENT_BUILD                      │
│    └── 等待 Agent 领取任务                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

**关键源码位置**

| 功能 | 类名 | 文件路径 |
|------|------|----------|
| 构建启动控制 | `BuildStartControl` | `biz-engine/.../control/BuildStartControl.kt` |
| Stage 控制 | `StageControl` | `biz-engine/.../control/StageControl.kt` |
| Container 控制 | `ContainerControl` | `biz-engine/.../control/ContainerControl.kt` |
| 下发 Container 事件 | `StartContainerStageCmd` | `biz-engine/.../command/stage/impl/StartContainerStageCmd.kt` |
| 启动 Task 执行 | `StartActionTaskContainerCmd` | `biz-engine/.../command/container/impl/StartActionTaskContainerCmd.kt` |

---

#### 2.2.3 阶段三：任务执行

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 7. Agent (Go) - 构建机代理                                                   │
│    源码: src/agent/agent/src/pkg/job/build.go                               │
│                                                                              │
│    主循环 (Ask Loop):                                                        │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │  for {                                                              │  │
│    │      result := api.AskBuild()  // 向 Dispatch 请求任务              │  │
│    │      if result.HasBuild {                                           │  │
│    │          go runBuild(result.BuildInfo)  // 异步执行                 │  │
│    │      }                                                              │  │
│    │      time.Sleep(interval)  // 轮询间隔                              │  │
│    │  }                                                                  │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│    runBuild() 流程:                                                          │
│    ├── 1. 准备工作空间: 创建/清理工作目录                                   │
│    ├── 2. 设置环境变量: PROJECT_ID, PIPELINE_ID, BUILD_ID 等               │
│    ├── 3. 启动 Worker: java -jar worker.jar                                │
│    ├── 4. 监控 Worker: 等待进程结束                                         │
│    └── 5. 清理工作空间: 可选                                                │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 8. Worker (Kotlin) - 任务执行器                                              │
│    源码: src/worker/worker-common/src/main/kotlin/.../Runner.kt             │
│                                                                              │
│    启动流程:                                                                 │
│    ┌─────────────────────────────────────────────────────────────────────┐  │
│    │  1. 初始化                                                          │  │
│    │     ├── 启动心跳线程 (HeartbeatTask)                                │  │
│    │     ├── 初始化日志服务 (LoggerService)                              │  │
│    │     └── 调用 jobStarted API 通知服务端就绪                          │  │
│    │                                                                     │  │
│    │  2. 任务循环 (loopPickup)                                           │  │
│    │     loop@ while (true) {                                            │  │
│    │         val buildTask = EngineService.claimTask()  // 领取任务      │  │
│    │         when (buildTask.status) {                                   │  │
│    │             DO -> {                                                 │  │
│    │                 val task = TaskFactory.create(buildTask.type)       │  │
│    │                 task.run(buildTask, buildVariables, workspace)      │  │
│    │                 EngineService.completeTask(result)  // 上报结果     │  │
│    │             }                                                       │  │
│    │             WAIT -> Thread.sleep(sleepMills)  // 暂无任务           │  │
│    │             END -> break@loop  // Job 结束                          │  │
│    │         }                                                           │  │
│    │     }                                                               │  │
│    │                                                                     │  │
│    │  3. 完成构建                                                        │  │
│    │     └── EngineService.endBuild()                                    │  │
│    └─────────────────────────────────────────────────────────────────────┘  │
│                                                                              │
│    Task 类型:                                                                │
│    ├── MarketAtomTask: 研发商店插件                                         │
│    ├── LinuxScriptTask: Shell 脚本                                          │
│    ├── WindowsScriptTask: Bat 脚本                                          │
│    ├── CodeGitPullTask: 代码拉取                                            │
│    └── ArchiveFileTask: 制品归档                                            │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      │ 每个 Task 完成后调用 completeTask API
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 9. Task 控制器（引擎侧 Task 执行）                                           │
│    TaskControl.handle(PipelineBuildAtomTaskEvent)                            │
│    源码: biz-engine/.../control/TaskControl.kt:82                           │
│                                                                              │
│    说明: 此控制器处理引擎侧执行的 Task（如人工审核、质量红线等）             │
│    对于 Worker 侧执行的 Task，由 Worker 直接调用 completeTask API            │
│                                                                              │
│    主要职责:                                                                 │
│    ├── 状态检查: 确认 Task 未完成                                           │
│    ├── 区分执行位置: runByVmTask() 判断是否在构建机执行                     │
│    ├── 引擎侧执行: taskAtomService.start()/tryFinish()                      │
│    └── 结果处理: pipelineBuildTaskService.finishTask()                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

**关键源码位置**

| 功能 | 类名 | 文件路径 |
|------|------|----------|
| Agent 主程序 | `build.go` | `src/agent/agent/src/pkg/job/build.go` |
| Worker 主循环 | `Runner` | `src/worker/worker-common/.../Runner.kt` |
| Task 控制器 | `TaskControl` | `biz-engine/.../control/TaskControl.kt` |
| Task 完成处理 | `PipelineBuildTaskService` | `biz-base/.../service/PipelineBuildTaskService.kt` |

---

#### 2.2.4 阶段四：结果处理与状态回传

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 10. Task 完成 → 触发 Container 事件                                          │
│     PipelineBuildTaskService.finishTask()                                    │
│     源码: biz-base/.../service/PipelineBuildTaskService.kt                  │
│                                                                              │
│     处理流程:                                                                │
│     ├── 更新 Task 状态: T_PIPELINE_BUILD_TASK                               │
│     ├── 保存构建变量: 插件输出变量                                          │
│     └── 发送事件: PipelineBuildContainerEvent → 触发下一个 Task 调度        │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 11. Container/Job 完成 → 触发 Stage 事件                                     │
│     ContainerControl → UpdateStateContainerCmdFinally                        │
│                                                                              │
│     处理流程:                                                                │
│     ├── 检查所有 Task 是否完成                                              │
│     ├── 更新 Container 状态: T_PIPELINE_BUILD_CONTAINER                     │
│     └── 发送事件: PipelineBuildStageEvent → 检查 Stage 是否完成             │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 12. Stage 完成 → 触发下一 Stage 或流水线完成                                 │
│     StageControl → UpdateStateForStageCmdFinally                             │
│                                                                              │
│     处理流程:                                                                │
│     ├── 检查所有 Job 是否完成                                               │
│     ├── Quality: 检查准出红线 (Check-Out)                                   │
│     ├── 更新 Stage 状态: T_PIPELINE_BUILD_STAGE                             │
│     └── 判断下一步:                                                         │
│         ├── 有下一个 Stage: 发送 PipelineBuildStageEvent                    │
│         └── 无下一个 Stage: 发送 PipelineBuildFinishEvent                   │
└─────────────────────────────────────────────────────────────────────────────┘
      │
      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 13. 流水线完成处理                                                           │
│     BuildEndControl.handle(PipelineBuildFinishEvent)                         │
│     源码: biz-engine/.../control/BuildEndControl.kt                         │
│                                                                              │
│     处理流程:                                                                │
│     ├── 更新构建状态: T_PIPELINE_BUILD_HISTORY                              │
│     ├── 释放资源: 构建机、互斥锁等                                          │
│     └── 触发后续处理:                                                       │
│         ├── Notify: 发送通知（邮件/企微/RTX）                               │
│         ├── Metrics: 更新统计数据                                           │
│         ├── Artifactory: 制品归档                                           │
│         └── WebSocket: 实时推送状态更新                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

#### 2.2.5 执行流程总览图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        流水线执行全流程总览                                   │
└─────────────────────────────────────────────────────────────────────────────┘

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
    │
    ▼
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ BuildStart      │────▶│ StageControl    │────▶│ Container       │
│ Control         │     │ (责任链)        │     │ Control         │
│ (启动控制)      │     │                 │     │ (责任链)        │
└─────────────────┘     └─────────────────┘     └────────┬────────┘
                                                         │
                              ┌───────────────────────────┤
                              │                           │
                              ▼                           ▼
                   ┌─────────────────┐         ┌─────────────────┐
                   │ Dispatch        │         │ TaskControl     │
                   │ (构建机分配)    │         │ (引擎侧Task)    │
                   └────────┬────────┘         └─────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │ Agent (Go)      │
                   │ (领取任务)      │
                   └────────┬────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │ Worker (Kotlin) │
                   │ (执行Task)      │
                   └────────┬────────┘
                            │
                            │ completeTask API
                            ▼
                   ┌─────────────────┐
                   │ PipelineBuild   │
                   │ TaskService     │
                   │ (状态回传)      │
                   └────────┬────────┘
                            │
                            │ PipelineBuildContainerEvent
                            ▼
                   ┌─────────────────┐
                   │ 状态逐层回传    │
                   │ Task→Job→Stage  │
                   │ →Pipeline       │
                   └────────┬────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │ BuildEndControl │
                   │ (完成处理)      │
                   └────────┬────────┘
                            │
        ┌───────────────────┼───────────────────┐
        │                   │                   │
        ▼                   ▼                   ▼
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Notify    │     │   Metrics   │     │  WebSocket  │
│   (通知)    │     │   (统计)    │     │  (推送)     │
└─────────────┘     └─────────────┘     └─────────────┘
```

### 2.3 Job 执行机制详解（核心）

#### 2.3.1 Job 中的系统 Task

**重要概念**：启动构建机本身就是一个 Task！每个 `vmBuild` 类型的 Job 会自动生成系统 Task：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    T_PIPELINE_BUILD_TASK 表数据示例                          │
├─────────────────────────────────────────────────────────────────────────────┤
│ TASK_ID      │ TASK_NAME          │ TASK_ATOM                │ TASK_SEQ    │
├──────────────┼────────────────────┼──────────────────────────┼─────────────┤
│ startVM-1    │ Prepare_Job#1      │ dispatchVMStartupTaskAtom│ 1    ← 系统 │
│ e-xxx-1      │ Bash               │ (空)                     │ 2    ← 用户 │
│ e-xxx-2      │ 代码拉取           │ (空)                     │ 3    ← 用户 │
│ e-xxx-3      │ 编译构建           │ (空)                     │ 4    ← 用户 │
│ end-1000     │ Wait_Finish_Job#1  │ (空)                     │ 1000 ← 系统 │
│ stopVM-1001  │ Clean_Job#1        │ dispatchVMShutdownTaskAtom│ 1001 ← 系统 │
└─────────────────────────────────────────────────────────────────────────────┘

说明：
• startVM-{containerId}：启动构建机 Task（TASK_SEQ=1），调用 Dispatch 拉起构建机
• 用户插件：TASK_SEQ 从 2 开始，TASK_ATOM 为空（由 Worker 执行）
• end-1000：等待所有用户插件完成的占位 Task
• stopVM-{containerId+1000}：关闭构建机 Task，释放资源
```

#### 2.3.2 构建机启动与 Task 执行的协作流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        构建机启动流程（核心）                                 │
└─────────────────────────────────────────────────────────────────────────────┘

Process 引擎                    Dispatch 调度                    Agent/Worker
    │                              │                                │
    │ 1. 执行 startVM Task         │                                │
    │    (dispatchVMStartupTaskAtom)                                │
    │─────────────────────────────▶│                                │
    │                              │                                │
    │   (引擎不阻塞，继续处理      │ 2. 分配构建资源                 │
    │    其他事件)                 │   (创建容器/分配构建机)         │
    │                              │───────────────────────────────▶│
    │                              │                                │
    │                              │                                │ 3. Agent 启动 Worker
    │                              │                                │
    │ 4. Worker 调用 jobStarted API│                                │
    │◀────────────────────────────────────────────────────────────────│
    │   (BuildJobResource.jobStarted)                               │
    │                              │                                │
    │ 5. 更新 startVM Task 状态    │                                │
    │    为 SUCCEED                │                                │
    │                              │                                │
    │ 6. 发送 PipelineBuildContainerEvent                           │
    │    (通知引擎继续执行)        │                                │
    │                              │                                │
    │ 7. 返回 BuildVariables       │                                │
    │────────────────────────────────────────────────────────────────▶│
    │                              │                                │
    │                              │                                │ 8. Worker 开始执行插件
    ▼                              ▼                                ▼

关键点：
• Worker 采用 Pull 模式（主动报到），而非服务端 Push
• 构建机就绪后，Worker 调用 jobStarted API 通知服务端
• 服务端通过 PipelineBuildContainerEvent 事件驱动后续 Task
```

#### 2.3.3 普通 Task 执行流程（Worker 侧）

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Worker 任务循环                                 │
└─────────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────────┐
│                         Runner.loopPickup() 主循环                           │
│                                                                             │
│   loop@ while (true) {                                                      │
│       │                                                                     │
│       │ 1. 领取任务                                                         │
│       ├──▶ val buildTask = EngineService.claimTask()                        │
│       │         │                                                           │
│       │         │ HTTP API: GET /api/build/worker/claim                     │
│       │         ▼                                                           │
│       │    ┌─────────────────────────────────────────────────────────────┐  │
│       │    │ 服务端 EngineVMBuildService.buildClaimTask()                │  │
│       │    │   • 查询 QUEUE_CACHE/RUNNING 状态的 Task                    │  │
│       │    │   • 返回第一个待执行的 Task                                 │  │
│       │    │   • 无任务时返回 WAIT 状态                                  │  │
│       │    └─────────────────────────────────────────────────────────────┘  │
│       │                                                                     │
│       │ 2. 根据状态处理                                                     │
│       ├──▶ when (buildTask.status) {                                        │
│       │        BuildTaskStatus.DO -> {                                      │
│       │            │                                                        │
│       │            │ 3. 创建执行器                                          │
│       │            ├──▶ val task = TaskFactory.create(buildTask.type)       │
│       │            │         │                                              │
│       │            │         ├── linuxScript → LinuxScriptTask              │
│       │            │         ├── windowsScript → WindowsScriptTask          │
│       │            │         ├── marketBuild → MarketAtomTask               │
│       │            │         └── marketBuildLess → MarketAtomTask           │
│       │            │                                                        │
│       │            │ 4. 执行任务                                            │
│       │            ├──▶ val taskDaemon = TaskDaemon(task, buildTask, ...)   │
│       │            ├──▶ taskDaemon.runWithTimeout()                         │
│       │            │         │                                              │
│       │            │         └── task.run(buildTask, buildVariables, ws)    │
│       │            │                                                        │
│       │            │ 5. 上报结果                                            │
│       │            └──▶ EngineService.completeTask(buildTaskRst)            │
│       │                      │                                              │
│       │                      │ HTTP API: POST /api/build/worker/complete    │
│       │                      ▼                                              │
│       │                 ┌─────────────────────────────────────────────────┐ │
│       │                 │ 服务端处理:                                     │ │
│       │                 │   • 保存构建变量                                │ │
│       │                 │   • 更新 Task 状态                              │ │
│       │                 │   • 发送 PipelineBuildContainerEvent            │ │
│       │                 │   • 引擎调度下一个 Task                         │ │
│       │                 └─────────────────────────────────────────────────┘ │
│       │        }                                                            │
│       │                                                                     │
│       │        BuildTaskStatus.WAIT -> {                                    │
│       │            Thread.sleep(sleepMills)  // 暂无任务，等待              │
│       │        }                                                            │
│       │                                                                     │
│       │        BuildTaskStatus.END -> break@loop  // Job 结束               │
│       │    }                                                                │
│   }                                                                         │
└─────────────────────────────────────────────────────────────────────────────┘
```

#### 2.3.4 Task 状态流转

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           Task 状态流转图                                    │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────┐
                    │   QUEUE     │ ← 初始状态（引擎创建）
                    │   排队中    │
                    └──────┬──────┘
                           │ 引擎调度到该 Task
                           ▼
                    ┌─────────────┐
                    │ QUEUE_CACHE │ ← 待领取（Worker 可见）
                    │  待执行     │
                    └──────┬──────┘
                           │ Worker claimTask 领取
                           ▼
                    ┌─────────────┐
                    │   RUNNING   │ ← 执行中
                    │   执行中    │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │   SUCCEED   │ │   FAILED    │ │  CANCELED   │
    │   成功      │ │   失败      │ │   取消      │
    └─────────────┘ └─────────────┘ └─────────────┘

特殊状态：
• RETRY：失败重试中
• PAUSE：人工暂停
• SKIP：跳过执行
```

#### 2.3.5 服务端与 Worker 的通信协议

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Worker ↔ Process 通信接口                             │
└─────────────────────────────────────────────────────────────────────────────┘

┌──────────────────┬────────────────────────────────────────────────────────┐
│ 接口             │ 说明                                                   │
├──────────────────┼────────────────────────────────────────────────────────┤
│ jobStarted       │ Worker 启动后调用，通知服务端构建机已就绪              │
│                  │ → 返回 BuildVariables（构建变量、上下文等）            │
├──────────────────┼────────────────────────────────────────────────────────┤
│ claimTask        │ 领取待执行的 Task                                      │
│                  │ → 返回 BuildTask（任务详情）或 WAIT/END 状态           │
├──────────────────┼────────────────────────────────────────────────────────┤
│ completeTask     │ 上报 Task 执行结果                                     │
│                  │ → 触发服务端调度下一个 Task                            │
├──────────────────┼────────────────────────────────────────────────────────┤
│ heartbeat        │ 心跳上报，保持连接活跃                                 │
│                  │ → 服务端检测构建机存活状态                             │
├──────────────────┼────────────────────────────────────────────────────────┤
│ jobEnd           │ Job 执行完毕，Worker 退出前调用                        │
│                  │ → 触发 stopVM Task 释放资源                            │
└──────────────────┴────────────────────────────────────────────────────────┘

API 路径：/api/build/worker/{action}
实现类：BuildJobResource / BuildJobResourceImpl
服务类：EngineVMBuildService
```

### 2.4 流水线数据流图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              数据流向                                        │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────────────────────┐
                    │           Model (JSON)              │
                    │  流水线编排模型                      │
                    │  Pipeline → Stage → Job → Task      │
                    └─────────────────┬───────────────────┘
                                      │
                    ┌─────────────────▼───────────────────┐
                    │         T_PIPELINE_RESOURCE         │
                    │  存储编排模型                        │
                    └─────────────────┬───────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          │                           │                           │
          ▼                           ▼                           ▼
┌─────────────────┐       ┌─────────────────┐       ┌─────────────────┐
│ T_PIPELINE_     │       │ T_PIPELINE_     │       │ T_PIPELINE_     │
│ BUILD_HISTORY   │       │ BUILD_STAGE     │       │ BUILD_CONTAINER │
│ 构建历史        │       │ 阶段状态        │       │ Job 状态        │
└────────┬────────┘       └────────┬────────┘       └────────┬────────┘
         │                         │                         │
         │                         │                         │
         │                         ▼                         │
         │                ┌─────────────────┐                │
         │                │ T_PIPELINE_     │                │
         │                │ BUILD_TASK      │◀───────────────┘
         │                │ 任务状态        │
         │                └────────┬────────┘
         │                         │
         │    ┌────────────────────┼────────────────────┐
         │    │                    │                    │
         ▼    ▼                    ▼                    ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ T_PIPELINE_     │    │ T_PIPELINE_     │    │ T_REPORT        │
│ BUILD_VAR       │    │ BUILD_DETAIL    │    │ 构建报告        │
│ 构建变量        │    │ 构建详情        │    │ (Artifactory)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## 三、模块间调用关系

### 3.1 服务依赖图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                            服务依赖关系                                      │
└─────────────────────────────────────────────────────────────────────────────┘

                              ┌──────────┐
                              │ Project  │ ◀──── 所有服务的基础依赖
                              │ 项目管理 │
                              └────┬─────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         │                         │                         │
         ▼                         ▼                         ▼
    ┌──────────┐             ┌──────────┐             ┌──────────┐
    │   Auth   │             │ Process  │             │  Store   │
    │ 权限认证 │◀────────────│ 流水线   │────────────▶│ 研发商店 │
    └────┬─────┘             └────┬─────┘             └────┬─────┘
         │                        │                        │
         │           ┌────────────┼────────────┐           │
         │           │            │            │           │
         │           ▼            ▼            ▼           │
         │     ┌──────────┐ ┌──────────┐ ┌──────────┐     │
         │     │Repository│ │ Dispatch │ │  Ticket  │     │
         │     │ 代码库   │ │ 构建调度 │ │ 凭证管理 │     │
         │     └────┬─────┘ └────┬─────┘ └────┬─────┘     │
         │          │            │            │           │
         │          │            ▼            │           │
         │          │     ┌──────────┐        │           │
         │          │     │Environment│       │           │
         │          │     │ 构建机   │        │           │
         │          │     └────┬─────┘        │           │
         │          │          │              │           │
         │          │          ▼              │           │
         │          │   ┌────────────┐        │           │
         │          │   │Agent/Worker│        │           │
         │          │   │ 构建执行   │        │           │
         │          │   └─────┬──────┘        │           │
         │          │         │               │           │
         │          ▼         ▼               ▼           │
         │     ┌──────────────────────────────────┐       │
         │     │         支撑服务                  │       │
         │     │  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐│       │
         │     │  │ Log │ │Artif│ │Notif│ │Quali││       │
         │     │  │日志 │ │制品 │ │通知 │ │红线 │◀───────┘
         │     │  └─────┘ └─────┘ └─────┘ └─────┘│
         │     └──────────────────────────────────┘
         │                    │
         │                    ▼
         │            ┌──────────────┐
         └───────────▶│   Metrics    │
                      │   度量统计   │
                      └──────────────┘
```

### 3.2 典型调用链路

#### 链路1：手动触发构建

```
Frontend (devops-pipeline)
    │
    │ POST /api/process/user/builds/{projectId}/{pipelineId}/start
    ▼
Process (UserBuildResource)
    │
    ├──▶ Auth (ServiceAuthResource)
    │    └── 检查 pipeline_execute 权限
    │
    ├──▶ Project (ServiceProjectResource)
    │    └── 获取项目信息
    │
    ├──▶ Repository (ServiceRepositoryResource)
    │    └── 获取代码库信息
    │
    ├──▶ Ticket (ServiceTicketResource)
    │    └── 获取凭证信息
    │
    ├──▶ Store (ServiceAtomResource)
    │    └── 校验插件版本
    │
    └──▶ RabbitMQ
         └── 发送 PipelineBuildStartEvent
              │
              ▼
         Process (BuildEventDispatcher)
              │
              └──▶ Dispatch (ServiceDispatchResource)
                   └── 分配构建机
                        │
                        ▼
                   Environment (ServiceNodeResource)
                        └── 查询可用节点
                             │
                             ▼
                        Agent (Ask API)
                             └── 获取构建任务
                                  │
                                  ▼
                             Worker (Runner)
                                  │
                                  ├──▶ Store: 下载插件
                                  ├──▶ Ticket: 获取凭证
                                  ├──▶ Repository: 拉取代码
                                  ├──▶ Artifactory: 上传制品
                                  └──▶ Log: 上报日志
```

#### 链路2：代码提交触发

```
Git Server (GitLab/GitHub/工蜂)
    │
    │ Webhook POST /api/repository/webhooks/{projectId}
    ▼
Repository (WebhookResource)
    │
    ├──▶ 解析 Webhook 事件
    │    └── Push/MR/Tag 等
    │
    └──▶ Process (ServiceBuildResource)
         └── 触发流水线构建
              │
              └── (后续流程同链路1)
```

#### 链路3：研发商店插件执行

```
Worker (MarketAtomTask)
    │
    ├──▶ Store (ServiceMarketAtomResource)
    │    └── 获取插件信息和下载地址
    │
    ├──▶ Artifactory (BuildArchiveResource)
    │    └── 下载插件安装包
    │
    ├──▶ Ticket (BuildCredentialResource)
    │    └── 获取插件所需凭证
    │
    ├──▶ 执行插件脚本
    │    └── Python/NodeJS/Go/Java
    │
    └──▶ Log (BuildLogResource)
         └── 上报执行日志
```

## 四、核心数据模型关系

### 4.1 流水线模型 (Model) 结构

```
Model (流水线模型)
│
├── name: String                    # 流水线名称
├── desc: String                    # 描述
├── stages: List<Stage>             # 阶段列表
│   │
│   └── Stage (阶段)
│       ├── id: String              # 阶段ID (s-xxx)
│       ├── name: String            # 阶段名称
│       ├── checkIn: StagePauseCheck # 准入检查
│       ├── checkOut: StagePauseCheck # 准出检查
│       └── containers: List<Container> # Job 列表
│           │
│           └── Container (Job)
│               ├── id: String      # Job ID (c-xxx)
│               ├── name: String    # Job 名称
│               ├── @type: String   # 类型标识
│               │   ├── vmBuild     # 需要构建机
│               │   ├── normal      # 无编译环境
│               │   └── trigger     # 触发器
│               ├── dispatchType: DispatchType # 调度类型
│               ├── mutexGroup: String # 互斥组
│               └── elements: List<Element> # 任务列表
│                   │
│                   └── Element (Task)
│                       ├── id: String      # 任务ID (t-xxx)
│                       ├── name: String    # 任务名称
│                       ├── @type: String   # 插件类型
│                       │   ├── marketBuild # 研发商店插件
│                       │   ├── linuxScript # Shell 脚本
│                       │   ├── windowsScript # Bat 脚本
│                       │   ├── manualReviewUserTask # 人工审核
│                       │   └── ...
│                       └── data: Map       # 插件参数
│
├── triggers: List<Trigger>         # 触发器配置
├── params: List<BuildFormProperty> # 启动参数
└── setting: PipelineSetting        # 流水线设置
```

### 4.2 数据库表关联

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           核心表关联关系                                     │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌─────────────────────┐
                    │     T_PROJECT       │
                    │  (devops_ci_project)│
                    └──────────┬──────────┘
                               │ PROJECT_ID
         ┌─────────────────────┼─────────────────────┐
         │                     │                     │
         ▼                     ▼                     ▼
┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐
│ T_PIPELINE_INFO │   │  T_REPOSITORY   │   │  T_AUTH_RESOURCE│
│   流水线信息    │   │    代码库       │   │    权限资源     │
└────────┬────────┘   └─────────────────┘   └─────────────────┘
         │ PIPELINE_ID
         │
    ┌────┴────┬─────────────────┬─────────────────┐
    │         │                 │                 │
    ▼         ▼                 ▼                 ▼
┌────────┐ ┌────────────┐ ┌────────────┐ ┌────────────────┐
│RESOURCE│ │  SETTING   │ │  SUMMARY   │ │ BUILD_HISTORY  │
│编排模型│ │  配置      │ │  摘要      │ │   构建历史     │
└────────┘ └────────────┘ └────────────┘ └───────┬────────┘
                                                 │ BUILD_ID
                                                 │
         ┌───────────────────────────────────────┼───────────────┐
         │                   │                   │               │
         ▼                   ▼                   ▼               ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────┐ ┌─────────────┐
│  BUILD_STAGE    │ │ BUILD_CONTAINER │ │ BUILD_TASK  │ │  BUILD_VAR  │
│   阶段状态      │ │   Job 状态      │ │  任务状态   │ │  构建变量   │
└─────────────────┘ └─────────────────┘ └─────────────┘ └─────────────┘
```

## 五、事件驱动机制

### 5.1 核心事件流

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           事件驱动流程                                       │
└─────────────────────────────────────────────────────────────────────────────┘

构建启动
    │
    ▼
PipelineBuildStartEvent ──────────────────────────────────────────────────────┐
    │                                                                          │
    ▼                                                                          │
┌───────────────────┐                                                          │
│ StageControl      │                                                          │
│ 处理 Stage 事件   │                                                          │
└─────────┬─────────┘                                                          │
          │                                                                    │
          ▼                                                                    │
PipelineBuildContainerEvent ──────────────────────────────────────────────────┤
          │                                                                    │
          ▼                                                                    │
┌───────────────────┐                                                          │
│ ContainerControl  │                                                          │
│ 处理 Job 事件     │                                                          │
└─────────┬─────────┘                                                          │
          │                                                                    │
          ├──▶ 需要构建机: 发送 PipelineAgentStartupEvent                      │
          │         │                                                          │
          │         ▼                                                          │
          │    Dispatch 分配构建机                                             │
          │         │                                                          │
          │         ▼                                                          │
          │    Agent 执行 → Worker 执行任务                                    │
          │         │                                                          │
          │         ▼                                                          │
          │    PipelineBuildTaskFinishEvent ◀──────────────────────────────────┤
          │         │                                                          │
          │         ▼                                                          │
          │    ┌───────────────────┐                                           │
          │    │ TaskControl       │                                           │
          │    │ 处理任务完成事件 │                                           │
          │    └─────────┬─────────┘                                           │
          │              │                                                     │
          │              ▼                                                     │
          │    所有任务完成? ──▶ PipelineBuildContainerEvent (完成)            │
          │                              │                                     │
          │                              ▼                                     │
          │                    所有 Job 完成?                                  │
          │                              │                                     │
          │                              ▼                                     │
          │                    PipelineBuildStageEvent (完成)                  │
          │                              │                                     │
          │                              ▼                                     │
          │                    ┌───────────────────┐                           │
          │                    │ StageControl      │                           │
          │                    │ 检查下一个 Stage  │                           │
          │                    └─────────┬─────────┘                           │
          │                              │                                     │
          │                              ├──▶ 有下一个 Stage: 循环处理         │
          │                              │                                     │
          │                              └──▶ 无: PipelineBuildFinishEvent     │
          │                                              │                     │
          │                                              ▼                     │
          │                                   ┌───────────────────┐            │
          │                                   │ 构建完成处理      │            │
          │                                   │ • 更新状态        │            │
          │                                   │ • 发送通知        │            │
          │                                   │ • 更新统计        │            │
          │                                   └───────────────────┘            │
          │                                                                    │
          └──▶ 无编译环境: 直接执行任务 ───────────────────────────────────────┘
```

### 5.2 事件类型汇总

| 事件类型 | 触发时机 | 处理器 |
|----------|----------|--------|
| `PipelineBuildStartEvent` | 构建启动 | StageControl |
| `PipelineBuildStageEvent` | Stage 状态变更 | StageControl |
| `PipelineBuildContainerEvent` | Job 状态变更 | ContainerControl |
| `PipelineBuildTaskFinishEvent` | 任务完成 | TaskControl |
| `PipelineBuildFinishEvent` | 构建完成 | BuildFinishListener |
| `PipelineBuildCancelEvent` | 构建取消 | CancelControl |
| `PipelineAgentStartupEvent` | 构建机启动 | DispatchListener |
| `PipelineAgentShutdownEvent` | 构建机释放 | DispatchListener |

### 5.3 事件驱动的非阻塞机制

**核心设计**：引擎发出事件后不阻塞等待，继续处理其他事件。

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        非阻塞事件处理机制                                    │
└─────────────────────────────────────────────────────────────────────────────┘

场景：Job 需要等待构建机就绪（可能需要几秒到几分钟）

传统阻塞方式（❌ 不采用）：
┌──────────────────────────────────────────────────────────────────────────┐
│  Process 引擎线程                                                         │
│      │                                                                    │
│      ├── 发送调度请求                                                     │
│      ├── 阻塞等待构建机就绪...  ← 线程被占用，无法处理其他任务            │
│      ├── 构建机就绪                                                       │
│      └── 继续执行                                                         │
└──────────────────────────────────────────────────────────────────────────┘

BK-CI 事件驱动方式（✅ 采用）：
┌──────────────────────────────────────────────────────────────────────────┐
│  Process 引擎                                                             │
│      │                                                                    │
│      ├── 发送调度请求（PipelineAgentStartupEvent）                        │
│      ├── 立即返回，处理其他事件（其他流水线、其他 Job）                   │
│      │                                                                    │
│  ... 时间流逝，构建机启动中 ...                                           │
│      │                                                                    │
│  Worker 启动后                                                            │
│      ├── 调用 jobStarted API                                              │
│      ├── 服务端发送 PipelineBuildContainerEvent                           │
│      │                                                                    │
│  引擎收到事件                                                             │
│      └── 继续执行该 Job 的后续 Task                                       │
└──────────────────────────────────────────────────────────────────────────┘

优势：
• 单个引擎实例可并发处理大量构建
• 不会因为某个构建机慢而阻塞其他构建
• 资源利用率高
```

### 5.4 关键事件触发点源码位置

| 事件 | 触发位置 | 说明 |
|------|----------|------|
| `PipelineBuildStartEvent` | `PipelineRuntimeService.sendBuildStartEvent()` | 构建启动时发送 |
| `PipelineBuildContainerEvent` | `EngineVMBuildService.setStartUpVMStatus()` | 构建机就绪后发送 |
| `PipelineBuildContainerEvent` | `PipelineBuildTaskService.finishTask()` | Task 完成后发送 |
| `PipelineBuildStageEvent` | `ContainerControl` | 所有 Job 完成后发送 |
| `PipelineBuildFinishEvent` | `StageControl` | 所有 Stage 完成后发送 |

## 六、跨模块协作场景

### 6.1 场景：创建并执行一条完整流水线

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 步骤 1: 创建项目                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Project + Auth                                                         │
│ 操作:                                                                        │
│   • Project: 创建项目记录 (T_PROJECT)                                        │
│   • Auth: 创建项目权限组，授予创建者管理员权限                               │
│   • Project: 初始化项目配额、路由规则                                        │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 步骤 2: 关联代码库                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Repository + Ticket                                                    │
│ 操作:                                                                        │
│   • Ticket: 创建 Git 凭证 (用户名密码/SSH/Token)                             │
│   • Repository: 关联代码库，配置 Webhook                                     │
│   • Repository: 验证代码库连通性                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 步骤 3: 安装构建机                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Environment                                                            │
│ 操作:                                                                        │
│   • 创建节点记录 (T_NODE)                                                    │
│   • 生成 Agent 安装脚本                                                      │
│   • Agent 安装并上报状态 (T_ENVIRONMENT_THIRDPARTY_AGENT)                    │
│   • 配置节点环境变量、并行任务数                                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 步骤 4: 安装插件                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Store                                                                  │
│ 操作:                                                                        │
│   • 浏览研发商店，选择插件                                                   │
│   • 安装插件到项目 (T_STORE_PROJECT_REL)                                     │
│   • 插件下载量统计更新                                                       │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 步骤 5: 创建流水线                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Process + Store + Repository + Auth                                    │
│ 操作:                                                                        │
│   • 前端编排流水线 Model                                                     │
│   • Process: 校验插件 (调用 Store)                                           │
│   • Process: 校验代码库 (调用 Repository)                                    │
│   • Process: 保存流水线 (T_PIPELINE_INFO, T_PIPELINE_RESOURCE)               │
│   • Auth: 创建流水线权限资源                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 步骤 6: 执行流水线                                                           │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: 全部模块协作                                                           │
│ 操作:                                                                        │
│   • Process: 创建构建记录，发送启动事件                                      │
│   • Quality: 检查准入红线                                                    │
│   • Dispatch: 分配构建机                                                     │
│   • Environment: 查询可用节点                                                │
│   • Agent: 接收任务，启动 Worker                                             │
│   • Worker: 执行任务                                                         │
│     - Store: 下载插件                                                        │
│     - Ticket: 获取凭证                                                       │
│     - Repository: 拉取代码                                                   │
│     - Artifactory: 上传制品                                                  │
│     - Log: 上报日志                                                          │
│   • Quality: 检查准出红线                                                    │
│   • Notify: 发送构建通知                                                     │
│   • Metrics: 更新统计数据                                                    │
│   • WebSocket: 实时推送状态                                                  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.2 场景：插件开发与发布

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段 1: 插件开发                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ 开发者本地:                                                                  │
│   • 使用 SDK 开发插件 (Python/NodeJS/Go/Java)                                │
│   • 编写 task.json 定义输入输出                                              │
│   • 本地测试                                                                 │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段 2: 插件上架                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Store + Artifactory                                                    │
│ 操作:                                                                        │
│   • Store: 创建插件记录 (T_ATOM)                                             │
│   • Artifactory: 上传插件包                                                  │
│   • Store: 提交审核                                                          │
│   • Store: 审核通过，发布到商店                                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 阶段 3: 插件使用                                                             │
├─────────────────────────────────────────────────────────────────────────────┤
│ 模块: Store + Process + Worker                                               │
│ 操作:                                                                        │
│   • Store: 用户安装插件到项目                                                │
│   • Process: 流水线中使用插件                                                │
│   • Worker (MarketAtomTask):                                                 │
│     - 从 Store 获取插件信息                                                  │
│     - 从 Artifactory 下载插件包                                              │
│     - 准备输入参数                                                           │
│     - 执行插件脚本                                                           │
│     - 解析输出结果                                                           │
│   • Store: 更新使用统计                                                      │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 6.3 场景：权限控制流程

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 权限检查链路                                                                 │
└─────────────────────────────────────────────────────────────────────────────┘

用户请求
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ Gateway (OpenResty)                                                          │
│   • 验证 Token/Session                                                       │
│   • 获取用户信息                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 业务服务 (如 Process)                                                        │
│   • 调用 Auth 服务检查权限                                                   │
│   │                                                                          │
│   └──▶ Auth (RbacPermissionService)                                         │
│        │                                                                     │
│        ├── 1. 检查用户是否为项目成员                                         │
│        │   └── T_AUTH_RESOURCE_GROUP_MEMBER                                  │
│        │                                                                     │
│        ├── 2. 获取用户所属用户组                                             │
│        │   └── T_AUTH_RESOURCE_GROUP                                         │
│        │                                                                     │
│        ├── 3. 获取用户组权限                                                 │
│        │   └── T_AUTH_ACTION                                                 │
│        │                                                                     │
│        └── 4. 判断是否有目标操作权限                                         │
│            └── 返回 true/false                                               │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 权限不足时的处理                                                             │
│   • 返回 403 错误                                                            │
│   • 前端展示权限申请入口                                                     │
│   • 用户可申请加入用户组                                                     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 七、技术栈与工具链

### 7.1 后端技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **语言** | Kotlin/Java | 主要业务代码 |
| **框架** | Spring Boot 3 | 微服务基础框架 |
| **构建** | Gradle Kotlin DSL | 依赖管理和构建 |
| **数据库** | MySQL/MariaDB + JOOQ | 关系型数据存储 |
| **缓存** | Redis | 缓存和分布式锁 |
| **消息** | RabbitMQ | 事件驱动消息队列 |
| **搜索** | ElasticSearch | 日志存储和检索 |
| **服务发现** | Consul | 微服务注册发现 |
| **静态分析** | Detekt | Kotlin 代码检查 |

### 7.2 前端技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **框架** | Vue 2.7 | 前端框架 |
| **状态管理** | Vuex 3.6 | 全局状态 |
| **路由** | Vue Router 3.6 | 前端路由 |
| **UI 组件** | bk-magic-vue | 蓝鲸 MagicBox |
| **样式** | Sass/SCSS | CSS 预处理 |
| **打包** | Webpack 5 | 模块打包 |
| **代码检查** | ESLint | 代码规范检查 |

### 7.3 Agent 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **语言** | Go 1.19+ | Agent 主程序 |
| **进程管理** | Daemon + Agent | 双进程架构 |
| **通信** | HTTP/HTTPS | 与后端服务通信 |
| **数据采集** | Telegraf | 构建机指标采集 |

### 7.4 Worker 技术栈

| 层次 | 技术 | 说明 |
|------|------|------|
| **语言** | Kotlin | Worker 主程序 |
| **运行时** | JVM | Java 虚拟机 |
| **插件执行** | 多语言 | Python/NodeJS/Go/Java |

## 八、开发指南索引

### 8.1 按模块查找 Skill

| 模块 | Skill 编号 | 描述 |
|------|------------|------|
| 全局架构 | 00 | 本文档 |
| 后端开发 | 01 | 后端微服务开发规范 |
| API 设计 | 02 | API 接口设计规范 |
| 单元测试 | 03 | 单元测试编写指南 |
| 前端开发 | 04 | Vue 前端开发规范 |
| Agent 开发 | 05 | Go Agent 开发指南 |
| 数据库 | 06, 44 | 数据库脚本管理、表设计规范 |
| Git 提交 | 07 | 提交规范 |
| 事件驱动 | 08 | 事件驱动架构 |
| 设计模式 | 09, 27 | 责任链模式、设计模式实践 |
| 分布式锁 | 10 | 分布式锁使用 |
| 服务调用 | 11 | 服务间调用规范 |
| 监控 | 12 | 性能监控 |
| 重试 | 13 | 重试机制 |
| 国际化 | 14 | i18n 和日志 |
| 参数校验 | 15 | 参数验证 |
| 定时任务 | 16 | 定时任务开发 |
| AOP | 17 | 切面编程 |
| 条件配置 | 18 | 条件配置 |
| JWT | 19 | JWT 安全认证 |
| 审计日志 | 20 | 审计日志 |
| 表达式 | 21 | 表达式解析器 |
| 分片 | 23 | 数据库分片 |
| OAuth2 | 24 | OAuth2 认证 |
| 变量扩展 | 26 | 流水线变量扩展 |
| 流水线模型 | 28 | Pipeline Model 架构 |
| Process | 29 系列 | 流水线核心模块 |
| Auth | 30 | 权限认证模块 |
| Project | 31 | 项目管理模块 |
| Repository | 32 | 代码库模块 |
| Store | 33 | 研发商店模块 |
| Artifactory | 34 | 制品库模块 |
| Dispatch | 35 | 构建调度模块 |
| Environment | 36 | 构建机环境模块 |
| Ticket | 37 | 凭证管理模块 |
| Notify | 39 | 通知服务模块 |
| OpenAPI | 41 | 开放接口模块 |
| Worker | 42 | 构建执行器模块 |
| Agent | 43 | 构建机代理模块 |

### 8.2 按场景查找

| 场景 | 涉及 Skill |
|------|------------|
| 新增流水线功能 | 28, 29 系列, 08 |
| 开发新插件 | 33, 42 |
| 修改权限逻辑 | 30, 24 |
| 优化构建调度 | 35, 36, 43 |
| 添加新通知渠道 | 39 |
| 扩展 API 接口 | 02, 41 |
| 数据库表变更 | 06, 44 |
| 前端页面开发 | 04 |
| Agent 功能扩展 | 05, 43 |

## 九、总结

BK-CI 是一个以**流水线**为核心的 CI/CD 平台，通过 16 个后端微服务、14 个前端应用和 Go 构建代理的协作，实现了从代码提交到制品交付的完整自动化流程。

### 核心理解要点

1. **Process 是核心** - 流水线编排、构建调度、状态管理都在这里
2. **事件驱动** - 通过 RabbitMQ 实现模块间解耦和异步处理
3. **分层架构** - 每个服务都有 api/biz/model/boot 四层结构
4. **双层构建** - Agent(Go) 负责进程管理，Worker(Kotlin) 负责任务执行
5. **权限统一** - Auth 模块基于 RBAC 统一管理所有资源权限

### Job/Task 执行机制要点

6. **启动构建机是 Task** - 每个 vmBuild 类型的 Job 会自动生成 `startVM`、`end`、`stopVM` 系统 Task
7. **Worker 采用 Pull 模式** - Worker 主动调用 `claimTask` 领取任务，而非服务端推送
8. **非阻塞等待** - 引擎发出调度请求后不阻塞，通过事件回调继续执行
9. **Task 完成触发事件** - Worker 调用 `completeTask` 后，服务端发送 `PipelineBuildContainerEvent` 调度下一个 Task

### 关键代码入口

| 功能 | 代码位置 |
|------|----------|
| Worker 主循环 | `Runner.loopPickup()` |
| 领取任务 | `EngineVMBuildService.buildClaimTask()` |
| 上报结果 | `EngineVMBuildService.buildCompleteTask()` |
| 构建机就绪通知 | `EngineVMBuildService.setStartUpVMStatus()` |
| Task 完成处理 | `PipelineBuildTaskService.finishTask()` |
| 容器事件处理 | `ContainerControl.handle()` |

掌握了这个全局视图，就能理解 BK-CI 任何功能的实现位置和模块协作方式。

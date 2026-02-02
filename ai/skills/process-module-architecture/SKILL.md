---
name: process-module-architecture
description: Process 流水线核心模块架构总览，涵盖流水线 CRUD、构建执行引擎、事件驱动机制、分层架构设计。当用户开发流水线核心功能、理解 Process 模块、修改构建逻辑或进行流水线相关开发时使用。
---

# Process 流水线核心模块架构指南

> **模块定位**: Process 是 BK-CI 的核心模块，负责流水线的编排、调度与执行。

## 详细文档导航

本 Skill 提供 Process 模块的架构总览。如需深入了解各层实现细节，请查阅以下参考文档：

| 文档 | 内容 |
|------|------|
| [1-api-layer.md](reference/1-api-layer.md) | API 接口层详细分析（708 行） |
| [2-service-layer.md](reference/2-service-layer.md) | Service 业务层详细分析（872 行） |
| [3-engine-control.md](reference/3-engine-control.md) | 构建引擎 Control 层分析（790 行） |
| [4-dao-database.md](reference/4-dao-database.md) | DAO 层与数据库表结构（901 行） |
| [5-event-driven.md](reference/5-event-driven.md) | 事件驱动机制详解（759 行） |

> **使用建议**: 先阅读本文档了解整体架构，再根据具体开发需求深入对应的参考文档。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/process/
├── api-process/          # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/process/api/
│       ├── user/         # 用户接口（20+文件）
│       ├── service/      # 服务间调用接口（25+文件）
│       ├── builds/       # 构建机接口（8文件）
│       ├── template/     # 模板接口（6文件）
│       └── op/           # 运维接口（12文件）
│
├── biz-base/             # 基础业务逻辑层（核心）
│   └── src/main/kotlin/com/tencent/devops/process/
│       ├── engine/
│       │   ├── dao/      # 数据访问层（20+文件）
│       │   ├── service/  # 核心服务（30+文件）
│       │   ├── pojo/     # 数据对象
│       │   │   └── event/# 事件定义（19文件）
│       │   └── control/  # 控制逻辑
│       └── service/      # 业务服务
│
├── biz-engine/           # 构建引擎层
│   └── src/main/kotlin/com/tencent/devops/process/engine/
│       ├── control/      # 调度控制器（9文件）
│       ├── atom/         # 插件执行
│       └── listener/     # 事件监听
│
├── biz-process/          # 业务处理层
│   └── src/main/kotlin/com/tencent/devops/process/
│       ├── api/          # API 实现（ResourceImpl）
│       ├── service/      # Facade 服务
│       ├── permission/   # 权限服务
│       └── trigger/      # 触发服务
│
├── boot-engine/          # 引擎启动模块
├── boot-process/         # 服务启动模块
└── model-process/        # 数据模型层（JOOQ 生成）
```

### 1.2 模块职责矩阵

| 模块 | 职责 | 核心类数量 | 依赖关系 |
|------|------|------------|----------|
| **api-process** | REST API 接口定义 | 91 | 被 biz-process 实现 |
| **biz-process** | 业务逻辑、API 实现 | 100+ | 依赖 biz-base |
| **biz-base** | 引擎核心服务、DAO | 185+ | 依赖 model-process |
| **biz-engine** | 构建调度引擎 | 25 | 依赖 biz-base |
| **model-process** | JOOQ 数据模型 | 自动生成 | 基础层 |

## 二、分层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              请求入口                                    │
│                    HTTP Request / 服务间调用 / MQ 消息                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-process)                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │UserPipeline  │ │ServiceBuild  │ │BuildBuild    │ │UserTemplate  │    │
│  │Resource      │ │Resource      │ │Resource      │ │Resource      │    │
│  │(用户流水线)   │ │(服务间构建)   │ │(构建机调用)   │ │(模板管理)    │    │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-process)                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      ResourceImpl 实现层                          │   │
│  │  UserPipelineResourceImpl | ServiceBuildResourceImpl | ...       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      Facade Service 层                            │   │
│  │  PipelineInfoFacadeService    - 流水线信息管理门面                  │   │
│  │  PipelineListFacadeService    - 流水线列表查询门面                  │   │
│  │  PipelineVersionFacadeService - 版本管理门面                       │   │
│  │  ParamFacadeService           - 参数管理门面                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      基础业务层 (biz-base)                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      Engine Service 层                            │   │
│  │  PipelineRepositoryService  - 流水线存储服务（110KB，核心）         │   │
│  │  PipelineRuntimeService     - 运行时服务（101KB，核心）            │   │
│  │  PipelineContainerService   - 容器管理服务                        │   │
│  │  PipelineStageService       - 阶段管理服务                        │   │
│  │  PipelineTaskService        - 任务管理服务                        │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      DAO 层                                       │   │
│  │  PipelineInfoDao | PipelineBuildDao | PipelineResourceDao        │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      引擎层 (biz-engine)                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      Control 层 (核心调度)                         │   │
│  │  BuildStartControl | StageControl | ContainerControl | TaskControl│   │
│  │  BuildEndControl | BuildCancelControl | MutexControl             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      数据层 (model-process + MySQL)                      │
│  数据库：devops_process（共 92 张表）                                    │
└─────────────────────────────────────────────────────────────────────────┘
```

## 三、核心数据流

### 3.1 流水线创建流程

```
用户请求
    │
    ▼
UserPipelineResource.create()          # API 层
    │
    ▼
UserPipelineResourceImpl.create()      # 实现层
    │
    ▼
PipelineInfoFacadeService.createPipeline()  # Facade 层
    │
    ├─► 权限校验 (PipelinePermissionService)
    ├─► 模型校验 (ModelCheckPlugin)
    │
    ▼
PipelineRepositoryService.deployPipeline()  # Engine Service 层
    │
    ├─► PipelineInfoDao.create()        # 保存流水线信息
    ├─► PipelineResourceDao.create()    # 保存流水线模型
    └─► PipelineSettingDao.create()     # 保存流水线配置
```

### 3.2 构建执行完整流程（对应流程图）

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                        触发层                                                     │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐                             │
│  │ Quartz定时调度   │     │ API接口触发      │     │ Webhook触发     │                             │
│  │ PipelineQuartz  │     │ UserBuild       │     │ PipelineBuild   │                             │
│  │ Service         │     │ Resource        │     │ WebhookService  │                             │
│  └────────┬────────┘     └────────┬────────┘     └────────┬────────┘                             │
│           │                       │                       │                                       │
│           └───────────────────────┼───────────────────────┘                                       │
│                                   ▼                                                               │
│                    ┌──────────────────────────────┐                                               │
│                    │ PipelineTimerService         │  ← 定时触发服务                                │
│                    │ PipelineBuildFacadeService   │  ← 构建门面服务                                │
│                    └──────────────┬───────────────┘                                               │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                     拦截器链层                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                         PipelineInterceptorChain                                             │ │
│  │  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────────┐                  │ │
│  │  │ RunLock         │→ │ Queue           │→ │ TimerTriggerScmChange       │                  │ │
│  │  │ Interceptor     │  │ Interceptor     │  │ Interceptor                 │                  │ │
│  │  │ (运行锁检查)     │  │ (队列/并发控制)  │  │ (定时触发源码变更检查)        │                  │ │
│  │  └─────────────────┘  └─────────────────┘  └─────────────────────────────┘                  │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │ 拦截通过
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   运行时服务层                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  PipelineRuntimeService.startBuild()                                                             │
│    ├─► 生成 buildId                                                                              │
│    ├─► 创建构建记录 (T_PIPELINE_BUILD_HISTORY)                                                   │
│    ├─► 创建 Stage/Container/Task 记录                                                            │
│    └─► 保存构建变量                                                                              │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   事件分发层                                                      │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  PipelineEventDispatcher.dispatch(PipelineBuildStartEvent)  ──────────►  RabbitMQ                │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   监听器层 (Listener)                                             │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                       │
│  │PipelineBuildStart   │  │PipelineStageBuild   │  │PipelineContainerBuild│                      │
│  │Listener             │  │Listener             │  │Listener             │                       │
│  │  └► BuildStartControl│  │  └► StageControl    │  │  └► ContainerControl│                       │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                       │
│  ┌─────────────────────┐  ┌─────────────────────┐  ┌─────────────────────┐                       │
│  │PipelineAtomTaskBuild│  │PipelineBuildFinish  │  │PipelineBuildCancel  │                       │
│  │Listener             │  │Listener             │  │Listener             │                       │
│  │  └► TaskControl     │  │  └► BuildEndControl │  │  └► BuildCancelControl│                      │
│  └─────────────────────┘  └─────────────────────┘  └─────────────────────┘                       │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   引擎控制层 (Control)                                            │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  BuildStartControl ─► StageControl ─► ContainerControl ─► TaskControl ─► BuildEndControl        │
│        │                   │                 │                 │                                 │
│        │                   │                 │                 ▼                                 │
│        │                   │                 │    ┌─────────────────────────┐                    │
│        │                   │                 │    │ 发送 AgentStartupEvent  │                    │
│        │                   │                 │    │ 到 Dispatch 服务        │                    │
│        │                   │                 │    └───────────┬─────────────┘                    │
│        │                   │                 │                │                                  │
│        ▼                   ▼                 ▼                ▼                                  │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │                              命令链 (Command Chain)                                          │ │
│  │  Stage: CheckSkip → CheckInterrupt → CheckPauseReview → StartContainer → UpdateState       │ │
│  │  Container: CheckSkip → CheckDependOn → CheckMutex → CheckDispatch → StartTask → UpdateState│ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   Dispatch 调度服务                                               │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │ DispatcherService (构建机分配服务)                                                           │ │
│  │   ├─► ThirdPartyDispatchService (第三方构建机调度)                                           │ │
│  │   │     ├─► ThirdPartyAgentIDDispatchType (指定Agent)                                       │ │
│  │   │     └─► ThirdPartyAgentEnvDispatchType (环境调度)                                       │ │
│  │   ├─► VMDispatcher (虚拟机调度)                                                             │ │
│  │   └─► DockerDispatcher (Docker调度)                                                         │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
│                                           │                                                      │
│                                           ▼                                                      │
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │ 构建分发 REST API                                                                            │ │
│  │   BuildAgentBuildResource.claimBuildTask()  ← Agent 领取任务                                 │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────┼───────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   构建机 Agent                                                    │
├──────────────────────────────────────────────────────────────────────────────────────────────────┤
│  ┌─────────────────────────────────────────────────────────────────────────────────────────────┐ │
│  │ BuildRunner (容器或虚运行)                    MasterRunner (裸运行)                          │ │
│  │   ├─► 领取任务                                 ├─► 领取任务                                  │ │
│  │   ├─► 执行插件                                 ├─► 执行插件                                  │ │
│  │   ├─► 心跳上报                                 ├─► 心跳上报                                  │ │
│  │   └─► 回调完成                                 └─► 回调完成                                  │ │
│  └─────────────────────────────────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────────────────────────────────┘
```

## 四、核心类速查

### 4.1 触发层

| 类名 | 模块 | 职责 |
|------|------|------|
| `PipelineQuartzService` | biz-process | Quartz 定时调度服务 |
| `PipelineQuartzJob` | biz-process | Quartz Job 实现 |
| `PipelineTimerService` | biz-process | 定时器管理服务 |
| `PipelineTimerBuildListener` | biz-process | 定时触发监听器 |

### 4.2 拦截器层

| 类名 | 模块 | 职责 |
|------|------|------|
| `PipelineInterceptorChain` | biz-base | 拦截器链 |
| `PipelineInterceptor` | biz-base | 拦截器接口 |
| `RunLockInterceptor` | biz-base | 运行锁拦截器 |
| `QueueInterceptor` | biz-base | 队列/并发控制拦截器 |
| `TimerTriggerScmChangeInterceptor` | biz-base | 定时触发源码变更检查 |

### 4.3 Service 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `PipelineRepositoryService` | 110KB | 流水线存储，最核心的服务 |
| `PipelineRuntimeService` | 101KB | 构建运行时管理 |
| `PipelineBuildFacadeService` | 137KB | 构建管理门面 |
| `PipelineInfoFacadeService` | 80KB | 流水线信息管理门面 |

### 4.4 监听器层

| 类名 | 模块 | 消费事件 |
|------|------|----------|
| `PipelineBuildStartListener` | biz-engine | `PipelineBuildStartEvent` |
| `PipelineStageBuildListener` | biz-engine | `PipelineBuildStageEvent` |
| `PipelineContainerBuildListener` | biz-engine | `PipelineBuildContainerEvent` |
| `PipelineAtomTaskBuildListener` | biz-engine | `PipelineBuildAtomTaskEvent` |
| `PipelineBuildFinishListener` | biz-engine | `PipelineBuildFinishEvent` |
| `PipelineBuildCancelListener` | biz-engine | `PipelineBuildCancelEvent` |

### 4.5 Control 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `BuildStartControl` | 41KB | 构建启动控制 |
| `StageControl` | 10KB | 阶段调度控制 |
| `ContainerControl` | 13KB | 容器调度控制 |
| `TaskControl` | 12KB | 任务调度控制 |
| `BuildEndControl` | 27KB | 构建结束控制 |
| `MutexControl` | 26KB | 互斥锁控制 |

### 4.6 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `PipelineBuildDao` | 80KB+ | 构建历史数据访问 |
| `PipelineInfoDao` | 30KB | 流水线信息数据访问 |
| `PipelineResourceDao` | 15KB | 流水线模型数据访问 |

## 五、开发规范

### 5.1 新增功能检查清单

- [ ] API 接口定义在 `api-process` 模块
- [ ] API 实现在 `biz-process` 模块
- [ ] 核心服务在 `biz-base` 模块
- [ ] DAO 使用 JOOQ，禁止手写 SQL
- [ ] 事件通过 `PipelineEventDispatcher` 分发
- [ ] 遵循命名规范（见各子 Skill）

### 5.2 常见问题

**Q: 流水线模型存储在哪里？**
A: `T_PIPELINE_RESOURCE` 表的 `MODEL` 字段，JSON 格式。

**Q: 如何扩展新的触发方式？**
A: 在 `StartType` 枚举中添加，并在 `PipelineTriggerEventService` 处理。

**Q: 事件如何分发？**
A: 通过 `PipelineEventDispatcher` 发送到 RabbitMQ，由对应 Control 消费。

---

**版本**: 2.1.0 | **更新日期**: 2025-12-10 | **补充**: 根据流程图完善触发层、拦截器层、监听器层、Dispatch 调度服务

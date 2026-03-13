---
name: process-module-architecture
description: Process 模块架构指南，用于创建/修改流水线配置、触发和调试构建任务、扩展触发方式、处理构建事件与调度逻辑。当用户进行流水线开发（pipeline 配置、CI/CD 持续集成、自动化构建）、排查构建问题、修改 Process 模块代码时使用。
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

## 二、分层架构

```
请求入口 (HTTP / 服务间调用 / MQ)
  │
  ▼
API 层 (api-process) ── UserPipelineResource, ServiceBuildResource, BuildBuildResource, UserTemplateResource
  │
  ▼
业务层 (biz-process) ── ResourceImpl 实现 → Facade Service (PipelineInfoFacade, PipelineBuildFacade, ...)
  │
  ▼
基础业务层 (biz-base) ── Engine Service (RepositoryService, RuntimeService, ...) → DAO 层
  │
  ▼
引擎层 (biz-engine) ── Control 层 (BuildStart/Stage/Container/Task/BuildEnd/MutexControl)
  │
  ▼
数据层 (model-process + MySQL: devops_process, 92 张表)
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

### 3.2 构建执行完整流程

```
触发 (Quartz/API/Webhook)
  → PipelineBuildFacadeService.buildManualStartup()
  → PipelineInterceptorChain (RunLock → Queue → TimerTriggerScmChange)
  → PipelineRuntimeService.startBuild() (生成 buildId, 创建构建记录)
  → PipelineEventDispatcher.dispatch(PipelineBuildStartEvent) → RabbitMQ
  → Listener → Control 链:
      BuildStartControl → StageControl → ContainerControl → TaskControl → BuildEndControl
  → Command Chain:
      Stage:     CheckSkip → CheckInterrupt → CheckPauseReview → StartContainer → UpdateState
      Container: CheckSkip → CheckDependOn → CheckMutex → CheckDispatch → StartTask → UpdateState
  → Dispatch (ThirdParty/VM/Docker) → Agent 领取并执行任务
```

**关键事件-监听器映射**:

| 事件 | 监听器 | 控制器 |
|------|--------|--------|
| `PipelineBuildStartEvent` | `PipelineBuildStartListener` | `BuildStartControl` |
| `PipelineBuildStageEvent` | `PipelineStageBuildListener` | `StageControl` |
| `PipelineBuildContainerEvent` | `PipelineContainerBuildListener` | `ContainerControl` |
| `PipelineBuildAtomTaskEvent` | `PipelineAtomTaskBuildListener` | `TaskControl` |
| `PipelineBuildFinishEvent` | `PipelineBuildFinishListener` | `BuildEndControl` |
| `PipelineBuildCancelEvent` | `PipelineBuildCancelListener` | `BuildCancelControl` |

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

### 4.4 Control 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `BuildStartControl` | 41KB | 构建启动控制 |
| `StageControl` | 10KB | 阶段调度控制 |
| `ContainerControl` | 13KB | 容器调度控制 |
| `TaskControl` | 12KB | 任务调度控制 |
| `BuildEndControl` | 27KB | 构建结束控制 |
| `MutexControl` | 26KB | 互斥锁控制 |

### 4.5 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `PipelineBuildDao` | 80KB+ | 构建历史数据访问 |
| `PipelineInfoDao` | 30KB | 流水线信息数据访问 |
| `PipelineResourceDao` | 15KB | 流水线模型数据访问 |

## 五、代码示例

### 5.1 通过 API 触发构建

```kotlin
// POST /api/user/builds/{projectId}/{pipelineId}
// 对应 UserBuildResource.manualStartupNew()
@Path("/{projectId}/{pipelineId}")
fun manualStartupNew(
    @PathParam("projectId") projectId: String,
    @PathParam("pipelineId") pipelineId: String,
    @RequestBody values: Map<String, String> = emptyMap()
): Result<BuildId>

// 实现入口: PipelineBuildFacadeService.buildManualStartup()
```

### 5.2 分发自定义事件

```kotlin
// 通过 PipelineEventDispatcher 发送事件到 RabbitMQ
pipelineEventDispatcher.dispatch(
    PipelineBuildStartEvent(
        source = "manualTrigger",
        projectId = projectId,
        pipelineId = pipelineId,
        userId = userId,
        buildId = buildId,
        taskId = "",
        status = BuildStatus.QUEUE,
        actionType = ActionType.START
    )
)
```

### 5.3 扩展新的触发方式

```kotlin
// 1. 在 StartType 枚举添加新类型
// src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/enums/StartType.kt
enum class StartType {
    MANUAL, TIME_TRIGGER, SERVICE, PIPELINE, WEB_HOOK, REMOTE,
    MY_CUSTOM_TRIGGER  // 新增
}

// 2. 在 PipelineTriggerEventService 中处理
fun onCustomTrigger(event: MyCustomTriggerEvent) {
    pipelineBuildFacadeService.buildManualStartup(
        userId = event.userId,
        startType = StartType.MY_CUSTOM_TRIGGER,
        projectId = event.projectId,
        pipelineId = event.pipelineId,
        values = event.params
    )
}
```

### 5.4 查询流水线模型（JOOQ DAO 模式）

```kotlin
// 流水线模型存储在 T_PIPELINE_RESOURCE 表 MODEL 字段（JSON 格式）
// 使用 PipelineResourceDao 读取:
val model = pipelineResourceDao.getLatestVersionModelString(
    dslContext = dslContext,
    projectId = projectId,
    pipelineId = pipelineId
) // 返回 JSON 字符串，反序列化为 Model 对象
```

## 六、开发检查清单

- [ ] API 接口定义在 `api-process` 模块
- [ ] API 实现在 `biz-process` 模块（`ResourceImpl` 后缀）
- [ ] 核心服务在 `biz-base` 模块
- [ ] DAO 使用 JOOQ，禁止手写 SQL
- [ ] 事件通过 `PipelineEventDispatcher` 分发
- [ ] 验证: `./gradlew :process:test` 确认无回归
- [ ] 验证: 检查 `T_PIPELINE_BUILD_HISTORY` 表确认构建记录正确写入

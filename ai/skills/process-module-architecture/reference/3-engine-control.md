

# Process 模块构建引擎 Control 层详细分析

> **模块路径**: `src/backend/ci/core/process/biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/`

## 一、Control 层概述

Control 层是 BK-CI 流水线构建引擎的**核心调度层**，负责处理 RabbitMQ 事件消息，驱动构建流程的执行。

### 1.1 目录结构

```
biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/
├── BuildStartControl.kt      # 构建启动控制器 (41KB)
├── BuildEndControl.kt        # 构建结束控制器 (27KB)
├── BuildCancelControl.kt     # 构建取消控制器 (20KB)
├── BuildMonitorControl.kt    # 构建监控控制器 (23KB)
├── StageControl.kt           # 阶段调度控制器 (10KB)
├── ContainerControl.kt       # 容器调度控制器 (13KB)
├── TaskControl.kt            # 任务调度控制器 (12KB)
├── MutexControl.kt           # 互斥锁控制器 (26KB)
├── DependOnControl.kt        # 依赖控制器 (4KB)
├── DispatchQueueControl.kt   # 调度队列控制器 (5KB)
├── HeartbeatControl.kt       # 心跳控制器 (7KB)
└── command/
    ├── stage/                # Stage 命令链
    │   ├── StageCmd.kt
    │   ├── StageCmdChain.kt
    │   └── impl/
    │       ├── CheckConditionalSkipStageCmd.kt
    │       ├── CheckInterruptStageCmd.kt
    │       ├── CheckPauseReviewStageCmd.kt
    │       ├── StartContainerStageCmd.kt
    │       └── UpdateStateForStageCmdFinally.kt
    └── container/            # Container 命令链
        ├── ContainerCmd.kt
        ├── ContainerCmdChain.kt
        └── impl/
            ├── CheckConditionalSkipContainerCmd.kt
            ├── CheckDependOnContainerCmd.kt
            ├── CheckDispatchQueueContainerCmd.kt
            ├── CheckMutexContainerCmd.kt
            ├── CheckPauseContainerCmd.kt
            ├── ContainerCmdLoop.kt
            ├── InitializeMatrixGroupStageCmd.kt
            ├── MatrixExecuteContainerCmd.kt
            ├── StartActionTaskContainerCmd.kt
            └── UpdateStateContainerCmdFinally.kt
```

### 1.2 Control 类职责矩阵

| Control 类 | 消费的事件 | 职责 |
|------------|-----------|------|
| `BuildStartControl` | `PipelineBuildStartEvent` | 构建启动、排队、并发控制 |
| `StageControl` | `PipelineBuildStageEvent` | Stage 调度、条件跳过、审核 |
| `ContainerControl` | `PipelineBuildContainerEvent` | Job 调度、互斥、矩阵执行 |
| `TaskControl` | `PipelineBuildAtomTaskEvent` | 插件任务执行 |
| `BuildEndControl` | `PipelineBuildFinishEvent` | 构建结束、状态更新、通知 |
| `BuildCancelControl` | `PipelineBuildCancelEvent` | 构建取消处理 |
| `BuildMonitorControl` | `PipelineBuildMonitorEvent` | 超时监控、状态检查 |
| `MutexControl` | - | 互斥锁管理（被其他 Control 调用） |
| `HeartbeatControl` | `PipelineContainerAgentHeartBeatEvent` | 构建机心跳处理 |

## 二、Listener 监听器层

### 2.1 监听器架构

Listener 层是 MQ 消息的入口，负责接收 RabbitMQ 事件并委托给对应的 Control 处理。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         RabbitMQ                                         │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌─────────────┐        │
│  │ build.start │ │ build.stage │ │build.container│ │ build.task │        │
│  └──────┬──────┘ └──────┬──────┘ └──────┬──────┘ └──────┬──────┘        │
└─────────┼───────────────┼───────────────┼───────────────┼────────────────┘
          │               │               │               │
          ▼               ▼               ▼               ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Listener 层                                      │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐            │
│  │PipelineBuildStart│ │PipelineStageBuild│ │PipelineContainer│            │
│  │Listener         │ │Listener         │ │BuildListener    │            │
│  │ run(event) {    │ │ run(event) {    │ │ run(event) {    │            │
│  │   buildControl  │ │   stageControl  │ │   containerControl│           │
│  │   .handle(event)│ │   .handle(event)│ │   .handle(event)│            │
│  │ }               │ │ }               │ │ }               │            │
│  └────────┬────────┘ └────────┬────────┘ └────────┬────────┘            │
└───────────┼───────────────────┼───────────────────┼──────────────────────┘
            │                   │                   │
            ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Control 层                                       │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐            │
│  │ BuildStartControl│ │ StageControl    │ │ ContainerControl │           │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 监听器目录结构

```
biz-engine/src/main/kotlin/com/tencent/devops/process/engine/listener/
├── run/
│   ├── PipelineBuildStartListener.kt       # 构建启动监听器
│   ├── PipelineStageBuildListener.kt       # Stage 调度监听器
│   ├── PipelineContainerBuildListener.kt   # Container 调度监听器
│   ├── PipelineAtomTaskBuildListener.kt    # Task 执行监听器
│   ├── PipelineTaskPauseListener.kt        # 任务暂停监听器
│   ├── finish/
│   │   ├── PipelineBuildFinishListener.kt  # 构建完成监听器
│   │   ├── PipelineBuildCancelListener.kt  # 构建取消监听器
│   │   └── SubPipelineBuildFinishListener.kt # 子流水线完成监听器
│   ├── monitor/
│   │   └── PipelineBuildMonitorListener.kt # 构建监控监听器
│   └── start/
│       └── ...                             # 启动相关监听器
```

### 2.3 监听器实现示例

**PipelineBuildStartListener** - 构建启动监听器
```kotlin
@Component
class PipelineBuildStartListener @Autowired constructor(
    private val buildControl: BuildStartControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineBuildStartEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildStartEvent) {
        buildControl.handle(event)  // 委托给 Control 处理
    }
}
```

**PipelineStageBuildListener** - Stage 调度监听器
```kotlin
@Component
class PipelineStageBuildListener @Autowired constructor(
    private val stageControl: StageControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineBuildStageEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildStageEvent) {
        stageControl.handle(event)  // 委托给 Control 处理
    }
}
```

**PipelineContainerBuildListener** - Container 调度监听器
```kotlin
@Component
class PipelineContainerBuildListener @Autowired constructor(
    private val containerControl: ContainerControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineBuildContainerEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildContainerEvent) {
        containerControl.handle(event)  // 委托给 Control 处理
    }
}
```

**PipelineAtomTaskBuildListener** - Task 执行监听器
```kotlin
@Component
class PipelineAtomTaskBuildListener @Autowired constructor(
    private val taskControl: TaskControl,
    pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineEventListener<PipelineBuildAtomTaskEvent>(pipelineEventDispatcher) {

    override fun run(event: PipelineBuildAtomTaskEvent) {
        taskControl.handle(event)  // 委托给 Control 处理
    }
}
```

### 2.4 监听器与 Control 的关系

| 监听器 | 消费事件 | 委托 Control |
|--------|----------|--------------|
| `PipelineBuildStartListener` | `PipelineBuildStartEvent` | `BuildStartControl` |
| `PipelineStageBuildListener` | `PipelineBuildStageEvent` | `StageControl` |
| `PipelineContainerBuildListener` | `PipelineBuildContainerEvent` | `ContainerControl` |
| `PipelineAtomTaskBuildListener` | `PipelineBuildAtomTaskEvent` | `TaskControl` |
| `PipelineBuildFinishListener` | `PipelineBuildFinishEvent` | `BuildEndControl` |
| `PipelineBuildCancelListener` | `PipelineBuildCancelEvent` | `BuildCancelControl` |
| `PipelineBuildMonitorListener` | `PipelineBuildMonitorEvent` | `BuildMonitorControl` |
| `PipelineTaskPauseListener` | `PipelineTaskPauseEvent` | 暂停处理逻辑 |

### 2.5 PipelineEventListener 基类

```kotlin
abstract class PipelineEventListener<T : IPipelineEvent>(
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {
    /**
     * 子类实现具体的事件处理逻辑
     */
    abstract fun run(event: T)
    
    /**
     * 统一的异常处理和重试逻辑
     */
    fun handle(event: T) {
        try {
            run(event)
        } catch (e: Exception) {
            // 异常处理、日志记录
            logger.error("Handle event error: ${event::class.simpleName}", e)
            // 可能的重试逻辑
        }
    }
}
```

## 三、构建调度流程

### 2.1 整体流程图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PipelineBuildStartEvent                          │
│                              (RabbitMQ)                                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         BuildStartControl                                │
│  1. 获取构建锁                                                           │
│  2. 检查并发控制（排队/取消）                                             │
│  3. 更新构建状态为 RUNNING                                               │
│  4. 发送 PipelineBuildStageEvent (stage-1)                              │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         StageControl                                     │
│  1. 获取 Stage 锁                                                        │
│  2. 执行命令链：                                                         │
│     - CheckConditionalSkipStageCmd (条件跳过)                            │
│     - CheckInterruptStageCmd (中断检查)                                  │
│     - CheckPauseReviewStageCmd (审核暂停)                                │
│     - StartContainerStageCmd (启动容器)                                  │
│     - UpdateStateForStageCmdFinally (状态更新)                           │
│  3. 发送 PipelineBuildContainerEvent                                    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         ContainerControl                                 │
│  1. 获取 Container 锁                                                    │
│  2. 执行命令链：                                                         │
│     - CheckConditionalSkipContainerCmd (条件跳过)                        │
│     - CheckDependOnContainerCmd (依赖检查)                               │
│     - CheckMutexContainerCmd (互斥检查)                                  │
│     - CheckDispatchQueueContainerCmd (调度队列)                          │
│     - CheckPauseContainerCmd (暂停检查)                                  │
│     - InitializeMatrixGroupStageCmd (矩阵初始化)                         │
│     - MatrixExecuteContainerCmd (矩阵执行)                               │
│     - StartActionTaskContainerCmd (启动任务)                             │
│     - UpdateStateContainerCmdFinally (状态更新)                          │
│  3. 发送 PipelineBuildAtomTaskEvent                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         TaskControl                                      │
│  1. 获取 Container 锁                                                    │
│  2. 执行插件任务                                                         │
│  3. 更新任务状态                                                         │
│  4. 发送下一个任务事件或容器完成事件                                       │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                          (循环直到所有任务完成)
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         BuildEndControl                                  │
│  1. 更新构建状态为 SUCCEED/FAILED                                        │
│  2. 发送构建完成通知                                                     │
│  3. 释放资源                                                             │
└─────────────────────────────────────────────────────────────────────────┘
```

## 三、核心 Control 类详解

### 3.1 BuildStartControl - 构建启动控制

**文件**: `BuildStartControl.kt` (41KB)

**消费事件**: `PipelineBuildStartEvent`

```kotlin
@Service
class BuildStartControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineRuntimeExtService: PipelineRuntimeExtService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineRepositoryVersionService: PipelineRepositoryVersionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val buildDetailService: PipelineBuildDetailService,
    private val buildVariableService: BuildVariableService,
    private val buildLogPrinter: BuildLogPrinter,
    private val meterRegistry: MeterRegistry
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(BuildStartControl::class.java)!!
        private const val TAG = "startVM-0"
        private const val JOB_ID = "0"
    }
}
```

**核心方法**:

```kotlin
@BkTimed
fun handle(event: PipelineBuildStartEvent) {
    val watcher = Watcher(id = "ENGINE|BuildStart|${event.buildId}")
    with(event) {
        try {
            execute(watcher)
        } catch (ignored: Throwable) {
            LOG.error("ENGINE|$buildId|$source| start fail $ignored", ignored)
        } finally {
            watcher.stop()
            LogUtils.printCostTimeWE(watcher = watcher)
        }
    }
}

private fun PipelineBuildStartEvent.execute(watcher: Watcher) {
    // 1. 获取构建锁
    val buildIdLock = BuildIdLock(redisOperation, buildId)
    buildIdLock.lock()
    try {
        // 2. 获取构建信息
        val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
            ?: return
        
        // 3. 检查构建状态
        if (buildInfo.status.isFinish()) {
            LOG.info("ENGINE|$buildId|BUILD_ALREADY_FINISH|${buildInfo.status}")
            return
        }
        
        // 4. 并发控制检查
        val setting = pipelineRepositoryService.getSetting(projectId, pipelineId)
        if (needQueue(buildInfo, setting)) {
            // 排队处理
            handleQueue(buildInfo, setting)
            return
        }
        
        // 5. 更新构建状态为 RUNNING
        pipelineRuntimeService.updateBuildStatus(
            projectId = projectId,
            buildId = buildId,
            buildStatus = BuildStatus.RUNNING
        )
        
        // 6. 发送 Stage 事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                source = "BuildStartControl",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId,
                stageId = "stage-1",  // 第一个 Stage
                actionType = ActionType.START
            )
        )
        
        // 7. 发送监控事件
        pipelineEventDispatcher.dispatch(
            PipelineBuildMonitorEvent(
                source = "BuildStartControl",
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                buildId = buildId
            )
        )
    } finally {
        buildIdLock.unlock()
    }
}
```

### 3.2 StageControl - 阶段调度控制

**文件**: `StageControl.kt` (10KB)

**消费事件**: `PipelineBuildStageEvent`

**设计模式**: **责任链模式**（Command Chain）

```kotlin
@Service
class StageControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineContainerService: PipelineContainerService,
    private val buildVariableService: BuildVariableService,
    private val pipelineContextService: PipelineContextService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineAsCodeService: PipelineAsCodeService
) {
    // 命令缓存
    private val commandCache: LoadingCache<Class<out StageCmd>, StageCmd> = 
        CacheBuilder.newBuilder()
            .maximumSize(500L)
            .build(object : CacheLoader<Class<out StageCmd>, StageCmd>() {
                override fun load(clazz: Class<out StageCmd>): StageCmd {
                    return SpringContextUtil.getBean(clazz)
                }
            })
}
```

**命令链执行**:

```kotlin
@BkTimed
fun handle(event: PipelineBuildStageEvent) {
    val stageIdLock = StageIdLock(redisOperation, buildId, stageId)
    try {
        if (!stageIdLock.tryLock()) {
            // 获取锁失败，延迟重试
            event.delayMills = 1000
            pipelineEventDispatcher.dispatch(event)
            return
        }
        execute(watcher = watcher)
    } finally {
        stageIdLock.unlock()
    }
}

private fun PipelineBuildStageEvent.execute(watcher: Watcher) {
    // 1. 获取构建信息
    val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
    if (buildInfo == null || buildInfo.status.isFinish()) {
        return
    }
    
    // 2. 获取 Stage 信息
    val stage = pipelineStageService.getStage(projectId, buildId, stageId)
        ?: return
    
    // 3. 构建命令链上下文
    val context = StageContext(
        buildInfo = buildInfo,
        stage = stage,
        event = this,
        variables = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
    )
    
    // 4. 执行命令链
    val commandChain = StageCmdChain(
        listOf(
            commandCache.get(CheckConditionalSkipStageCmd::class.java),
            commandCache.get(CheckInterruptStageCmd::class.java),
            commandCache.get(CheckPauseReviewStageCmd::class.java),
            commandCache.get(StartContainerStageCmd::class.java),
            commandCache.get(UpdateStateForStageCmdFinally::class.java)
        )
    )
    commandChain.doCommand(context)
}
```

**Stage 命令链**:

| 命令 | 职责 | 执行条件 |
|------|------|----------|
| `CheckConditionalSkipStageCmd` | 检查条件跳过 | Stage 有条件表达式 |
| `CheckInterruptStageCmd` | 检查中断 | 构建被取消 |
| `CheckPauseReviewStageCmd` | 检查审核暂停 | Stage 需要人工审核 |
| `StartContainerStageCmd` | 启动容器 | Stage 可执行 |
| `UpdateStateForStageCmdFinally` | 更新状态 | 始终执行 |

### 3.3 ContainerControl - 容器调度控制

**文件**: `ContainerControl.kt` (13KB)

**消费事件**: `PipelineBuildContainerEvent`

**设计模式**: **责任链模式**（Command Chain）

```kotlin
@Service
class ContainerControl @Autowired constructor(
    private val buildLogPrinter: BuildLogPrinter,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildVariableService: BuildVariableService,
    private val pipelineAsCodeService: PipelineAsCodeService
) {
    private val commandCache: LoadingCache<Class<out ContainerCmd>, ContainerCmd> = 
        CacheBuilder.newBuilder()
            .maximumSize(500L)
            .build(...)
}
```

**Container 命令链**:

| 命令 | 职责 | 执行条件 |
|------|------|----------|
| `CheckConditionalSkipContainerCmd` | 检查条件跳过 | Job 有条件表达式 |
| `CheckDependOnContainerCmd` | 检查依赖 | Job 有 dependOn 配置 |
| `CheckMutexContainerCmd` | 检查互斥 | Job 有互斥组配置 |
| `CheckDispatchQueueContainerCmd` | 检查调度队列 | 需要排队 |
| `CheckPauseContainerCmd` | 检查暂停 | Job 被暂停 |
| `InitializeMatrixGroupStageCmd` | 矩阵初始化 | 矩阵 Job |
| `MatrixExecuteContainerCmd` | 矩阵执行 | 矩阵 Job |
| `StartActionTaskContainerCmd` | 启动任务 | Job 可执行 |
| `UpdateStateContainerCmdFinally` | 更新状态 | 始终执行 |

### 3.4 TaskControl - 任务调度控制

**文件**: `TaskControl.kt` (12KB)

**消费事件**: `PipelineBuildAtomTaskEvent`

```kotlin
@Service
class TaskControl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val taskAtomService: TaskAtomService,
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineBuildTaskService: PipelineBuildTaskService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(TaskControl::class.java)
    }
}
```

**核心逻辑**:

```kotlin
@BkTimed
fun handle(event: PipelineBuildAtomTaskEvent) {
    val containerIdLock = ContainerIdLock(redisOperation, buildId, containerId)
    try {
        containerIdLock.lock()
        execute()
    } finally {
        containerIdLock.unlock()
    }
}

private fun PipelineBuildAtomTaskEvent.execute() {
    // 1. 获取构建信息
    val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
    val buildTask = pipelineTaskService.getBuildTask(projectId, buildId, taskId)
    
    // 2. 检查执行次数
    if (executeCount != buildTask?.executeCount) {
        LOG.info("ENGINE|$buildId|BAD_EC_WARN")
        return
    }
    
    // 3. 检查构建状态
    if (buildInfo?.status?.isFinish() == true || buildTask.status.isFinish()) {
        return
    }
    
    // 4. 判断任务类型
    if (taskAtomService.runByVmTask(buildTask)) {
        // 构建机上执行的任务
        handleVmTask(buildTask)
    } else {
        // 后端执行的任务
        val atomResponse = taskAtomService.execute(buildTask)
        handleAtomResponse(buildTask, atomResponse)
    }
}
```

### 3.5 BuildEndControl - 构建结束控制

**文件**: `BuildEndControl.kt` (27KB)

**消费事件**: `PipelineBuildFinishEvent`

```kotlin
@Service
class BuildEndControl @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val redisOperation: RedisOperation,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildVariableService: BuildVariableService,
    private val pipelineBuildRecordService: PipelineBuildRecordService
) {
    companion object {
        private val LOG = LoggerFactory.getLogger(BuildEndControl::class.java)
    }
}
```

**核心逻辑**:

```kotlin
@BkTimed
fun handle(event: PipelineBuildFinishEvent) {
    with(event) {
        val buildIdLock = BuildIdLock(redisOperation, buildId)
        buildIdLock.lock()
        try {
            execute()
        } finally {
            buildIdLock.unlock()
        }
    }
}

private fun PipelineBuildFinishEvent.execute() {
    // 1. 获取构建信息
    val buildInfo = pipelineRuntimeService.getBuildInfo(projectId, buildId)
        ?: return
    
    // 2. 计算最终状态
    val finalStatus = calculateFinalStatus(buildInfo)
    
    // 3. 更新构建状态
    pipelineRuntimeService.updateBuildStatus(
        projectId = projectId,
        buildId = buildId,
        buildStatus = finalStatus
    )
    
    // 4. 更新构建摘要
    pipelineRuntimeService.updateBuildSummary(projectId, pipelineId, buildId)
    
    // 5. 发送通知事件
    pipelineEventDispatcher.dispatch(
        PipelineBuildNotifyEvent(
            source = "BuildEndControl",
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            buildId = buildId,
            buildStatus = finalStatus
        )
    )
    
    // 6. 发送广播事件
    pipelineEventDispatcher.dispatch(
        PipelineBuildStatusBroadCastEvent(
            source = "BuildEndControl",
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            buildId = buildId,
            actionType = PipelineBuildStatusBroadCastEventType.END
        )
    )
}
```

### 3.6 MutexControl - 互斥锁控制

**文件**: `MutexControl.kt` (26KB)

**职责**: 管理 Job 级别的互斥锁，确保同一互斥组内只有一个 Job 运行

```kotlin
@Service
class MutexControl @Autowired constructor(
    private val redisOperation: RedisOperation,
    private val pipelineContainerService: PipelineContainerService,
    private val buildLogPrinter: BuildLogPrinter
) {
    companion object {
        private const val MUTEX_LOCK_KEY_PREFIX = "process:pipeline:mutex:"
        private const val MUTEX_QUEUE_KEY_PREFIX = "process:pipeline:mutex:queue:"
    }
    
    /**
     * 尝试获取互斥锁
     */
    fun tryLock(
        mutexGroup: String,
        projectId: String,
        pipelineId: String,
        buildId: String,
        containerId: String
    ): MutexResult {
        val lockKey = "$MUTEX_LOCK_KEY_PREFIX$projectId:$mutexGroup"
        val lockValue = "$buildId:$containerId"
        
        // 尝试获取锁
        val acquired = redisOperation.setIfAbsent(lockKey, lockValue, 3600)
        if (acquired) {
            return MutexResult.ACQUIRED
        }
        
        // 检查是否是自己持有的锁
        val currentHolder = redisOperation.get(lockKey)
        if (currentHolder == lockValue) {
            return MutexResult.ACQUIRED
        }
        
        // 加入等待队列
        val queueKey = "$MUTEX_QUEUE_KEY_PREFIX$projectId:$mutexGroup"
        redisOperation.zadd(queueKey, System.currentTimeMillis().toDouble(), lockValue)
        
        return MutexResult.WAITING
    }
    
    /**
     * 释放互斥锁
     */
    fun unlock(
        mutexGroup: String,
        projectId: String,
        buildId: String,
        containerId: String
    ) {
        val lockKey = "$MUTEX_LOCK_KEY_PREFIX$projectId:$mutexGroup"
        val lockValue = "$buildId:$containerId"
        
        val currentHolder = redisOperation.get(lockKey)
        if (currentHolder == lockValue) {
            redisOperation.delete(lockKey)
            // 通知队列中的下一个
            notifyNext(projectId, mutexGroup)
        }
    }
}
```

## 四、命令链模式详解

### 4.1 StageCmd 接口

```kotlin
interface StageCmd {
    /**
     * 是否可以执行
     */
    fun canExecute(context: StageContext): Boolean
    
    /**
     * 执行命令
     */
    fun execute(context: StageContext)
}
```

### 4.2 StageCmdChain 实现

```kotlin
class StageCmdChain(private val commands: List<StageCmd>) {
    
    fun doCommand(context: StageContext) {
        for (cmd in commands) {
            if (cmd.canExecute(context)) {
                cmd.execute(context)
                if (context.cmdFlowState == CmdFlowState.BREAK) {
                    break  // 中断命令链
                }
            }
        }
    }
}
```

### 4.3 示例命令实现

```kotlin
@Service
class CheckConditionalSkipStageCmd @Autowired constructor(
    private val pipelineStageService: PipelineStageService,
    private val expressionParser: ExpressionParser
) : StageCmd {
    
    override fun canExecute(context: StageContext): Boolean {
        // 只有 Stage 状态为 QUEUE 且有条件表达式时才执行
        return context.stage.status == BuildStatus.QUEUE 
            && context.stage.controlOption?.runCondition != null
    }
    
    override fun execute(context: StageContext) {
        val condition = context.stage.controlOption?.runCondition
        val variables = context.variables
        
        // 解析条件表达式
        val shouldSkip = !expressionParser.evaluate(condition, variables)
        
        if (shouldSkip) {
            // 跳过 Stage
            pipelineStageService.updateStageStatus(
                projectId = context.stage.projectId,
                buildId = context.stage.buildId,
                stageId = context.stage.stageId,
                buildStatus = BuildStatus.SKIP
            )
            context.cmdFlowState = CmdFlowState.BREAK  // 中断命令链
        }
    }
}
```

## 五、锁机制

### 5.1 锁类型

| 锁类 | 用途 | 粒度 |
|------|------|------|
| `BuildIdLock` | 构建级别锁 | buildId |
| `StageIdLock` | Stage 级别锁 | buildId + stageId |
| `ContainerIdLock` | Container 级别锁 | buildId + containerId |
| `PipelineBuildStartLock` | 构建启动锁 | pipelineId |
| `ConcurrencyGroupLock` | 并发组锁 | concurrencyGroup |

### 5.2 锁使用示例

```kotlin
class BuildIdLock(
    private val redisOperation: RedisOperation,
    private val buildId: String
) {
    private val lockKey = "process:build:lock:$buildId"
    
    fun lock() {
        redisOperation.lock(lockKey, 60)
    }
    
    fun tryLock(): Boolean {
        return redisOperation.tryLock(lockKey, 60)
    }
    
    fun unlock() {
        redisOperation.unlock(lockKey)
    }
}
```

## 六、开发规范

### 6.1 新增 Control 检查清单

- [ ] 实现 `@BkTimed` 注解的 `handle()` 方法
- [ ] 使用适当粒度的锁
- [ ] 处理异常并记录日志
- [ ] 使用 `Watcher` 记录耗时
- [ ] 通过 `PipelineEventDispatcher` 发送后续事件

### 6.2 新增命令检查清单

- [ ] 实现 `StageCmd` 或 `ContainerCmd` 接口
- [ ] 实现 `canExecute()` 判断条件
- [ ] 实现 `execute()` 业务逻辑
- [ ] 必要时设置 `context.cmdFlowState = CmdFlowState.BREAK`

---

**版本**: 1.1.0 | **更新日期**: 2025-12-10 | **补充**: 添加 Listener 监听器层详解

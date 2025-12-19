---
name: 29-5-process-event-driven
description: Process 模块事件驱动机制详解
---

# Process 模块事件驱动机制详解

> **模块路径**: `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/pojo/event/`

## 一、事件驱动架构概述

BK-CI 流水线引擎采用**事件驱动架构**，通过 RabbitMQ 消息队列实现异步解耦和分布式调度。

### 1.1 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         事件生产者 (Producer)                            │
│  PipelineRuntimeService | Control 类 | Facade Service                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         PipelineEventDispatcher                          │
│                      (事件分发器)                                        │
│  dispatch(event) ─────────────────────────────────────────────────────► │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         RabbitMQ                                         │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐            │
│  │ pipeline.build  │ │ pipeline.build  │ │ pipeline.build  │            │
│  │ .start          │ │ .stage          │ │ .container      │            │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘            │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐            │
│  │ pipeline.build  │ │ pipeline.build  │ │ pipeline.build  │            │
│  │ .task           │ │ .finish         │ │ .cancel         │            │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘            │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         事件消费者 (Consumer)                            │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐            │
│  │BuildStartControl│ │ StageControl    │ │ContainerControl │            │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘            │
│  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐            │
│  │ TaskControl     │ │ BuildEndControl │ │BuildCancelControl│           │
│  └─────────────────┘ └─────────────────┘ └─────────────────┘            │
└─────────────────────────────────────────────────────────────────────────┘
```

### 1.2 事件类型总览

```
biz-base/src/main/kotlin/com/tencent/devops/process/engine/pojo/event/
├── PipelineBuildStartEvent.kt          # 构建启动事件
├── PipelineBuildStageEvent.kt          # 阶段调度事件
├── PipelineBuildContainerEvent.kt      # 容器调度事件
├── PipelineBuildAtomTaskEvent.kt       # 任务执行事件
├── PipelineBuildFinishEvent.kt         # 构建完成事件
├── PipelineBuildCancelEvent.kt         # 构建取消事件
├── PipelineBuildMonitorEvent.kt        # 构建监控事件
├── PipelineBuildNotifyEvent.kt         # 构建通知事件
├── PipelineBuildWebSocketPushEvent.kt  # WebSocket 推送事件
├── PipelineBuildReviewReminderEvent.kt # 审核提醒事件
├── PipelineContainerAgentHeartBeatEvent.kt # Agent 心跳事件
├── PipelineTaskPauseEvent.kt           # 任务暂停事件
├── PipelineCreateEvent.kt              # 流水线创建事件
├── PipelineUpdateEvent.kt              # 流水线更新事件
├── PipelineDeleteEvent.kt              # 流水线删除事件
├── PipelineRestoreEvent.kt             # 流水线恢复事件
├── PipelineTemplateInstanceEvent.kt    # 模板实例化事件
├── PipelineTemplateMigrateEvent.kt     # 模板迁移事件
└── PipelineTemplateTriggerUpgradesEvent.kt # 模板触发升级事件
```

## 二、核心事件类详解

### 2.1 IPipelineEvent - 事件基类

```kotlin
package com.tencent.devops.common.event.pojo.pipeline

/**
 * 流水线事件基类
 */
abstract class IPipelineEvent(
    open var actionType: ActionType,      // 动作类型
    open val source: String,              // 事件来源
    open val projectId: String,           // 项目ID
    open val pipelineId: String,          // 流水线ID
    open val userId: String,              // 用户ID
    open var delayMills: Int = 0          // 延迟毫秒数
)
```

### 2.2 PipelineBuildStartEvent - 构建启动事件

**文件**: `PipelineBuildStartEvent.kt`

**消费者**: `BuildStartControl`

**队列**: `pipeline.build.start`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_START)
data class PipelineBuildStartEvent(
    override val source: String,           // 事件来源
    override val projectId: String,        // 项目ID
    override val pipelineId: String,       // 流水线ID
    override val userId: String,           // 用户ID
    val buildId: String,                   // 构建ID
    val taskId: String,                    // 任务ID
    val status: BuildStatus? = null,       // 构建状态
    override var actionType: ActionType,   // 动作类型
    override var delayMills: Int = 0,      // 延迟毫秒
    val buildNoType: BuildNoType? = null,  // 构建号类型
    val executeCount: Int = 1,             // 执行次数
    val debug: Boolean? = false            // 是否调试
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- `PipelineBuildFacadeService.startPipeline()` 启动构建时

**处理逻辑**:
1. 获取构建锁
2. 检查并发控制
3. 更新构建状态为 RUNNING
4. 发送 `PipelineBuildStageEvent`

### 2.3 PipelineBuildStageEvent - 阶段调度事件

**文件**: `PipelineBuildStageEvent.kt`

**消费者**: `StageControl`

**队列**: `pipeline.build.stage`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_STAGE)
data class PipelineBuildStageEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val stageId: String,                   // 阶段ID
    override var actionType: ActionType,
    override var delayMills: Int = 0,
    val executeCount: Int = 1
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- `BuildStartControl` 启动第一个 Stage
- `StageControl` 启动下一个 Stage
- Stage 完成后触发下一个 Stage

**处理逻辑**:
1. 获取 Stage 锁
2. 执行命令链（条件检查、审核、启动容器）
3. 发送 `PipelineBuildContainerEvent`

### 2.4 PipelineBuildContainerEvent - 容器调度事件

**文件**: `PipelineBuildContainerEvent.kt`

**消费者**: `ContainerControl`

**队列**: `pipeline.build.container`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_CONTAINER)
data class PipelineBuildContainerEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val stageId: String,
    val containerId: String,               // 容器ID
    val containerHashId: String? = null,   // 容器哈希ID
    val containerType: String,             // 容器类型
    override var actionType: ActionType,
    override var delayMills: Int = 0,
    val executeCount: Int = 1,
    val reason: String? = null,            // 原因
    val sendEventCount: Int = 0,           // 发送事件计数
    val errorCode: Int? = null,            // 错误码
    val errorTypeName: String? = null      // 错误类型
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- `StageControl` 启动 Stage 下的容器
- `ContainerControl` 容器状态变更

**处理逻辑**:
1. 获取 Container 锁
2. 执行命令链（依赖检查、互斥、矩阵执行）
3. 发送 `PipelineBuildAtomTaskEvent`

### 2.5 PipelineBuildAtomTaskEvent - 任务执行事件

**文件**: `PipelineBuildAtomTaskEvent.kt`

**消费者**: `TaskControl`

**队列**: `pipeline.build.task`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_ATOM_TASK)
data class PipelineBuildAtomTaskEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val stageId: String,
    val containerId: String,
    val containerHashId: String? = null,
    val containerType: String,
    val taskId: String,                    // 任务ID
    val taskParam: TaskParam,              // 任务参数
    override var actionType: ActionType,
    override var delayMills: Int = 0,
    val executeCount: Int = 1,
    val errorCode: Int? = null,
    val errorTypeName: String? = null
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- `ContainerControl` 启动任务
- `TaskControl` 任务完成后启动下一个任务

**处理逻辑**:
1. 获取 Container 锁
2. 执行插件任务
3. 更新任务状态
4. 发送下一个任务事件或容器完成事件

### 2.6 PipelineBuildFinishEvent - 构建完成事件

**文件**: `PipelineBuildFinishEvent.kt`

**消费者**: `BuildEndControl`

**队列**: `pipeline.build.finish`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_FINISH)
data class PipelineBuildFinishEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val status: BuildStatus,               // 最终状态
    override var actionType: ActionType = ActionType.END,
    override var delayMills: Int = 0,
    val executeCount: Int = 1,
    val errorInfoList: List<ErrorInfo>? = null  // 错误信息列表
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- 所有 Stage 执行完成
- 构建被取消
- 构建超时

**处理逻辑**:
1. 更新构建最终状态
2. 更新构建摘要
3. 发送通知事件
4. 释放资源

### 2.7 PipelineBuildCancelEvent - 构建取消事件

**文件**: `PipelineBuildCancelEvent.kt`

**消费者**: `BuildCancelControl`

**队列**: `pipeline.build.cancel`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_CANCEL)
data class PipelineBuildCancelEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    val status: BuildStatus,               // 取消状态
    override var actionType: ActionType = ActionType.TERMINATE,
    override var delayMills: Int = 0,
    val executeCount: Int = 1
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- 用户手动取消构建
- 系统强制终止

**处理逻辑**:
1. 终止所有运行中的任务
2. 更新构建状态为 CANCELED
3. 发送 `PipelineBuildFinishEvent`

### 2.8 PipelineBuildMonitorEvent - 构建监控事件

**文件**: `PipelineBuildMonitorEvent.kt`

**消费者**: `BuildMonitorControl`

**队列**: `pipeline.build.monitor`

```kotlin
@Event(StreamBinding.PIPELINE_BUILD_MONITOR)
data class PipelineBuildMonitorEvent(
    override val source: String,
    override val projectId: String,
    override val pipelineId: String,
    override val userId: String,
    val buildId: String,
    override var actionType: ActionType = ActionType.REFRESH,
    override var delayMills: Int = 60000   // 默认 60 秒
) : IPipelineEvent(actionType, source, projectId, pipelineId, userId, delayMills)
```

**触发时机**:
- `BuildStartControl` 构建启动后
- `BuildMonitorControl` 周期性自触发

**处理逻辑**:
1. 检查构建是否超时
2. 检查任务是否心跳超时
3. 如果构建未结束，延迟后再次发送监控事件

## 三、事件分发器

### 3.1 PipelineEventDispatcher

**文件**: `common-event/src/main/kotlin/com/tencent/devops/common/event/dispatcher/pipeline/PipelineEventDispatcher.kt`

```kotlin
@Component
class PipelineEventDispatcher @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate
) {
    
    /**
     * 分发单个事件
     */
    fun dispatch(event: IPipelineEvent) {
        val routingKey = getRoutingKey(event)
        val exchange = getExchange(event)
        
        if (event.delayMills > 0) {
            // 延迟消息
            rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                event
            ) { message ->
                message.messageProperties.delay = event.delayMills
                message
            }
        } else {
            rabbitTemplate.convertAndSend(exchange, routingKey, event)
        }
    }
    
    /**
     * 批量分发事件
     */
    fun dispatch(vararg events: IPipelineEvent) {
        events.forEach { dispatch(it) }
    }
    
    private fun getRoutingKey(event: IPipelineEvent): String {
        return when (event) {
            is PipelineBuildStartEvent -> StreamBinding.PIPELINE_BUILD_START
            is PipelineBuildStageEvent -> StreamBinding.PIPELINE_BUILD_STAGE
            is PipelineBuildContainerEvent -> StreamBinding.PIPELINE_BUILD_CONTAINER
            is PipelineBuildAtomTaskEvent -> StreamBinding.PIPELINE_BUILD_ATOM_TASK
            is PipelineBuildFinishEvent -> StreamBinding.PIPELINE_BUILD_FINISH
            is PipelineBuildCancelEvent -> StreamBinding.PIPELINE_BUILD_CANCEL
            is PipelineBuildMonitorEvent -> StreamBinding.PIPELINE_BUILD_MONITOR
            else -> throw IllegalArgumentException("Unknown event type: ${event::class}")
        }
    }
}
```

### 3.2 StreamBinding 常量

```kotlin
object StreamBinding {
    const val PIPELINE_BUILD_START = "pipeline.build.start"
    const val PIPELINE_BUILD_STAGE = "pipeline.build.stage"
    const val PIPELINE_BUILD_CONTAINER = "pipeline.build.container"
    const val PIPELINE_BUILD_ATOM_TASK = "pipeline.build.task"
    const val PIPELINE_BUILD_FINISH = "pipeline.build.finish"
    const val PIPELINE_BUILD_CANCEL = "pipeline.build.cancel"
    const val PIPELINE_BUILD_MONITOR = "pipeline.build.monitor"
    const val PIPELINE_BUILD_NOTIFY = "pipeline.build.notify"
    const val PIPELINE_BUILD_WEBSOCKET = "pipeline.build.websocket"
    const val PIPELINE_BUILD_HEARTBEAT = "pipeline.build.heartbeat"
}
```

## 四、事件监听器

### 4.1 监听器配置

**文件**: `biz-engine/src/main/kotlin/com/tencent/devops/process/engine/listener/`

```kotlin
@Configuration
class PipelineBuildListenerConfiguration {
    
    @Bean
    fun buildStartListener(
        buildStartControl: BuildStartControl
    ): Consumer<PipelineBuildStartEvent> {
        return Consumer { event ->
            buildStartControl.handle(event)
        }
    }
    
    @Bean
    fun buildStageListener(
        stageControl: StageControl
    ): Consumer<PipelineBuildStageEvent> {
        return Consumer { event ->
            stageControl.handle(event)
        }
    }
    
    @Bean
    fun buildContainerListener(
        containerControl: ContainerControl
    ): Consumer<PipelineBuildContainerEvent> {
        return Consumer { event ->
            containerControl.handle(event)
        }
    }
    
    @Bean
    fun buildTaskListener(
        taskControl: TaskControl
    ): Consumer<PipelineBuildAtomTaskEvent> {
        return Consumer { event ->
            taskControl.handle(event)
        }
    }
    
    @Bean
    fun buildFinishListener(
        buildEndControl: BuildEndControl
    ): Consumer<PipelineBuildFinishEvent> {
        return Consumer { event ->
            buildEndControl.handle(event)
        }
    }
}
```

## 五、事件流转示例

### 5.1 正常构建流程

```
用户触发构建
    │
    ▼
PipelineBuildFacadeService.startPipeline()
    │
    ├─► pipelineRuntimeService.startBuild()  // 创建构建记录
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildStartEvent(
                buildId = "b-xxx",
                actionType = ActionType.START
            )
        )
    │
    ▼
BuildStartControl.handle()
    │
    ├─► 并发控制检查
    ├─► 更新状态为 RUNNING
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildStageEvent(
                stageId = "stage-1"
            ),
            PipelineBuildMonitorEvent()
        )
    │
    ▼
StageControl.handle()
    │
    ├─► 执行命令链
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildContainerEvent(
                containerId = "1"
            )
        )
    │
    ▼
ContainerControl.handle()
    │
    ├─► 执行命令链
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildAtomTaskEvent(
                taskId = "T-1-1-1"
            )
        )
    │
    ▼
TaskControl.handle()
    │
    ├─► 执行插件任务
    ├─► 更新任务状态
    │
    └─► 如果还有任务:
            pipelineEventDispatcher.dispatch(
                PipelineBuildAtomTaskEvent(taskId = "T-1-1-2")
            )
        否则:
            pipelineEventDispatcher.dispatch(
                PipelineBuildContainerEvent(actionType = END)
            )
    │
    ▼
... (循环直到所有 Stage 完成)
    │
    ▼
pipelineEventDispatcher.dispatch(
    PipelineBuildFinishEvent(
        status = BuildStatus.SUCCEED
    )
)
    │
    ▼
BuildEndControl.handle()
    │
    ├─► 更新构建最终状态
    ├─► 更新构建摘要
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildNotifyEvent()
        )
```

### 5.2 取消构建流程

```
用户取消构建
    │
    ▼
PipelineBuildFacadeService.buildManualShutdown()
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildCancelEvent(
                status = BuildStatus.CANCELED
            )
        )
    │
    ▼
BuildCancelControl.handle()
    │
    ├─► 终止所有运行中的任务
    ├─► 更新任务状态为 CANCELED
    │
    └─► pipelineEventDispatcher.dispatch(
            PipelineBuildFinishEvent(
                status = BuildStatus.CANCELED
            )
        )
    │
    ▼
BuildEndControl.handle()
    │
    └─► 更新构建最终状态
```

## 六、Dispatch 调度服务

### 6.1 Dispatch 服务架构

Dispatch 服务负责将构建任务分配给合适的构建机（Agent）。

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Process 服务                                     │
│  ContainerControl ─► 发送 PipelineAgentStartupEvent                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Dispatch 服务                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ ThirdPartyBuildListener                                              ││
│  │   消费 PipelineAgentStartupEvent                                     ││
│  │   └─► ThirdPartyDispatchService.startUp()                           ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                    │                                     │
│                                    ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ ThirdPartyDispatchService (构建机调度服务)                            ││
│  │   ├─► ThirdPartyAgentIDDispatchType (指定 Agent ID)                  ││
│  │   │     └─► buildByAgentId()                                        ││
│  │   └─► ThirdPartyAgentEnvDispatchType (环境调度)                       ││
│  │         └─► buildByEnv()                                            ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                    │                                     │
│                                    ▼                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ 调度策略                                                             ││
│  │   ├─► TPAQueueService (第三方 Agent 队列服务)                         ││
│  │   ├─► TPASingleQueueService (单 Agent 队列)                          ││
│  │   └─► TPAEnvQueueService (环境队列)                                  ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         构建机 Agent                                     │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ Agent 轮询 REST API                                                  ││
│  │   BuildAgentBuildResource.claimBuildTask()  ← 领取任务               ││
│  │   BuildAgentBuildResource.completeTask()    ← 完成回调               ││
│  │   BuildAgentBuildResource.heartbeat()       ← 心跳上报               ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
```

### 6.2 Dispatch 服务核心类

| 类名 | 模块 | 职责 |
|------|------|------|
| `ThirdPartyBuildListener` | dispatch | 监听 Agent 启动事件 |
| `ThirdPartyDispatchService` | dispatch | 第三方构建机调度 |
| `ThirdPartyAgentService` | dispatch | Agent 管理服务 |
| `TPAQueueService` | dispatch | Agent 队列服务 |
| `BuildAgentBuildResourceImpl` | dispatch | Agent REST API 实现 |

### 6.3 ThirdPartyDispatchService 核心逻辑

```kotlin
@Service
class ThirdPartyDispatchService @Autowired constructor(
    private val client: Client,
    private val redisOperation: RedisOperation,
    private val buildLogPrinter: BuildLogPrinter,
    private val thirdPartyAgentBuildService: ThirdPartyAgentService,
    private val tpaQueueService: TPAQueueService,
    private val tpaSingleQueueService: TPASingleQueueService
) {
    fun canDispatch(event: PipelineAgentStartupEvent) =
        event.dispatchType is ThirdPartyAgentIDDispatchType ||
        event.dispatchType is ThirdPartyAgentEnvDispatchType ||
        event.dispatchType is ThirdPartyDevCloudDispatchType

    fun startUp(dispatchMessage: DispatchMessage) {
        when (dispatchMessage.event.dispatchType) {
            is ThirdPartyAgentIDDispatchType -> {
                // 指定 Agent ID 调度
                val dispatchType = dispatchMessage.event.dispatchType as ThirdPartyAgentIDDispatchType
                buildByAgentId(dispatchMessage, dispatchType)
            }
            is ThirdPartyAgentEnvDispatchType -> {
                // 环境调度
                val dispatchType = dispatchMessage.event.dispatchType as ThirdPartyAgentEnvDispatchType
                buildByEnv(dispatchMessage, dispatchType)
            }
        }
    }
    
    private fun buildByAgentId(dispatchMessage: DispatchMessage, dispatchType: ThirdPartyAgentIDDispatchType) {
        // 1. 获取指定的 Agent
        val agent = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentById(projectId, dispatchType.displayName).data
        
        // 2. 检查 Agent 状态
        if (agent?.status != AgentStatus.IMPORT_OK) {
            throw BuildFailureException("Agent 不可用")
        }
        
        // 3. 将任务加入队列
        tpaSingleQueueService.addQueue(dispatchMessage, agent)
    }
    
    private fun buildByEnv(dispatchMessage: DispatchMessage, dispatchType: ThirdPartyAgentEnvDispatchType) {
        // 1. 获取环境下的所有 Agent
        val agents = client.get(ServiceThirdPartyAgentResource::class)
            .getAgentsByEnvId(projectId, dispatchType.envId).data
        
        // 2. 选择最优 Agent（负载均衡）
        val selectedAgent = selectBestAgent(agents)
        
        // 3. 将任务加入队列
        tpaQueueService.addQueue(dispatchMessage, selectedAgent)
    }
}
```

### 6.4 Agent 任务领取流程

```
Agent 启动
    │
    ▼
循环轮询 claimBuildTask()
    │
    ├─► 有任务 ─► 执行任务 ─► completeTask() ─► 继续轮询
    │
    └─► 无任务 ─► 等待 ─► 继续轮询
    
同时：
    └─► 定期 heartbeat() 上报心跳
```

### 6.5 BuildAgentBuildResource API

```kotlin
interface BuildAgentBuildResource {
    @POST
    @Path("/claim")
    fun claimBuildTask(
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID) projectId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID) agentId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY) secretKey: String
    ): Result<ThirdPartyBuildInfo?>
    
    @PUT
    @Path("/complete")
    fun completeTask(
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID) projectId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID) agentId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_SECRET_KEY) secretKey: String,
        buildInfo: ThirdPartyBuildWithStatus
    ): Result<Boolean>
    
    @POST
    @Path("/heartbeat")
    fun heartbeat(
        @HeaderParam(AUTH_HEADER_DEVOPS_PROJECT_ID) projectId: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_AGENT_ID) agentId: String,
        heartbeatInfo: ThirdPartyAgentHeartbeatInfo
    ): Result<HeartbeatResponse>
}
```

## 七、ActionType 动作类型

```kotlin
enum class ActionType {
    START,      // 启动
    END,        // 结束
    TERMINATE,  // 终止
    REFRESH,    // 刷新
    RETRY,      // 重试
    SKIP        // 跳过
}
```

**使用场景**:

| ActionType | 使用场景 |
|------------|----------|
| `START` | 启动构建、启动 Stage、启动 Container、启动 Task |
| `END` | 正常结束 |
| `TERMINATE` | 强制终止（取消、超时） |
| `REFRESH` | 刷新状态（监控事件） |
| `RETRY` | 重试执行 |
| `SKIP` | 跳过执行 |

## 八、开发规范

### 7.1 新增事件检查清单

- [ ] 继承 `IPipelineEvent` 基类
- [ ] 添加 `@Event(StreamBinding.XXX)` 注解
- [ ] 在 `StreamBinding` 中定义常量
- [ ] 在 `PipelineEventDispatcher` 中添加路由
- [ ] 创建对应的 Control 类消费事件
- [ ] 在 `ListenerConfiguration` 中注册监听器

### 7.2 事件发送规范

```kotlin
// 1. 使用 PipelineEventDispatcher 发送事件
pipelineEventDispatcher.dispatch(
    PipelineBuildStageEvent(
        source = "BuildStartControl",  // 标明来源
        projectId = projectId,
        pipelineId = pipelineId,
        userId = userId,
        buildId = buildId,
        stageId = stageId,
        actionType = ActionType.START
    )
)

// 2. 延迟发送
pipelineEventDispatcher.dispatch(
    PipelineBuildMonitorEvent(
        ...
        delayMills = 60000  // 60 秒后执行
    )
)

// 3. 批量发送
pipelineEventDispatcher.dispatch(
    PipelineBuildStageEvent(...),
    PipelineBuildMonitorEvent(...)
)
```

### 7.3 事件处理规范

```kotlin
@Service
class XxxControl @Autowired constructor(...) {
    
    @BkTimed  // 添加监控
    fun handle(event: XxxEvent) {
        val watcher = Watcher(id = "ENGINE|Xxx|${event.buildId}")
        try {
            // 1. 获取锁
            val lock = XxxLock(redisOperation, event.buildId)
            lock.lock()
            try {
                // 2. 执行业务逻辑
                execute(event)
            } finally {
                // 3. 释放锁
                lock.unlock()
            }
        } catch (e: Exception) {
            // 4. 异常处理
            LOG.error("ENGINE|${event.buildId}|ERROR|${e.message}", e)
        } finally {
            // 5. 记录耗时
            watcher.stop()
            LogUtils.printCostTimeWE(watcher)
        }
    }
}
```

---

**版本**: 1.1.0 | **更新日期**: 2025-12-10 | **补充**: 添加 Dispatch 调度服务详解

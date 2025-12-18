---
name: 29-2-process-service-layer
description: Process 模块 Service 业务层详细分析
---

# Process 模块 Service 业务层详细分析

> **模块路径**: `src/backend/ci/core/process/biz-process/src/main/kotlin/com/tencent/devops/process/service/` 和 `src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/`

## 一、Service 层架构概述

Process 模块的 Service 层采用**双层架构**设计：

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Facade Service 层 (biz-process)                  │
│  职责：业务编排、权限校验、参数转换、调用 Engine Service                    │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐         │
│  │PipelineInfoFacade│ │PipelineListFacade│ │PipelineBuildFacade│        │
│  │Service (80KB)    │ │Service (97KB)    │ │Service (137KB)   │         │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Engine Service 层 (biz-base)                     │
│  职责：核心业务逻辑、数据访问、事件分发                                     │
│  ┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐         │
│  │PipelineRepository│ │PipelineRuntime   │ │PipelineContainer │         │
│  │Service (110KB)   │ │Service (101KB)   │ │Service (50KB)    │         │
│  └──────────────────┘ └──────────────────┘ └──────────────────┘         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 二、定时触发系统

### 2.1 Quartz 定时调度架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Quartz 调度器                                    │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ PipelineQuartzService                                                ││
│  │   @Scheduled(initialDelay=20000, fixedDelay=3000000)                ││
│  │   reloadTimer() ─► 加载所有定时任务到 Quartz                          ││
│  └─────────────────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         PipelineQuartzJob                                │
│  execute(context) ─► PipelineJobBean.execute()                          │
│    ├─► 获取定时器配置 (pipelineTimerService.get())                       │
│    ├─► 检查项目路由标签                                                  │
│    ├─► Redis 分布式锁防重复触发                                          │
│    └─► 发送 PipelineTimerBuildEvent                                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         PipelineTimerBuildListener                       │
│  消费 PipelineTimerBuildEvent                                           │
│    ├─► 获取流水线模型                                                    │
│    ├─► 解析启动参数                                                      │
│    └─► 调用 pipelineBuildFacadeService.startPipeline()                  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心类说明

**PipelineQuartzService** - 定时调度服务
```kotlin
@Service
class PipelineQuartzService @Autowired constructor(
    private val pipelineTimerService: PipelineTimerService,
    private val schedulerManager: SchedulerManager
) {
    // 每 50 分钟重新加载定时任务
    @Scheduled(initialDelay = 20000, fixedDelay = 3000000)
    fun reloadTimer() {
        // 遍历所有定时器，添加到 Quartz
        pipelineTimerService.list(start, limit).forEach { timer ->
            timer.crontabExpressions.forEach { crontab ->
                addJob(projectId, pipelineId, crontab, taskId)
            }
        }
    }
    
    fun addJob(projectId: String, pipelineId: String, crontab: String, taskId: String) {
        val md5 = DigestUtils.md5Hex(crontab)
        val comboKey = "${pipelineId}_${md5}_${projectId}_$taskId"
        schedulerManager.addJob(
            key = comboKey,
            cronExpression = crontab,
            jobBeanClass = PipelineQuartzJob::class.java,
            projectId = projectId,
            pipelineId = pipelineId,
            taskId = taskId,
            md5 = md5
        )
    }
}
```

**PipelineJobBean** - Quartz Job 执行器
```kotlin
class PipelineJobBean(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineTimerService: PipelineTimerService,
    private val redisOperation: RedisOperation
) {
    fun execute(context: JobExecutionContext?) {
        // 1. 获取定时器配置
        val pipelineTimer = pipelineTimerService.get(projectId, pipelineId, taskId)
        
        // 2. Redis 锁防止重复触发
        val redisLock = PipelineTimerTriggerLock(redisOperation, pipelineLockKey, scheduledFireTime)
        if (redisLock.tryLock()) {
            // 3. 发送定时构建事件
            pipelineEventDispatcher.dispatch(
                PipelineTimerBuildEvent(
                    source = "timer_trigger",
                    projectId = pipelineTimer.projectId,
                    pipelineId = pipelineId,
                    userId = pipelineTimer.startUser,
                    channelCode = pipelineTimer.channelCode,
                    taskId = taskId,
                    startParam = pipelineTimer.startParam
                )
            )
        }
    }
}
```

## 三、拦截器链机制

### 3.1 拦截器架构

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PipelineInterceptorChain                         │
│  ┌─────────────────────────────────────────────────────────────────────┐│
│  │ filter(InterceptData) ─► 依次执行拦截器                              ││
│  │   如果任一拦截器返回 isNotOk()，则中断并返回错误                       ││
│  └─────────────────────────────────────────────────────────────────────┘│
│                                    │                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────────────┐  │
│  │ RunLock         │→ │ Queue           │→ │ TimerTriggerScmChange   │  │
│  │ Interceptor     │  │ Interceptor     │  │ Interceptor             │  │
│  └─────────────────┘  └─────────────────┘  └─────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
```

### 3.2 拦截器接口

```kotlin
interface PipelineInterceptor {
    fun execute(task: InterceptData): Response<BuildStatus>
}
```

### 3.3 拦截器链实现

```kotlin
class PipelineInterceptorChain(private val filters: List<PipelineInterceptor>) {
    fun filter(any: InterceptData): Response<BuildStatus> {
        var result = Response<BuildStatus>(OK)
        run lit@{
            filters.forEach {
                result = it.execute(any)
                if (result.isNotOk()) {
                    return@lit  // 中断链
                }
            }
        }
        return result
    }
}
```

### 3.4 拦截器配置

```kotlin
@Configuration
class PipelineInterceptorConfigure {
    @Bean
    fun pipelineInterceptorChain(
        runLockInterceptor: RunLockInterceptor,
        queueInterceptor: QueueInterceptor,
        timerTriggerScmChangeInterceptor: TimerTriggerScmChangeInterceptor
    ): PipelineInterceptorChain {
        return PipelineInterceptorChain(
            listOf(
                runLockInterceptor,           // 1. 运行锁检查
                queueInterceptor,             // 2. 队列/并发控制
                timerTriggerScmChangeInterceptor  // 3. 定时触发源码变更检查
            )
        )
    }
}
```

### 3.5 各拦截器职责

| 拦截器 | 职责 | 返回结果 |
|--------|------|----------|
| `RunLockInterceptor` | 检查流水线是否被锁定 | `LOCK` 时拒绝，`SINGLE/SINGLE_LOCK` 时排队 |
| `QueueInterceptor` | 并发控制、队列管理 | 队列满时拒绝，超出并发时取消旧构建 |
| `TimerTriggerScmChangeInterceptor` | 定时触发时检查源码是否变更 | 无变更时跳过构建 |

### 3.6 RunLockInterceptor 详解

```kotlin
@Component
class RunLockInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService
) : PipelineInterceptor {
    
    override fun execute(task: InterceptData): Response<BuildStatus> {
        val runLockType = task.runLockType
        
        return when {
            // 流水线被锁定，拒绝执行
            runLockType == PipelineRunLockType.LOCK -> 
                Response(ERROR_PIPELINE_LOCK, "流水线已锁定")
            
            // 串行模式，检查是否有正在运行的构建
            runLockType == PipelineRunLockType.SINGLE -> {
                val runningCount = pipelineRuntimeService.getRunningCount(projectId, pipelineId)
                if (runningCount >= 1) {
                    Response(BuildStatus.QUEUE)  // 排队
                } else {
                    Response(BuildStatus.RUNNING)  // 可执行
                }
            }
            
            // 并发组模式
            runLockType == PipelineRunLockType.GROUP_LOCK -> {
                // 检查同一并发组是否有正在运行的构建
                checkConcurrencyGroup(task)
            }
            
            else -> Response(BuildStatus.RUNNING)
        }
    }
}
```

### 3.7 QueueInterceptor 详解

```kotlin
@Component
class QueueInterceptor @Autowired constructor(
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) : PipelineInterceptor {
    
    override fun execute(task: InterceptData): Response<BuildStatus> {
        val buildSummary = pipelineRuntimeService.getBuildSummaryRecord(projectId, pipelineId)
        
        return when {
            // 队列已满
            buildSummary.queueCount >= task.maxQueueSize -> {
                if (task.cancelAllowed) {
                    // 取消最早排队的构建
                    outQueueCancelBySingle(projectId, pipelineId, task)
                    Response(BuildStatus.RUNNING)
                } else {
                    Response(ERROR_PIPELINE_QUEUE_FULL, "队列已满")
                }
            }
            
            // 运行数+排队数超过最大限制
            (buildSummary.runningCount + buildSummary.queueCount) >= maxLimit ->
                Response(ERROR_PIPELINE_QUEUE_FULL, "超过最大并发限制")
            
            else -> Response(BuildStatus.RUNNING)
        }
    }
    
    private fun outQueueCancelBySingle(projectId: String, pipelineId: String, task: InterceptData) {
        // 取消最早排队的构建
        val buildInfo = pipelineRuntimeExtService.popNextQueueBuildInfo(projectId, pipelineId)
        if (buildInfo != null) {
            pipelineEventDispatcher.dispatch(
                PipelineBuildCancelEvent(
                    source = "QueueInterceptor",
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildInfo.buildId,
                    status = BuildStatus.CANCELED
                )
            )
        }
    }
}
```

## 四、Facade Service 层详解 (biz-process)

### 2.1 目录结构

```
biz-process/src/main/kotlin/com/tencent/devops/process/service/
├── PipelineInfoFacadeService.kt      # 流水线信息管理门面 (80KB)
├── PipelineListFacadeService.kt      # 流水线列表查询门面 (97KB)
├── PipelineVersionFacadeService.kt   # 版本管理门面 (37KB)
├── ParamFacadeService.kt             # 参数管理门面 (18KB)
├── builds/
│   ├── PipelineBuildFacadeService.kt # 构建管理门面 (137KB) - 最大
│   ├── PipelineBuildRetryService.kt  # 重试服务 (25KB)
│   └── PipelinePauseBuildFacadeService.kt # 暂停服务 (11KB)
├── template/
│   ├── TemplateFacadeService.kt      # 模板管理门面 (120KB)
│   └── TemplateSettingService.kt     # 模板配置 (7KB)
├── view/
│   ├── PipelineViewService.kt        # 视图服务 (34KB)
│   └── PipelineViewGroupService.kt   # 视图分组 (44KB)
└── webhook/
    └── PipelineBuildWebhookService.kt # Webhook 服务 (33KB)
```

### 2.2 PipelineInfoFacadeService - 流水线信息管理

**文件**: `PipelineInfoFacadeService.kt` (80KB)

**职责**: 流水线 CRUD 操作的业务编排层

```kotlin
@Service
class PipelineInfoFacadeService @Autowired constructor(
    private val dslContext: DSLContext,
    private val templateDao: TemplateDao,
    private val pipelineSettingFacadeService: PipelineSettingFacadeService,
    private val pipelineRepositoryService: PipelineRepositoryService,  // Engine Service
    private val pipelineGroupService: PipelineGroupService,
    private val pipelineViewGroupService: PipelineViewGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val modelCheckPlugin: ModelCheckPlugin,
    private val client: Client,
    // ... 更多依赖
) {
    // 核心方法
}
```

**核心方法**:

| 方法 | 功能 | 调用链 |
|------|------|--------|
| `createPipeline()` | 创建流水线 | 权限校验 → 模型校验 → `PipelineRepositoryService.deployPipeline()` |
| `editPipeline()` | 编辑流水线 | 权限校验 → 版本检查 → `PipelineRepositoryService.deployPipeline()` |
| `copyPipeline()` | 复制流水线 | 获取模型 → 修改名称 → `createPipeline()` |
| `deletePipeline()` | 删除流水线 | 权限校验 → `PipelineRepositoryService.deletePipeline()` |
| `getPipeline()` | 获取流水线 | `PipelineRepositoryService.getModel()` |
| `exportPipeline()` | 导出流水线 | 获取模型 → JSON/YAML 转换 → 返回文件流 |
| `importPipeline()` | 导入流水线 | 解析文件 → 模型校验 → `createPipeline()` |

**创建流水线流程**:

```kotlin
fun createPipeline(
    userId: String,
    projectId: String,
    model: Model,
    channelCode: ChannelCode,
    checkPermission: Boolean = true
): String {
    // 1. 权限校验
    if (checkPermission) {
        pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
    }
    
    // 2. 项目流水线数量限制检查
    val pipelineCount = pipelineInfoDao.countByProject(dslContext, projectId)
    if (pipelineCount >= maxPipelinePerProject) {
        throw ErrorCodeException(ERROR_MAX_PIPELINE_COUNT_PER_PROJECT)
    }
    
    // 3. 模型校验（插件存在性、参数合法性等）
    modelCheckPlugin.checkModelIntegrity(model, projectId)
    
    // 4. 调用 Engine Service 保存
    val pipelineId = pipelineRepositoryService.deployPipeline(
        model = model,
        projectId = projectId,
        signPipelineId = null,  // 新建时为空
        userId = userId,
        channelCode = channelCode,
        create = true
    ).pipelineId
    
    // 5. 创建权限资源
    pipelinePermissionService.createResource(
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        pipelineName = model.name
    )
    
    // 6. 记录操作日志
    operationLogService.addOperationLog(
        userId = userId,
        projectId = projectId,
        pipelineId = pipelineId,
        operationType = OperationLogType.CREATE
    )
    
    return pipelineId
}
```

### 2.3 PipelineBuildFacadeService - 构建管理

**文件**: `builds/PipelineBuildFacadeService.kt` (137KB) - **最大的 Service 文件**

**职责**: 构建启动、停止、重试、查询等业务编排

```kotlin
@Service
class PipelineBuildFacadeService(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val pipelineInterceptorChain: PipelineInterceptorChain,
    private val pipelineBuildService: PipelineBuildService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val pipelineRuntimeService: PipelineRuntimeService,
    private val buildVariableService: BuildVariableService,
    private val pipelineTaskService: PipelineTaskService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineStageService: PipelineStageService,
    private val redisOperation: RedisOperation,
    // ... 更多依赖
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineBuildFacadeService::class.java)
    }
}
```

**核心方法**:

| 方法 | 功能 | 说明 |
|------|------|------|
| `buildManualStartup()` | 手动启动构建 | 入口方法，处理参数后调用 `startPipeline()` |
| `startPipeline()` | 启动流水线 | 核心启动逻辑，调用拦截器链 |
| `buildManualShutdown()` | 手动停止构建 | 发送取消事件 |
| `buildManualReview()` | 人工审核 | 处理审核通过/拒绝 |
| `retry()` | 重试构建 | 支持整体重试或指定任务重试 |
| `getBuildDetail()` | 获取构建详情 | 返回 `ModelDetail` |
| `getBuildStatus()` | 获取构建状态 | 返回 `BuildHistoryWithVars` |
| `getHistoryBuild()` | 获取构建历史 | 分页查询 |
| `getBuildVars()` | 获取构建变量 | 返回变量键值对 |

**启动构建流程**:

```kotlin
fun startPipeline(
    userId: String,
    readyToBuildPipelineInfo: PipelineInfo,
    startType: StartType,
    startParams: MutableMap<String, String>,
    channelCode: ChannelCode,
    isMobile: Boolean,
    model: Model,
    frequencyLimit: Boolean = true
): BuildId {
    val projectId = readyToBuildPipelineInfo.projectId
    val pipelineId = readyToBuildPipelineInfo.pipelineId
    
    // 1. 构建拦截器链校验（频率限制、并发控制等）
    val interceptResult = pipelineInterceptorChain.filter(
        InterceptData(
            pipelineInfo = readyToBuildPipelineInfo,
            model = model,
            startType = startType,
            frequencyLimit = frequencyLimit
        )
    )
    if (interceptResult.isNotOk()) {
        throw ErrorCodeException(errorCode = interceptResult.status.toString())
    }
    
    // 2. 调用 Runtime Service 创建构建记录
    val buildId = pipelineRuntimeService.startBuild(
        pipelineInfo = readyToBuildPipelineInfo,
        fullModel = model,
        startParams = startParams,
        startType = startType,
        buildNo = buildNo
    )
    
    // 3. 发送构建启动事件到 MQ
    pipelineEventDispatcher.dispatch(
        PipelineBuildStartEvent(
            source = "startPipeline",
            projectId = projectId,
            pipelineId = pipelineId,
            userId = userId,
            buildId = buildId.id,
            taskId = "",
            status = BuildStatus.QUEUE,
            actionType = ActionType.START
        )
    )
    
    return buildId
}
```

### 2.4 PipelineListFacadeService - 列表查询

**文件**: `PipelineListFacadeService.kt` (97KB)

**职责**: 流水线列表查询、搜索、过滤

**核心方法**:

| 方法 | 功能 |
|------|------|
| `listPipelineInfo()` | 分页查询流水线列表 |
| `searchByPipelineName()` | 按名称搜索 |
| `listPipelinesByViewId()` | 按视图查询 |
| `listDeletedPipeline()` | 查询已删除流水线 |
| `getPipelineStatus()` | 批量获取状态 |

### 2.5 TemplateFacadeService - 模板管理

**文件**: `template/TemplateFacadeService.kt` (120KB)

**职责**: 流水线模板的创建、实例化、更新

**核心方法**:

| 方法 | 功能 |
|------|------|
| `createTemplate()` | 创建模板 |
| `updateTemplate()` | 更新模板 |
| `deleteTemplate()` | 删除模板 |
| `createTemplateInstances()` | 批量实例化模板 |
| `updateTemplateInstances()` | 批量更新实例 |

## 三、Engine Service 层详解 (biz-base)

### 3.1 目录结构

```
biz-base/src/main/kotlin/com/tencent/devops/process/engine/service/
├── PipelineRepositoryService.kt      # 流水线存储服务 (110KB) - 核心
├── PipelineRuntimeService.kt         # 运行时服务 (101KB) - 核心
├── PipelineContainerService.kt       # 容器管理 (50KB)
├── PipelineStageService.kt           # 阶段管理 (40KB)
├── PipelineTaskService.kt            # 任务管理 (32KB)
├── PipelineBuildDetailService.kt     # 构建详情 (16KB)
├── PipelineSettingService.kt         # 配置服务 (6KB)
├── detail/
│   ├── BaseBuildDetailService.kt     # 详情基类 (12KB)
│   ├── StageBuildDetailService.kt    # 阶段详情 (11KB)
│   ├── ContainerBuildDetailService.kt # 容器详情 (12KB)
│   └── TaskBuildDetailService.kt     # 任务详情 (25KB)
├── record/
│   ├── PipelineBuildRecordService.kt # 构建记录 (36KB)
│   ├── StageBuildRecordService.kt    # 阶段记录 (21KB)
│   ├── ContainerBuildRecordService.kt # 容器记录 (22KB)
│   └── TaskBuildRecordService.kt     # 任务记录 (30KB)
└── vmbuild/
    └── EngineVMBuildService.kt       # 构建机服务 (60KB)
```

### 3.2 PipelineRepositoryService - 流水线存储

**文件**: `PipelineRepositoryService.kt` (110KB) - **最核心的 Engine Service**

**职责**: 流水线模型的存储、版本管理、部署

```kotlin
@Service
class PipelineRepositoryService constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val modelContainerIdGenerator: ModelContainerIdGenerator,
    private val pipelineIdGenerator: PipelineIdGenerator,
    private val modelTaskIdGenerator: ModelTaskIdGenerator,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineResourceDao: PipelineResourceDao,
    private val pipelineModelTaskDao: PipelineModelTaskDao,
    private val pipelineSettingDao: PipelineSettingDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    // ... 更多 DAO
) {
    companion object {
        private const val MAX_LEN_FOR_NAME = 255
        private val logger = LoggerFactory.getLogger(PipelineRepositoryService::class.java)
    }
}
```

**核心方法**:

| 方法 | 功能 | 操作的表 |
|------|------|----------|
| `deployPipeline()` | 部署流水线（创建/更新） | T_PIPELINE_INFO, T_PIPELINE_RESOURCE, T_PIPELINE_MODEL_TASK |
| `getPipelineInfo()` | 获取流水线信息 | T_PIPELINE_INFO |
| `getModel()` | 获取流水线模型 | T_PIPELINE_RESOURCE |
| `deletePipeline()` | 删除流水线 | 软删除，更新 DELETE 标记 |
| `restorePipeline()` | 恢复流水线 | 恢复 DELETE 标记 |
| `listPipelineIdByProject()` | 按项目列出流水线 | T_PIPELINE_INFO |
| `saveSetting()` | 保存流水线配置 | T_PIPELINE_SETTING |

**deployPipeline 核心逻辑**:

```kotlin
fun deployPipeline(
    model: Model,
    projectId: String,
    signPipelineId: String?,
    userId: String,
    channelCode: ChannelCode,
    create: Boolean,
    updateLastModifyUser: Boolean = true
): DeployPipelineResult {
    // 1. 生成/获取 pipelineId
    val pipelineId = signPipelineId ?: pipelineIdGenerator.getNextId()
    
    // 2. 为模型中的元素生成 ID
    model.stages.forEachIndexed { stageIndex, stage ->
        stage.id = stage.id ?: modelContainerIdGenerator.getNextId()
        stage.containers.forEachIndexed { containerIndex, container ->
            container.id = container.id ?: modelContainerIdGenerator.getNextId()
            container.elements.forEach { element ->
                element.id = element.id ?: modelTaskIdGenerator.getNextId()
            }
        }
    }
    
    // 3. 数据库事务操作
    val version = dslContext.transactionResult { configuration ->
        val context = DSL.using(configuration)
        
        if (create) {
            // 创建流水线信息
            pipelineInfoDao.create(
                dslContext = context,
                pipelineId = pipelineId,
                projectId = projectId,
                version = 1,
                pipelineName = model.name,
                userId = userId,
                channelCode = channelCode
            )
            // 创建构建摘要记录
            pipelineBuildSummaryDao.create(context, projectId, pipelineId)
        } else {
            // 更新版本号
            pipelineInfoDao.update(
                dslContext = context,
                projectId = projectId,
                pipelineId = pipelineId,
                userId = userId,
                version = newVersion
            )
        }
        
        // 保存流水线模型（JSON）
        pipelineResourceDao.create(
            dslContext = context,
            projectId = projectId,
            pipelineId = pipelineId,
            version = version,
            model = JsonUtil.toJson(model)
        )
        
        // 保存模型任务索引（用于搜索）
        saveModelTasks(context, model, projectId, pipelineId, version)
        
        version
    }
    
    // 4. 发送流水线创建/更新事件
    if (create) {
        pipelineEventDispatcher.dispatch(
            PipelineCreateEvent(projectId, pipelineId, userId)
        )
    } else {
        pipelineEventDispatcher.dispatch(
            PipelineUpdateEvent(projectId, pipelineId, userId)
        )
    }
    
    return DeployPipelineResult(pipelineId, version)
}
```

### 3.3 PipelineRuntimeService - 运行时服务

**文件**: `PipelineRuntimeService.kt` (101KB) - **构建运行时核心**

**职责**: 构建的创建、状态管理、查询

```kotlin
@Service
class PipelineRuntimeService @Autowired constructor(
    private val pipelineEventDispatcher: PipelineEventDispatcher,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val pipelineBuildDao: PipelineBuildDao,
    private val pipelineBuildSummaryDao: PipelineBuildSummaryDao,
    private val pipelineStageService: PipelineStageService,
    private val pipelineContainerService: PipelineContainerService,
    private val pipelineTaskService: PipelineTaskService,
    private val buildVariableService: BuildVariableService,
    // ... 更多依赖
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PipelineRuntimeService::class.java)
    }
}
```

**核心方法**:

| 方法 | 功能 | 说明 |
|------|------|------|
| `startBuild()` | 启动构建 | 创建构建记录、Stage/Container/Task 记录 |
| `getBuildInfo()` | 获取构建信息 | 返回 `BuildInfo` |
| `updateBuildStatus()` | 更新构建状态 | 状态机转换 |
| `getBuildHistoryByIds()` | 批量获取历史 | 返回 `List<BuildHistory>` |
| `cancelBuild()` | 取消构建 | 发送取消事件 |
| `finishBuild()` | 结束构建 | 更新最终状态、发送通知 |

**startBuild 核心逻辑**:

```kotlin
fun startBuild(
    pipelineInfo: PipelineInfo,
    fullModel: Model,
    startParams: Map<String, String>,
    startType: StartType,
    buildNo: Int?
): BuildId {
    val projectId = pipelineInfo.projectId
    val pipelineId = pipelineInfo.pipelineId
    
    // 1. 生成构建 ID
    val buildId = "b-${UUIDUtil.generate()}"
    
    // 2. 计算构建号
    val buildNum = pipelineBuildSummaryDao.updateBuildNum(
        dslContext, projectId, pipelineId
    )
    
    // 3. 创建构建记录
    pipelineBuildDao.create(
        dslContext = dslContext,
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        version = pipelineInfo.version,
        buildNum = buildNum,
        trigger = startType.name,
        status = BuildStatus.QUEUE,
        startUser = startParams[PIPELINE_START_USER_ID] ?: "",
        triggerUser = startParams[PIPELINE_START_USER_NAME] ?: ""
    )
    
    // 4. 创建 Stage 记录
    fullModel.stages.forEachIndexed { index, stage ->
        pipelineStageService.createStage(
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            stage = stage,
            seq = index
        )
        
        // 5. 创建 Container 记录
        stage.containers.forEachIndexed { containerIndex, container ->
            pipelineContainerService.createContainer(
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                stageId = stage.id!!,
                container = container,
                seq = containerIndex
            )
            
            // 6. 创建 Task 记录
            container.elements.forEachIndexed { taskIndex, element ->
                pipelineTaskService.createTask(
                    projectId = projectId,
                    pipelineId = pipelineId,
                    buildId = buildId,
                    stageId = stage.id!!,
                    containerId = container.id!!,
                    element = element,
                    seq = taskIndex
                )
            }
        }
    }
    
    // 7. 保存构建变量
    buildVariableService.batchSetVariable(
        projectId = projectId,
        pipelineId = pipelineId,
        buildId = buildId,
        variables = startParams
    )
    
    return BuildId(buildId)
}
```

### 3.4 PipelineContainerService - 容器管理

**文件**: `PipelineContainerService.kt` (50KB)

**职责**: Job（容器）的创建、状态管理

**核心方法**:

| 方法 | 功能 |
|------|------|
| `createContainer()` | 创建容器记录 |
| `getContainer()` | 获取容器信息 |
| `updateContainerStatus()` | 更新容器状态 |
| `listContainers()` | 列出阶段下的容器 |
| `listContainerBuildTasks()` | 列出容器下的任务 |

### 3.5 PipelineStageService - 阶段管理

**文件**: `PipelineStageService.kt` (40KB)

**职责**: Stage 的创建、状态管理

**核心方法**:

| 方法 | 功能 |
|------|------|
| `createStage()` | 创建阶段记录 |
| `getStage()` | 获取阶段信息 |
| `updateStageStatus()` | 更新阶段状态 |
| `listStages()` | 列出构建的所有阶段 |
| `getPrevStage()` | 获取前一个阶段 |
| `getNextStage()` | 获取下一个阶段 |

### 3.6 PipelineTaskService - 任务管理

**文件**: `PipelineTaskService.kt` (32KB)

**职责**: Task（插件）的创建、状态管理

**核心方法**:

| 方法 | 功能 |
|------|------|
| `createTask()` | 创建任务记录 |
| `getBuildTask()` | 获取任务信息 |
| `updateTaskStatus()` | 更新任务状态 |
| `listContainerBuildTasks()` | 列出容器下的任务 |
| `getNextPendingTask()` | 获取下一个待执行任务 |

### 3.7 EngineVMBuildService - 构建机服务

**文件**: `vmbuild/EngineVMBuildService.kt` (60KB)

**职责**: 与构建机（Agent）的交互

**核心方法**:

| 方法 | 功能 |
|------|------|
| `setStartUpVMStatus()` | 设置构建机启动状态 |
| `claimBuildTask()` | 领取待执行任务 |
| `completeClaimBuildTask()` | 完成任务回调 |
| `heartbeat()` | 心跳上报 |
| `endBuild()` | 结束构建 |

## 四、Service 层调用关系

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         ResourceImpl (API 实现)                          │
│  UserPipelineResourceImpl | UserBuildResourceImpl | ...                 │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Facade Service                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ PipelineInfoFacadeService                                        │    │
│  │   ├─► pipelinePermissionService.checkPermission()               │    │
│  │   ├─► modelCheckPlugin.checkModelIntegrity()                    │    │
│  │   └─► pipelineRepositoryService.deployPipeline()                │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ PipelineBuildFacadeService                                       │    │
│  │   ├─► pipelineInterceptorChain.filter()                         │    │
│  │   ├─► pipelineRuntimeService.startBuild()                       │    │
│  │   └─► pipelineEventDispatcher.dispatch(StartEvent)              │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         Engine Service                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ PipelineRepositoryService                                        │    │
│  │   ├─► pipelineInfoDao.create/update()                           │    │
│  │   ├─► pipelineResourceDao.create()                              │    │
│  │   └─► pipelineEventDispatcher.dispatch(CreateEvent)             │    │
│  └─────────────────────────────────────────────────────────────────┘    │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │ PipelineRuntimeService                                           │    │
│  │   ├─► pipelineBuildDao.create()                                 │    │
│  │   ├─► pipelineStageService.createStage()                        │    │
│  │   ├─► pipelineContainerService.createContainer()                │    │
│  │   └─► pipelineTaskService.createTask()                          │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DAO 层                                           │
│  PipelineInfoDao | PipelineBuildDao | PipelineResourceDao | ...         │
└─────────────────────────────────────────────────────────────────────────┘
```

## 五、开发规范

### 5.1 Facade Service 规范

```kotlin
@Service
class XxxFacadeService @Autowired constructor(
    private val permissionService: PipelinePermissionService,  // 权限服务
    private val engineService: XxxEngineService,               // Engine Service
    private val client: Client                                 // 微服务客户端
) {
    // 1. 权限校验
    // 2. 参数校验/转换
    // 3. 调用 Engine Service
    // 4. 记录操作日志
}
```

### 5.2 Engine Service 规范

```kotlin
@Service
class XxxEngineService @Autowired constructor(
    private val dslContext: DSLContext,
    private val xxxDao: XxxDao,
    private val pipelineEventDispatcher: PipelineEventDispatcher
) {
    // 1. 数据库事务操作
    // 2. 事件分发
    // 3. 不做权限校验（由 Facade 层处理）
}
```

### 5.3 新增 Service 检查清单

- [ ] Facade Service 放在 `biz-process/service/`
- [ ] Engine Service 放在 `biz-base/engine/service/`
- [ ] Facade Service 负责权限校验
- [ ] Engine Service 负责数据访问和事件分发
- [ ] 使用构造器注入依赖
- [ ] 添加 `@Service` 注解

---

**版本**: 1.1.0 | **更新日期**: 2025-12-10 | **补充**: 添加定时触发系统和拦截器链机制

---
name: design-patterns
description: BK-CI 项目设计模式实践指南，涵盖工厂模式、策略模式、观察者模式、装饰器模式、模板方法等在项目中的实际应用。当用户学习设计模式、重构代码、设计可扩展架构或理解项目设计时使用。
---

# Skill 27: BK-CI 项目设计模式实践指南

## 概述

本指南总结了 BK-CI 项目中广泛使用的设计模式及其实际应用场景，帮助开发者理解项目架构并遵循统一的设计模式规范。

---

## 1. 工厂模式（Factory Pattern）

### 1.1 简单工厂（Simple Factory）

**应用场景**：创建特定类型的对象，通过类型参数决定实例化哪个类。

**实际案例**：

#### TaskFactory - 任务工厂

**位置**：`worker-common/src/main/kotlin/com/tencent/devops/worker/common/task/TaskFactory.kt`

**实现方式**：
```kotlin
object TaskFactory {
    private val taskMap = ConcurrentHashMap<String, KClass<out ITask>>()

    fun init() {
        // 注册内置任务
        register(LinuxScriptElement.classType, LinuxScriptTask::class)
        register(WindowsScriptElement.classType, WindowsScriptTask::class)
        register(MarketBuildAtomElement.classType, MarketAtomTask::class)
        
        // 通过反射扫描并注册插件任务
        val reflections = Reflections("com.tencent.devops.plugin.worker.task")
        val taskClasses = reflections.getSubTypesOf(ITask::class.java)
        taskClasses?.forEach { taskClazz ->
            val taskClassType = taskClazz.getAnnotation(TaskClassType::class.java)
            taskClassType?.classTypes?.forEach { classType ->
                register(classType, taskClazz.kotlin)
            }
        }
    }

    fun create(type: String): ITask {
        val clazz = taskMap[type] ?: return EmptyTask(type)
        return clazz.primaryConstructor?.call() ?: EmptyTask(type)
    }
}
```

**使用方式**：
```kotlin
// 根据 element 类型创建对应的任务实例
val task = TaskFactory.create(element.getClassType())
task.run(buildTask, param, elementId)
```

**特点**：
- 使用 Kotlin `object` 实现单例
- 支持反射扫描自动注册
- 支持优先级覆盖（`priority` 字段）
- 使用 `ConcurrentHashMap` 保证线程安全

---

#### ScmFactory - 代码库工厂

**位置**：`common-scm/src/main/kotlin/com/tencent/devops/scm/ScmFactory.kt`

**实现方式**：
```kotlin
object ScmFactory {
    private val gitApi = GitApi()
    private val svnApi = SVNApi()

    fun getScm(
        projectName: String,
        url: String,
        type: ScmType,  // CODE_SVN, CODE_GIT, CODE_TGIT, CODE_GITLAB, CODE_P4
        // ... 其他参数
    ): IScm {
        return when (type) {
            ScmType.CODE_SVN -> CodeSvnScmImpl(...)
            ScmType.CODE_GIT -> CodeGitScmImpl(...)
            ScmType.CODE_TGIT -> CodeTGitScmImpl(...)
            ScmType.CODE_GITLAB -> CodeGitlabScmImpl(...)
            ScmType.CODE_P4 -> CodeP4ScmImpl(...)
            else -> throw TaskExecuteException(
                errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND,
                errorMsg = "Unknown repo($type)"
            )
        }
    }
}
```

**使用场景**：根据代码库类型（SVN、Git、GitLab、P4）创建对应的 SCM 实现。

---

#### 其他工厂实例

| 工厂类 | 位置 | 用途 |
|--------|------|------|
| `ApiFactory` | `worker-common/api/ApiFactory.kt` | 创建 Worker 端 API 客户端 |
| `CommandFactory` | `worker-common/task/script/CommandFactory.kt` | 创建脚本命令执行器 |
| `AtomRunConditionFactory` | `worker-common/task/market/AtomRunConditionFactory.kt` | 创建插件运行条件处理器 |
| `PathFilterFactory` | `common-webhook/service/code/filter/PathFilterFactory.kt` | 创建 Webhook 路径过滤器 |
| `DigestFactory` | `common-api/factory/DigestFactory.kt` | 创建摘要算法实现 |

---

### 1.2 抽象工厂（Abstract Factory）

**应用场景**：创建一系列相关对象的工厂。

**实际案例**：

#### BkApiHandleFactory - API 处理器工厂

**位置**：`common-web/src/main/kotlin/com/tencent/devops/common/web/factory/BkApiHandleFactory.kt`

**实现方式**：
```kotlin
object BkApiHandleFactory {
    private val handleMap = ConcurrentHashMap<String, BkApiHandleInterface>()

    fun registerHandle(handle: BkApiHandleInterface) {
        handleMap[handle.code()] = handle
    }

    fun getHandle(code: String): BkApiHandleInterface? {
        return handleMap[code]
    }
}

// 使用示例：根据 API 类型获取对应的处理器
interface BkApiHandleInterface {
    fun code(): String  // 返回 API 类型标识
    fun handle(request: BkApiRequest): BkApiResponse
}
```

---

## 2. 单例模式（Singleton Pattern）

### 2.1 Kotlin Object 单例

**应用场景**：全局唯一实例，提供统一访问点。

**实现方式**：使用 Kotlin `object` 关键字。

**实际案例**：

```kotlin
// 工厂单例
object TaskFactory { ... }
object ScmFactory { ... }
object ApiFactory { ... }

// 工具类单例
object JsonUtil { ... }
object DateTimeUtil { ... }
```

**特点**：
- 线程安全（由 JVM 保证）
- 延迟初始化（首次访问时初始化）
- 无法被继承

---

### 2.2 Spring 单例

**应用场景**：业务服务类，通过 Spring 容器管理生命周期。

**实现方式**：
```kotlin
@Service  // 默认单例
class PipelineService { ... }

@Component  // 默认单例
class UserArchivedPipelinePermissionCheckStrategy { ... }
```

**特点**：
- Spring 容器管理
- 支持依赖注入
- 支持 AOP 增强

---

## 3. 建造者模式（Builder Pattern）

### 3.1 Kotlin Data Class + 命名参数

**应用场景**：构建复杂对象，提供清晰的构造参数。

**实现方式**：
```kotlin
data class PipelineTriggerEventBuilder(
    var projectId: String? = null,
    var pipelineId: String? = null,
    var userId: String? = null,
    var triggerType: String? = null,
    var triggerUser: String? = null,
    var eventSource: String? = null,
    // ... 更多字段
) {
    fun build(): PipelineTriggerEvent {
        return PipelineTriggerEvent(
            projectId = projectId ?: throw IllegalArgumentException("projectId is required"),
            pipelineId = pipelineId ?: throw IllegalArgumentException("pipelineId is required"),
            // ... 其他字段
        )
    }
}
```

**使用方式**：
```kotlin
val event = PipelineTriggerEventBuilder()
    .apply {
        projectId = "demo"
        pipelineId = "p-12345"
        userId = "admin"
        triggerType = "MANUAL"
    }
    .build()
```

---

### 3.2 Fluent Builder（链式调用）

**实际案例**：

```kotlin
class Ansi(private var builder: StringBuilder) {
    fun bold(): Ansi {
        builder.append("\u001B[1m")
        return this
    }

    fun fgRed(): Ansi {
        builder.append("\u001B[31m")
        return this
    }

    fun reset(): Ansi {
        builder.append("\u001B[0m")
        return this
    }

    fun toString(): String = builder.toString()
}

// 使用
val text = Ansi(StringBuilder())
    .bold()
    .fgRed()
    .append("Error: ")
    .reset()
    .append("Build failed")
    .toString()
```

---

## 4. 策略模式（Strategy Pattern）

### 4.1 接口 + 实现类

**应用场景**：根据运行时条件选择不同的算法实现。

**实际案例**：

#### 流水线权限检查策略

**位置**：`process/biz-process/src/main/kotlin/com/tencent/devops/process/strategy/`

**接口定义**：
```kotlin
interface IUserPipelinePermissionCheckStrategy {
    fun checkUserPipelinePermission(
        userId: String,
        projectId: String,
        pipelineId: String,
        permission: AuthPermission,
        message: String? = null
    )
}
```

**实现类**：
```kotlin
@Component
class UserNormalPipelinePermissionCheckStrategy : IUserPipelinePermissionCheckStrategy {
    override fun checkUserPipelinePermission(...) {
        // 正常流水线的权限检查逻辑
    }
}

@Component
class UserArchivedPipelinePermissionCheckStrategy : IUserPipelinePermissionCheckStrategy {
    override fun checkUserPipelinePermission(...) {
        // 归档流水线的权限检查逻辑
    }
}
```

**策略工厂**：
```kotlin
object UserPipelinePermissionCheckStrategyFactory {
    fun getStrategy(archived: Boolean): IUserPipelinePermissionCheckStrategy {
        return if (archived) {
            SpringContextUtil.getBean(UserArchivedPipelinePermissionCheckStrategy::class.java)
        } else {
            SpringContextUtil.getBean(UserNormalPipelinePermissionCheckStrategy::class.java)
        }
    }
}
```

**使用方式**：
```kotlin
val strategy = UserPipelinePermissionCheckStrategyFactory.getStrategy(pipeline.archived)
strategy.checkUserPipelinePermission(userId, projectId, pipelineId, AuthPermission.VIEW)
```

---

### 4.2 数据迁移策略

**位置**：`misc/biz-misc/src/main/kotlin/com/tencent/devops/misc/strategy/`

**接口定义**：
```kotlin
interface MigrationStrategy {
    fun migrate(projectId: String, pipelineId: String): Boolean
}
```

**实现类示例**：
- `PipelineInfoMigrationStrategy` - 流水线基本信息迁移
- `PipelineSettingMigrationStrategy` - 流水线设置迁移
- `TemplatePipelineMigrationStrategy` - 模板流水线迁移
- `PipelineYamlInfoMigrationStrategy` - YAML 流水线迁移
- 共计 20+ 个迁移策略实现类

**特点**：
- 每个策略负责特定数据的迁移
- 通过 Spring 容器管理
- 支持组合多个策略执行

---

## 5. 责任链模式（Chain of Responsibility）

### 5.1 Handler + HandlerChain

**应用场景**：将请求沿着处理器链传递，直到某个处理器处理它。

**实际案例**：

#### 研发商店创建处理链

**位置**：`store/biz-store/src/main/kotlin/com/tencent/devops/store/common/handler/`

**接口定义**：
```kotlin
interface Handler<T : HandlerRequest> {
    /**
     * 能否满足运行条件
     */
    fun canExecute(handlerRequest: T): Boolean

    /**
     * 核心处理逻辑
     */
    fun execute(handlerRequest: T)

    /**
     * 执行总入口
     */
    fun doExecute(handlerRequest: T, chain: HandlerChain<T>) {
        if (canExecute(handlerRequest)) {
            execute(handlerRequest)
        }
        chain.handleRequest(handlerRequest)  // 传递给下一个处理器
    }
}

interface HandlerChain<T : HandlerRequest> {
    fun nextHandler(handlerRequest: T): Handler<T>?
    
    fun handleRequest(handlerRequest: T) {
        val handler = nextHandler(handlerRequest)
        handler?.doExecute(handlerRequest, this)
    }
}
```

**处理器链实现**：
```kotlin
class StoreCreateHandlerChain(
    private val handlerList: MutableList<Handler<StoreCreateRequest>>
) : HandlerChain<StoreCreateRequest> {
    
    override fun nextHandler(handlerRequest: StoreCreateRequest): Handler<StoreCreateRequest>? {
        return handlerList.removeFirstOrNull()
    }
}
```

**具体处理器**：
```kotlin
class StoreCreateParamCheckHandler : Handler<StoreCreateRequest> {
    override fun canExecute(handlerRequest: StoreCreateRequest): Boolean = true
    
    override fun execute(handlerRequest: StoreCreateRequest) {
        // 参数校验逻辑
        if (handlerRequest.atomCode.isBlank()) {
            throw InvalidParamException("Atom code is blank")
        }
    }
}

class StoreCreatePreBusHandler : Handler<StoreCreateRequest> {
    override fun canExecute(handlerRequest: StoreCreateRequest): Boolean = true
    
    override fun execute(handlerRequest: StoreCreateRequest) {
        // 前置业务逻辑
    }
}

class StoreCreateDataPersistHandler : Handler<StoreCreateRequest> {
    override fun canExecute(handlerRequest: StoreCreateRequest): Boolean = true
    
    override fun execute(handlerRequest: StoreCreateRequest) {
        // 数据持久化
    }
}
```

**使用方式**：
```kotlin
val handlerList = mutableListOf(
    StoreCreateParamCheckHandler(),
    StoreCreatePreBusHandler(),
    StoreCreateDataPersistHandler(),
    StoreCreatePostBusHandler()
)
val chain = StoreCreateHandlerChain(handlerList)
chain.handleRequest(storeCreateRequest)
```

---

### 5.2 其他责任链实例

| 处理链 | 用途 |
|--------|------|
| `StoreCreateHandlerChain` | 研发商店组件创建 |
| `StoreUpdateHandlerChain` | 研发商店组件更新 |
| `StoreDeleteHandlerChain` | 研发商店组件删除 |
| `PipelineInterceptorChain` | 流水线启动拦截器链 |
| `WebhookFilterChain` | Webhook 过滤器链 |

---

## 6. 装饰器模式（Decorator Pattern）

### 6.1 异常装饰器

**位置**：`worker-common/src/main/kotlin/com/tencent/devops/worker/common/exception/TaskExecuteExceptionDecorator.kt`

**实现方式**：
```kotlin
interface ExceptionDecorator<T : Throwable> {
    fun decorate(exception: T): TaskExecuteException
}

class DefaultExceptionBase : ExceptionDecorator<Throwable> {
    override fun decorate(exception: Throwable): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = exception.message ?: "Unknown error",
            errorType = ErrorType.SYSTEM,
            errorCode = ErrorCode.SYSTEM_WORKER_INITIALIZATION_ERROR
        )
    }
}

class FileNotFoundExceptionD : ExceptionDecorator<FileNotFoundException> {
    override fun decorate(exception: FileNotFoundException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "File not found: ${exception.message}",
            errorType = ErrorType.USER,
            errorCode = ErrorCode.USER_RESOURCE_NOT_FOUND
        )
    }
}

class RemoteServiceExceptionD : ExceptionDecorator<RemoteServiceException> {
    override fun decorate(exception: RemoteServiceException): TaskExecuteException {
        return TaskExecuteException(
            errorMsg = "Remote service error: ${exception.errorMessage}",
            errorType = ErrorType.THIRD_PARTY,
            errorCode = exception.errorCode
        )
    }
}
```

**使用方式**：
```kotlin
object TaskExecuteExceptionDecorator {
    private val factory = mapOf(
        IllegalStateException::class to IllegalStateExceptionD(),
        FileNotFoundException::class to FileNotFoundExceptionD(),
        RemoteServiceException::class to RemoteServiceExceptionD(),
        IOException::class to IOExceptionD()
    )

    fun decorate(exception: Throwable): TaskExecuteException {
        val decorator = factory[exception::class] ?: DefaultExceptionBase()
        return decorator.decorate(exception)
    }
}
```

**作用**：将各种异常统一装饰成 `TaskExecuteException`，便于统一处理和日志记录。

---

### 6.2 权限服务装饰器

**位置**：`auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/provider/rbac/service/DelegatingPermissionServiceDecorator.kt`

**实现方式**：
```kotlin
class DelegatingPermissionServiceDecorator(
    private val delegate: PermissionService,
    private val extraCheckers: List<PermissionChecker>
) : PermissionService {
    
    override fun checkPermission(userId: String, resourceType: String, action: String): Boolean {
        // 先执行委托对象的权限检查
        if (!delegate.checkPermission(userId, resourceType, action)) {
            return false
        }
        
        // 再执行额外的检查器
        return extraCheckers.all { it.check(userId, resourceType, action) }
    }
}
```

**作用**：在原有权限服务基础上增加额外的权限检查逻辑，而不修改原有服务代码。

---

## 7. 观察者模式（Observer Pattern）

### 7.1 Spring Event 实现

**应用场景**：事件驱动架构，实现模块间解耦。

**实际案例**：

#### 流水线事件监听

**事件定义**：
```kotlin
data class ProjectBroadCastEvent(
    val projectId: String,
    val eventType: EventType,
    val userId: String
) : ApplicationEvent(projectId)
```

**事件监听器接口**：
```kotlin
interface ProjectEventListener : EventListener<ProjectBroadCastEvent> {
    /**
     * 处理项目广播事件
     */
    override fun execute(event: ProjectBroadCastEvent)
}
```

**具体监听器实现**：
```kotlin
@Component
class SampleProjectEventListener : ProjectEventListener {
    override fun execute(event: ProjectBroadCastEvent) {
        logger.info("Received project event: ${event.eventType} for ${event.projectId}")
        when (event.eventType) {
            EventType.CREATE -> handleProjectCreate(event)
            EventType.UPDATE -> handleProjectUpdate(event)
            EventType.DELETE -> handleProjectDelete(event)
        }
    }
}
```

**发布事件**：
```kotlin
@Service
class ProjectService(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun createProject(projectId: String, userId: String) {
        // ... 创建项目逻辑
        
        // 发布事件
        applicationEventPublisher.publishEvent(
            ProjectBroadCastEvent(
                projectId = projectId,
                eventType = EventType.CREATE,
                userId = userId
            )
        )
    }
}
```

---

### 7.2 其他监听器实例

| 监听器 | 用途 | 位置 |
|--------|------|------|
| `PipelineBuildQualityListener` | 流水线构建质量检查 | `quality/biz-quality/` |
| `WebhookEventListener` | Webhook 事件处理 | `process/biz-process/webhook/` |
| `PipelineTimerBuildListener` | 定时触发构建 | `process/biz-process/plugin/trigger/timer/` |
| `PipelineBuildNotifyListener` | 构建通知 | `process/biz-process/notify/` |
| `PipelineWebSocketListener` | WebSocket 消息推送 | `process/biz-process/websocket/` |

---

## 8. 模板方法模式（Template Method Pattern）

### 8.1 抽象类 + 钩子方法

**应用场景**：定义算法框架，子类实现具体步骤。

**实际案例**：

#### 流水线版本创建后置处理器

**位置**：`process/biz-process/src/main/kotlin/com/tencent/devops/process/service/pipeline/version/processor/`

**接口定义**：
```kotlin
interface PipelineVersionCreatePostProcessor {
    /**
     * 版本创建前的后置处理
     */
    fun postProcessBeforeVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) {
        // 默认空实现
    }

    /**
     * 版本创建后的后置处理
     */
    fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) {
        // 默认空实现
    }
}
```

**具体实现**：
```kotlin
@Service
class PipelineOperateLogVersionPostProcessor : PipelineVersionCreatePostProcessor {
    
    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) {
        // 记录操作日志
        pipelineOperateLogService.save(
            projectId = context.projectId,
            pipelineId = context.pipelineId,
            versionId = context.versionId,
            operateType = "CREATE",
            userId = context.userId
        )
    }
}

@Service
class PipelineEventVersionPostProcessor : PipelineVersionCreatePostProcessor {
    
    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) {
        // 发送事件
        applicationEventPublisher.publishEvent(
            PipelineVersionCreateEvent(
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                versionId = context.versionId
            )
        )
    }
}
```

**处理器编排**：
```kotlin
@Service
class PipelineVersionCreateService(
    private val postProcessors: List<PipelineVersionCreatePostProcessor>
) {
    fun createVersion(context: PipelineVersionCreateContext, model: Model, setting: PipelineSetting) {
        // 前置处理
        postProcessors.forEach { it.postProcessBeforeVersionCreate(context, model, setting) }
        
        // 核心逻辑：创建版本
        val versionId = doCreateVersion(context, model, setting)
        context.versionId = versionId
        
        // 后置处理
        postProcessors.forEach { it.postProcessAfterVersionCreate(context, model, setting) }
    }
}
```

**特点**：
- 使用接口而非抽象类（Kotlin 推荐）
- 提供默认空实现（Java 8+ default method）
- 支持多个后置处理器编排
- 处理器通过 Spring 自动注入

---

### 8.2 其他后置处理器实例

| 后置处理器 | 用途 |
|------------|------|
| `PipelineOperateLogVersionPostProcessor` | 记录操作日志 |
| `PipelineEventVersionPostProcessor` | 发送版本事件 |
| `PipelineModelTaskVersionPostProcessor` | 处理模型任务 |
| `PipelinePermissionVersionPostProcessor` | 处理权限 |
| `PipelineTemplateRelationVersionPostProcessor` | 处理模板关系 |
| `SubPipelineVersionPostProcessor` | 处理子流水线 |
| `PipelineDebugVersionPostProcessor` | 处理调试流水线 |

---

## 9. 适配器模式（Adapter Pattern）

### 9.1 接口适配

**应用场景**：将一个接口转换成客户期望的另一个接口。

**实际案例**：

#### 代码库服务适配器

**位置**：`repository/biz-repository/src/main/kotlin/com/tencent/devops/repository/service/`

**目标接口**：
```kotlin
interface IRepositoryService {
    fun getRepository(projectId: String, repositoryId: String): Repository
    fun listRepositories(projectId: String): List<Repository>
    fun createRepository(projectId: String, request: RepositoryCreateRequest): Repository
}
```

**适配器实现**：
```kotlin
@Service
class CodeGitRepositoryService(
    private val gitApi: GitApi,
    private val credentialService: CredentialService
) : IRepositoryService {
    
    override fun getRepository(projectId: String, repositoryId: String): Repository {
        // 调用 Git API 获取仓库信息
        val gitRepo = gitApi.getRepository(repositoryId)
        
        // 转换为统一的 Repository 模型
        return Repository(
            projectId = projectId,
            repositoryId = repositoryId,
            aliasName = gitRepo.name,
            url = gitRepo.url,
            type = ScmType.CODE_GIT
        )
    }
    
    override fun listRepositories(projectId: String): List<Repository> {
        val gitRepos = gitApi.listRepositories(projectId)
        return gitRepos.map { adaptToRepository(it) }
    }
}

@Service
class CodeSvnRepositoryService(
    private val svnApi: SVNApi,
    private val credentialService: CredentialService
) : IRepositoryService {
    
    override fun getRepository(projectId: String, repositoryId: String): Repository {
        // 调用 SVN API 获取仓库信息
        val svnRepo = svnApi.getRepository(repositoryId)
        
        // 转换为统一的 Repository 模型
        return Repository(
            projectId = projectId,
            repositoryId = repositoryId,
            aliasName = svnRepo.name,
            url = svnRepo.url,
            type = ScmType.CODE_SVN
        )
    }
}
```

**服务注册**：
```kotlin
@Component
class CodeRepositoryServiceLoader : BeanPostProcessor {
    
    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        if (bean is IRepositoryService) {
            CodeRepositoryServiceRegistrar.register(bean)
        }
        return bean
    }
}

object CodeRepositoryServiceRegistrar {
    private val services = mutableMapOf<ScmType, IRepositoryService>()
    
    fun register(service: IRepositoryService) {
        services[service.getScmType()] = service
    }
    
    fun getService(scmType: ScmType): IRepositoryService {
        return services[scmType] ?: throw IllegalArgumentException("Unsupported scm type: $scmType")
    }
}
```

**使用方式**：
```kotlin
val service = CodeRepositoryServiceRegistrar.getService(ScmType.CODE_GIT)
val repo = service.getRepository(projectId, repositoryId)
```

---

## 10. 门面模式（Facade Pattern）

### 10.1 复杂系统的简化接口

**应用场景**：为复杂子系统提供统一的高层接口。

**实际案例**：

#### 参数门面服务

**位置**：`process/biz-process/src/main/kotlin/com/tencent/devops/process/service/ParamFacadeService.kt`

**实现方式**：
```kotlin
@Service
class ParamFacadeService(
    private val buildVariableService: BuildVariableService,
    private val pipelineContextService: PipelineContextService,
    private val secretService: SecretService,
    private val credentialService: CredentialService
) {
    /**
     * 获取构建参数（门面方法）
     */
    fun getBuildParameters(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Map<String, String> {
        // 1. 获取流水线上下文变量
        val contextVars = pipelineContextService.getAllVariables(projectId, pipelineId, buildId)
        
        // 2. 获取构建变量
        val buildVars = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        
        // 3. 获取凭证变量
        val credentialVars = credentialService.getCredentialVariables(projectId, buildId)
        
        // 4. 获取密钥变量
        val secretVars = secretService.getSecretVariables(projectId, buildId)
        
        // 5. 合并所有变量（优先级：secret > credential > build > context）
        return contextVars + buildVars + credentialVars + secretVars
    }
}
```

**特点**：
- 隐藏了多个子系统的复杂交互
- 提供简单统一的接口
- 内部处理优先级和合并逻辑

---

## 设计模式使用统计

### 使用频率排名

| 排名 | 设计模式 | 使用次数（估算） | 典型应用 |
|------|----------|------------------|----------|
| 1 | 单例模式 | 50+ | 工厂类、工具类 |
| 2 | 工厂模式 | 40+ | TaskFactory, ScmFactory, ApiFactory |
| 3 | 策略模式 | 30+ | 权限检查、数据迁移 |
| 4 | 观察者模式 | 25+ | 事件监听器 |
| 5 | 责任链模式 | 10+ | Handler 链、Interceptor 链 |
| 6 | 模板方法模式 | 15+ | 后置处理器 |
| 7 | 装饰器模式 | 5+ | 异常装饰器、权限装饰器 |
| 8 | 建造者模式 | 10+ | 复杂对象构建 |
| 9 | 适配器模式 | 5+ | SCM 适配器 |
| 10 | 门面模式 | 3+ | ParamFacadeService |

---

## 设计模式选择指南

### 何时使用工厂模式？

**场景**：
- ✅ 需要根据类型参数创建不同的对象
- ✅ 对象创建逻辑复杂，需要集中管理
- ✅ 需要支持扩展（插件化）

**示例**：
- `TaskFactory` - 根据任务类型创建任务实例
- `ScmFactory` - 根据代码库类型创建 SCM 实例

---

### 何时使用策略模式？

**场景**：
- ✅ 有多个算法实现同一个接口
- ✅ 运行时根据条件选择算法
- ✅ 避免大量 if-else 分支

**示例**：
- `IUserPipelinePermissionCheckStrategy` - 根据流水线状态选择权限检查策略
- `MigrationStrategy` - 不同数据的迁移策略

---

### 何时使用责任链模式？

**场景**：
- ✅ 请求需要经过多个处理器
- ✅ 每个处理器独立职责
- ✅ 处理顺序可配置

**示例**：
- `StoreCreateHandlerChain` - 参数校验 → 前置业务 → 数据持久化 → 后置业务
- `PipelineInterceptorChain` - 权限检查 → 并发控制 → 配额检查

---

### 何时使用观察者模式？

**场景**：
- ✅ 一个对象状态改变需要通知其他对象
- ✅ 实现模块间解耦
- ✅ 支持动态订阅/取消订阅

**示例**：
- 流水线创建事件 → 通知权限模块、质量模块、通知模块
- 构建完成事件 → 通知制品归档、报告生成、邮件发送

---

### 何时使用装饰器模式？

**场景**：
- ✅ 动态增强对象功能
- ✅ 不修改原有代码
- ✅ 支持多层装饰

**示例**：
- `TaskExecuteExceptionDecorator` - 将各种异常统一装饰成业务异常
- `DelegatingPermissionServiceDecorator` - 在原有权限检查基础上增加额外检查

---

## 最佳实践

### 1. 优先使用 Kotlin 语言特性

```kotlin
// ❌ 避免：Java 风格的工厂
class TaskFactory {
    companion object {
        private var instance: TaskFactory? = null
        fun getInstance(): TaskFactory {
            if (instance == null) {
                synchronized(this) {
                    if (instance == null) {
                        instance = TaskFactory()
                    }
                }
            }
            return instance!!
        }
    }
}

// ✅ 推荐：Kotlin object 单例
object TaskFactory {
    // ...
}
```

---

### 2. 利用 Spring 容器管理对象

```kotlin
// ❌ 避免：手动管理单例
object PermissionService {
    private val rbacService = RbacPermissionService()
    private val v3Service = V3PermissionService()
}

// ✅ 推荐：Spring 依赖注入
@Service
class PermissionService(
    private val rbacService: RbacPermissionService,
    private val v3Service: V3PermissionService
)
```

---

### 3. 使用 Data Class + 命名参数替代传统 Builder

```kotlin
// ❌ 避免：复杂的 Builder
class PipelineBuilder {
    private var projectId: String? = null
    private var pipelineId: String? = null
    
    fun projectId(projectId: String): PipelineBuilder {
        this.projectId = projectId
        return this
    }
    
    fun pipelineId(pipelineId: String): PipelineBuilder {
        this.pipelineId = pipelineId
        return this
    }
    
    fun build(): Pipeline { ... }
}

// ✅ 推荐：Data Class + 命名参数
data class Pipeline(
    val projectId: String,
    val pipelineId: String,
    val name: String = "",
    val desc: String = ""
)

// 使用
val pipeline = Pipeline(
    projectId = "demo",
    pipelineId = "p-123",
    name = "My Pipeline"
)
```

---

### 4. 策略模式与工厂模式结合

```kotlin
// 策略接口
interface IPermissionCheckStrategy {
    fun check(userId: String, resourceId: String): Boolean
}

// 策略工厂
object PermissionCheckStrategyFactory {
    private val strategies = mapOf(
        "RBAC" to RbacPermissionCheckStrategy(),
        "V3" to V3PermissionCheckStrategy()
    )
    
    fun getStrategy(type: String): IPermissionCheckStrategy {
        return strategies[type] ?: throw IllegalArgumentException("Unknown strategy: $type")
    }
}
```

---

### 5. 责任链模式使用 Spring 自动注入

```kotlin
@Service
class StoreCreateService(
    // Spring 自动注入所有 Handler 实现
    private val handlers: List<Handler<StoreCreateRequest>>
) {
    fun create(request: StoreCreateRequest) {
        val chain = StoreCreateHandlerChain(handlers.toMutableList())
        chain.handleRequest(request)
    }
}
```

---

## 相关文件索引

### 工厂模式

- `worker-common/task/TaskFactory.kt` - 任务工厂
- `common-scm/ScmFactory.kt` - SCM 工厂
- `worker-common/api/ApiFactory.kt` - API 工厂
- `common-api/factory/DigestFactory.kt` - 摘要算法工厂

### 策略模式

- `process/biz-process/strategy/bus/` - 权限检查策略
- `misc/biz-misc/strategy/impl/` - 数据迁移策略（20+ 个）
- `log/biz-log/strategy/factory/` - 日志权限检查策略

### 责任链模式

- `store/biz-store/common/handler/` - 研发商店处理链
- `process/biz-base/engine/interceptor/` - 流水线拦截器链
- `common-webhook/service/code/filter/` - Webhook 过滤器链

### 观察者模式

- `process/biz-process/engine/listener/` - 流水线事件监听器
- `quality/biz-quality/listener/` - 质量检查监听器
- `project/biz-project/listener/` - 项目事件监听器

### 模板方法模式

- `process/biz-process/service/pipeline/version/processor/` - 流水线版本后置处理器
- `process/biz-process/service/template/v2/version/processor/` - 模板版本后置处理器

### 装饰器模式

- `worker-common/exception/TaskExecuteExceptionDecorator.kt` - 异常装饰器
- `auth/biz-auth/provider/rbac/service/DelegatingPermissionServiceDecorator.kt` - 权限装饰器

---

## 扩展阅读

- [utility-components (责任链模式)](../utility-components/reference/4-chain-responsibility.md) - 责任链模式详细指南
- [microservice-infrastructure (事件驱动)](../microservice-infrastructure/reference/2-event-driven.md) - 事件驱动架构（观察者模式的应用）
- [01-后端微服务开发](../backend-microservice-development/SKILL.md) - 微服务开发规范

---

## 总结

BK-CI 项目广泛使用了多种设计模式，主要特点：

1. **以 Kotlin 惯用法为主**：使用 `object` 单例、数据类、命名参数等
2. **与 Spring 深度集成**：利用依赖注入、事件机制
3. **注重扩展性**：工厂模式、策略模式支持插件化
4. **模块解耦**：观察者模式、责任链模式实现模块间解耦
5. **代码可维护性**：统一的设计模式降低学习成本

在开发新功能时，应参考现有设计模式实现，保持代码风格一致性。

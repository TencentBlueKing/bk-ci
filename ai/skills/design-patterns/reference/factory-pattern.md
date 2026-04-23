# 工厂模式（Factory Pattern）

## TaskFactory — 任务工厂

**位置**：`worker-common/src/main/kotlin/com/tencent/devops/worker/common/task/TaskFactory.kt`

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
val task = TaskFactory.create(element.getClassType())
task.run(buildTask, param, elementId)
```

**特点**：`object` 单例 + 反射扫描自动注册 + `ConcurrentHashMap` 线程安全 + 优先级覆盖

## ScmFactory — 代码库工厂

**位置**：`common-scm/src/main/kotlin/com/tencent/devops/scm/ScmFactory.kt`

```kotlin
object ScmFactory {
    private val gitApi = GitApi()
    private val svnApi = SVNApi()

    fun getScm(
        projectName: String,
        url: String,
        type: ScmType,  // CODE_SVN, CODE_GIT, CODE_TGIT, CODE_GITLAB, CODE_P4
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

## BkApiHandleFactory — API 处理器工厂（抽象工厂）

**位置**：`common-web/src/main/kotlin/com/tencent/devops/common/web/factory/BkApiHandleFactory.kt`

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

interface BkApiHandleInterface {
    fun code(): String
    fun handle(request: BkApiRequest): BkApiResponse
}
```

## 其他工厂

| 工厂类 | 位置 | 用途 |
|--------|------|------|
| `ApiFactory` | `worker-common/api/ApiFactory.kt` | Worker 端 API 客户端 |
| `CommandFactory` | `worker-common/task/script/CommandFactory.kt` | 脚本命令执行器 |
| `AtomRunConditionFactory` | `worker-common/task/market/AtomRunConditionFactory.kt` | 插件运行条件处理器 |
| `PathFilterFactory` | `common-webhook/service/code/filter/PathFilterFactory.kt` | Webhook 路径过滤器 |
| `DigestFactory` | `common-api/factory/DigestFactory.kt` | 摘要算法实现 |

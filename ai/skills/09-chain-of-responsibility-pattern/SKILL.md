---
name: 09-chain-of-responsibility-pattern
description: 责任链模式实践指南，涵盖责任链设计、拦截器实现、流水线插件链、请求处理链。当用户实现责任链模式、设计拦截器、处理多步骤流程或实现可扩展的处理链时使用。
---

# 责任链模式

责任链模式实践指南.

## 触发条件

当用户需要实现拦截器、过滤器、命令链等链式处理逻辑时，使用此 Skill。

## 模式类型

### 1. 拦截器链（Interceptor Chain）

```kotlin
// 拦截器接口
interface PipelineInterceptor {
    fun execute(task: InterceptData): Response<BuildStatus>
}

// 拦截器链
class PipelineInterceptorChain(
    private val filters: List<PipelineInterceptor>
) {
    fun filter(any: InterceptData): Response<BuildStatus> {
        var result = Response<BuildStatus>(OK)
        run lit@{
            filters.forEach {
                result = it.execute(any)
                if (result.isNotOk()) return@lit  // 短路返回
            }
        }
        return result
    }
}
```

### 2. 命令链（Command Chain）

```kotlin
// 命令接口
interface ContainerCmd : Cmd<ContainerContext> {
    override fun execute(commandContext: ContainerContext)
}

// 命令链
class ContainerCmdChain(
    private val commandList: List<ContainerCmd>
) : CmdChain<ContainerContext> {
    
    override fun nextCommand(commandContext: ContainerContext): Cmd<ContainerContext>? {
        return commandList.getOrNull(commandContext.cmdFlowSeq++)
    }
}

// 命令执行器
fun executeChain(chain: ContainerCmdChain, context: ContainerContext) {
    var cmd = chain.nextCommand(context)
    while (cmd != null) {
        cmd.execute(context)
        cmd = chain.nextCommand(context)
    }
}
```

### 3. 过滤器链（Filter Chain）

```kotlin
// 过滤器接口
interface WebhookFilter {
    fun doFilter(response: WebhookFilterResponse): Boolean
}

// 过滤器链
class WebhookFilterChain(
    private val filters: List<WebhookFilter>
) : WebhookFilter {
    
    override fun doFilter(response: WebhookFilterResponse): Boolean {
        filters.forEach { filter ->
            if (!filter.doFilter(response)) {
                return false  // 任一过滤器拒绝则终止
            }
        }
        return true
    }
}
```

## 实现示例

### 流水线启动拦截器

```kotlin
@Component
class QueueInterceptor : PipelineInterceptor {
    override fun execute(task: InterceptData): Response<BuildStatus> {
        // 检查队列是否已满
        if (isQueueFull(task.pipelineId)) {
            return Response(BuildStatus.QUEUE_TIMEOUT)
        }
        return Response(BuildStatus.SUCCEED)
    }
}

@Component
class PermissionInterceptor : PipelineInterceptor {
    override fun execute(task: InterceptData): Response<BuildStatus> {
        // 检查用户权限
        if (!hasPermission(task.userId, task.pipelineId)) {
            return Response(BuildStatus.FAILED)
        }
        return Response(BuildStatus.SUCCEED)
    }
}
```

### 容器命令实现

```kotlin
@Component
class CheckContainerMutexCmd : ContainerCmd {
    override fun execute(commandContext: ContainerContext) {
        // 检查容器互斥锁
        if (hasMutex(commandContext.containerId)) {
            commandContext.buildStatus = BuildStatus.QUEUE
            commandContext.latestSummary = "等待互斥锁"
        }
    }
}

@Component
class StartContainerCmd : ContainerCmd {
    override fun execute(commandContext: ContainerContext) {
        // 启动容器
        startContainer(commandContext.containerId)
        commandContext.buildStatus = BuildStatus.RUNNING
    }
}
```

## 配置方式

```kotlin
@Configuration
class InterceptorConfiguration {
    
    @Bean
    fun pipelineInterceptorChain(
        interceptors: List<PipelineInterceptor>
    ): PipelineInterceptorChain {
        // 按优先级排序
        val sorted = interceptors.sortedBy { it.order() }
        return PipelineInterceptorChain(sorted)
    }
}
```

## 最佳实践

1. **单一职责**：每个处理器只负责一个功能
2. **顺序控制**：通过 `@Order` 或 `order()` 方法控制执行顺序
3. **短路返回**：失败时立即返回，避免不必要的处理
4. **上下文传递**：使用 Context 对象在链中传递数据

## 相关文件

- `process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/interceptor/`
- `process/biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/command/`
- `common-webhook/biz-common-webhook/src/main/kotlin/com/tencent/devops/common/webhook/service/code/filter/`

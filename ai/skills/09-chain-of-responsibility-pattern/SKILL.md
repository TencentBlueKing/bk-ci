---
name: 09-chain-of-responsibility-pattern
description: 责任链模式实践指南，涵盖责任链设计、拦截器实现、流水线插件链、请求处理链。当用户实现责任链模式、设计拦截器、处理多步骤流程或实现可扩展的处理链时使用。
core_files:
  - "src/backend/ci/core/process/biz-base/src/main/kotlin/com/tencent/devops/process/engine/interceptor/"
  - "src/backend/ci/core/process/biz-engine/src/main/kotlin/com/tencent/devops/process/engine/control/command/"
related_skills:
  - 27-design-patterns
  - 29-3-process-engine-control
token_estimate: 1800
---

# 责任链模式

## Quick Reference

```
拦截器链：PipelineInterceptorChain（短路返回）
命令链：ContainerCmdChain（顺序执行）
过滤器链：WebhookFilterChain（任一拒绝则终止）
```

### 最简示例

```kotlin
// 1. 定义拦截器接口
interface PipelineInterceptor {
    fun execute(task: InterceptData): Response<BuildStatus>
}

// 2. 实现拦截器
@Component
class QueueInterceptor : PipelineInterceptor {
    override fun execute(task: InterceptData): Response<BuildStatus> {
        if (isQueueFull(task.pipelineId)) {
            return Response(BuildStatus.QUEUE_TIMEOUT)
        }
        return Response(BuildStatus.SUCCEED)
    }
}

// 3. 组装责任链
@Bean
fun interceptorChain(interceptors: List<PipelineInterceptor>) =
    PipelineInterceptorChain(interceptors.sortedBy { it.order() })
```

## When to Use

- 多步骤流程处理
- 请求过滤/拦截
- 命令链执行
- 可扩展的处理逻辑

## When NOT to Use

- 单一处理逻辑 → 直接实现
- 需要并行处理 → 使用线程池

---

## 三种责任链模式

### 1. 拦截器链（短路返回）

```kotlin
class PipelineInterceptorChain(private val filters: List<PipelineInterceptor>) {
    fun filter(data: InterceptData): Response<BuildStatus> {
        filters.forEach {
            val result = it.execute(data)
            if (result.isNotOk()) return result  // 短路
        }
        return Response(BuildStatus.SUCCEED)
    }
}
```

### 2. 命令链（顺序执行）

```kotlin
class ContainerCmdChain(private val commands: List<ContainerCmd>) {
    fun execute(context: ContainerContext) {
        commands.forEach { it.execute(context) }
    }
}
```

### 3. 过滤器链（任一拒绝终止）

```kotlin
class WebhookFilterChain(private val filters: List<WebhookFilter>) {
    fun doFilter(response: WebhookFilterResponse): Boolean {
        return filters.all { it.doFilter(response) }
    }
}
```

---

## Checklist

实现责任链前确认：
- [ ] 每个处理器单一职责
- [ ] 使用 @Order 控制执行顺序
- [ ] 失败时正确短路返回
- [ ] 使用 Context 在链中传递数据

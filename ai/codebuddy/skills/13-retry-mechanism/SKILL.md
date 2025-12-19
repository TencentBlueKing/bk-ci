---
name: 13-retry-mechanism
description: 重试机制实践指南
---

# 重试机制

重试机制实践指南.

## 触发条件

当用户需要实现失败重试、网络请求重试、任务重试时，使用此 Skill。

## RetryUtils 工具类

### 1. 通用重试

```kotlin
// Action 接口
interface Action<T> {
    fun execute(): T
    fun fail(e: Throwable): T
}

// 使用示例
val result = RetryUtils.execute(
    action = object : RetryUtils.Action<String> {
        override fun execute(): String {
            return callRemoteService()
        }
        override fun fail(e: Throwable): String {
            logger.error("调用失败", e)
            return "default"
        }
    },
    retryTime = 3,
    retryPeriodMills = 500
)
```

### 2. 客户端重试（支持 429 限流）

```kotlin
val result = RetryUtils.clientRetry(
    retryTime = 5,
    retryPeriodMills = 500
) {
    client.get(ServiceXxxResource::class).doSomething()
}
```

### 3. 任意异常重试

```kotlin
RetryUtils.retryAnyException(
    retryTime = 3,
    retryPeriodMills = 50
) { retryCount ->
    logger.info("第 $retryCount 次尝试")
    doSomethingRisky()
}
```

## 重试策略

### 固定间隔

```kotlin
RetryUtils.execute(
    action = action,
    retryTime = 3,
    retryPeriodMills = 1000  // 每次重试间隔 1 秒
)
```

### 指数退避

```kotlin
fun <T> retryWithBackoff(
    maxRetries: Int,
    initialDelay: Long,
    action: () -> T
): T {
    var delay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return action()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            Thread.sleep(delay)
            delay *= 2  // 指数增长
        }
    }
    throw IllegalStateException("Should not reach here")
}
```

## 使用场景

| 场景 | 重试次数 | 间隔 | 说明 |
|------|---------|------|------|
| 服务调用 | 3 | 500ms | 网络抖动 |
| 数据库操作 | 2 | 100ms | 死锁重试 |
| 外部 API | 5 | 1000ms | 限流处理 |
| 消息发送 | 3 | 200ms | 队列繁忙 |

## 最佳实践

1. **幂等性**：确保重试操作是幂等的
2. **合理次数**：根据业务场景设置重试次数
3. **退避策略**：避免重试风暴
4. **日志记录**：记录每次重试的原因

## 相关文件

- `common-service/src/main/kotlin/com/tencent/devops/common/service/utils/RetryUtils.kt`

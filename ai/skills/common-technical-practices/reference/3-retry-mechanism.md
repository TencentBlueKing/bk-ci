# 重试机制实践指南

# 重试机制

## Quick Reference

```
通用重试：RetryUtils.execute(action, retryTime, retryPeriodMills)
客户端重试：RetryUtils.clientRetry(retryTime, retryPeriodMills) { ... }
任意异常：RetryUtils.retryAnyException(retryTime, retryPeriodMills) { ... }
```

### 最简示例

```kotlin
// 方式1：客户端重试（推荐，支持 429 限流）
val result = RetryUtils.clientRetry(retryTime = 3, retryPeriodMills = 500) {
    client.get(ServiceXxxResource::class).doSomething()
}

// 方式2：通用重试
val result = RetryUtils.execute(
    action = object : RetryUtils.Action<String> {
        override fun execute() = callRemoteService()
        override fun fail(e: Throwable) = "default"
    },
    retryTime = 3,
    retryPeriodMills = 500
)

// 方式3：任意异常重试
RetryUtils.retryAnyException(retryTime = 3, retryPeriodMills = 50) { retryCount ->
    logger.info("第 $retryCount 次尝试")
    doSomethingRisky()
}
```

## When to Use

- 网络请求重试
- 数据库死锁重试
- 外部 API 限流处理
- 消息发送重试

## When NOT to Use

- 非幂等操作
- 业务逻辑错误（不应重试）

---

## 重试策略建议

| 场景 | 重试次数 | 间隔 | 说明 |
|------|---------|------|------|
| 服务调用 | 3 | 500ms | 网络抖动 |
| 数据库操作 | 2 | 100ms | 死锁重试 |
| 外部 API | 5 | 1000ms | 限流处理 |

## 指数退避

```kotlin
fun <T> retryWithBackoff(maxRetries: Int, initialDelay: Long, action: () -> T): T {
    var delay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return action()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            Thread.sleep(delay)
            delay *= 2
        }
    }
    throw IllegalStateException()
}
```

---

## Checklist

实现重试前确认：
- [ ] 操作是幂等的
- [ ] 设置合理的重试次数
- [ ] 使用退避策略避免重试风暴
- [ ] 记录每次重试日志

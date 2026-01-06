---
name: 25-thread-pool-loop-util
description: 线程池与循环工具指南，涵盖线程池配置、任务提交、批量处理、循环工具类、并发控制。当用户配置线程池、实现批量并发处理、使用循环工具或优化并发性能时使用。
---

# 线程池与循环工具

线程池与循环工具指南.

## 触发条件

当用户需要实现线程池配置、MDC 传递、安全循环时，使用此 Skill。

## ThreadPoolUtil

```kotlin
object ThreadPoolUtil {
    // 创建带 MDC 的线程池
    fun createThreadPool(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long = 60L,
        workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue(1000),
        threadNamePrefix: String = "bk-pool"
    ): ThreadPoolExecutor {
        return object : ThreadPoolExecutor(
            corePoolSize,
            maximumPoolSize,
            keepAliveTime,
            TimeUnit.SECONDS,
            workQueue,
            NamedThreadFactory(threadNamePrefix)
        ) {
            override fun execute(command: Runnable) {
                // 传递 MDC 上下文
                val context = MDC.getCopyOfContextMap()
                super.execute {
                    try {
                        context?.let { MDC.setContextMap(it) }
                        command.run()
                    } finally {
                        MDC.clear()
                    }
                }
            }
        }
    }
}
```

## 使用示例

```kotlin
@Configuration
class ThreadPoolConfiguration {
    
    @Bean("buildExecutor")
    fun buildExecutor(): ThreadPoolExecutor {
        return ThreadPoolUtil.createThreadPool(
            corePoolSize = 10,
            maximumPoolSize = 50,
            keepAliveTime = 60,
            threadNamePrefix = "build-pool"
        )
    }
}

@Service
class BuildService(
    @Qualifier("buildExecutor")
    private val executor: ThreadPoolExecutor
) {
    fun asyncBuild(buildId: String) {
        executor.execute {
            // MDC 上下文已自动传递
            logger.info("开始构建: $buildId")
            doBuild(buildId)
        }
    }
}
```

## LoopUtil 安全循环

```kotlin
object LoopUtil {
    // 带超时的循环
    fun <T> loopWithTimeout(
        timeoutMs: Long,
        intervalMs: Long = 100,
        action: () -> T?
    ): T? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val result = action()
            if (result != null) return result
            Thread.sleep(intervalMs)
        }
        return null
    }
    
    // 带最大次数的循环
    fun <T> loopWithMaxTimes(
        maxTimes: Int,
        intervalMs: Long = 100,
        action: (Int) -> T?
    ): T? {
        repeat(maxTimes) { index ->
            val result = action(index)
            if (result != null) return result
            if (index < maxTimes - 1) Thread.sleep(intervalMs)
        }
        return null
    }
}
```

## 使用示例

```kotlin
// 等待构建完成（最多 5 分钟）
val buildResult = LoopUtil.loopWithTimeout(
    timeoutMs = 5 * 60 * 1000,
    intervalMs = 1000
) {
    val status = getBuildStatus(buildId)
    if (status.isFinished()) status else null
}

// 重试获取锁（最多 10 次）
val locked = LoopUtil.loopWithMaxTimes(
    maxTimes = 10,
    intervalMs = 500
) { attempt ->
    logger.info("第 ${attempt + 1} 次尝试获取锁")
    if (tryLock()) true else null
}
```

## 线程池监控

```kotlin
fun monitorThreadPool(executor: ThreadPoolExecutor) {
    logger.info("""
        ThreadPool Status:
        - Active: ${executor.activeCount}
        - Pool Size: ${executor.poolSize}
        - Core Size: ${executor.corePoolSize}
        - Max Size: ${executor.maximumPoolSize}
        - Queue Size: ${executor.queue.size}
        - Completed: ${executor.completedTaskCount}
    """.trimIndent())
}
```

## 最佳实践

1. **MDC 传递**：异步任务保持日志追踪
2. **合理配置**：根据业务场景配置线程池
3. **监控告警**：监控队列积压情况
4. **优雅关闭**：使用 `shutdown()` 和 `awaitTermination()`

## 相关文件

- `common-util/src/main/kotlin/com/tencent/devops/common/util/ThreadPoolUtil.kt`
- `common-util/src/main/kotlin/com/tencent/devops/common/util/LoopUtil.kt`

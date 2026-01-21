---
name: thread-pool-loop-util
description: 线程池与循环工具指南，涵盖线程池配置、任务提交、批量处理、循环工具类、并发控制。当用户配置线程池、实现批量并发处理、使用循环工具或优化并发性能时使用。
core_files:
  - "src/backend/ci/core/common/common-util/src/main/kotlin/com/tencent/devops/common/util/ThreadPoolUtil.kt"
  - "src/backend/ci/core/common/common-util/src/main/kotlin/com/tencent/devops/common/util/LoopUtil.kt"
related_skills:
  - 10-distributed-lock
token_estimate: 1500
---

# 线程池与循环工具

## Quick Reference

```
线程池：ThreadPoolUtil.createThreadPool()（自动传递 MDC 上下文）
安全循环：LoopUtil.loopWithTimeout() | LoopUtil.loopWithMaxTimes()
```

### 最简示例

```kotlin
// 创建带 MDC 的线程池
@Bean("buildExecutor")
fun buildExecutor(): ThreadPoolExecutor {
    return ThreadPoolUtil.createThreadPool(
        corePoolSize = 10,
        maximumPoolSize = 50,
        keepAliveTime = 60,
        threadNamePrefix = "build-pool"
    )
}

// 使用线程池
@Service
class BuildService(@Qualifier("buildExecutor") private val executor: ThreadPoolExecutor) {
    fun asyncBuild(buildId: String) {
        executor.execute {
            // MDC 上下文已自动传递
            logger.info("开始构建: $buildId")
            doBuild(buildId)
        }
    }
}

// 带超时的循环（等待构建完成，最多 5 分钟）
val result = LoopUtil.loopWithTimeout(timeoutMs = 5 * 60 * 1000, intervalMs = 1000) {
    val status = getBuildStatus(buildId)
    if (status.isFinished()) status else null
}

// 带最大次数的循环（重试获取锁，最多 10 次）
val locked = LoopUtil.loopWithMaxTimes(maxTimes = 10, intervalMs = 500) { attempt ->
    if (tryLock()) true else null
}
```

## When to Use

- 异步任务执行
- 轮询等待结果
- 重试操作

---

## ThreadPoolUtil

```kotlin
object ThreadPoolUtil {
    fun createThreadPool(
        corePoolSize: Int,
        maximumPoolSize: Int,
        keepAliveTime: Long = 60L,
        workQueue: BlockingQueue<Runnable> = LinkedBlockingQueue(1000),
        threadNamePrefix: String = "bk-pool"
    ): ThreadPoolExecutor
}
```

**特点**：自动传递 MDC 上下文，保持日志追踪链路。

## LoopUtil

```kotlin
object LoopUtil {
    // 带超时的循环
    fun <T> loopWithTimeout(timeoutMs: Long, intervalMs: Long = 100, action: () -> T?): T?
    
    // 带最大次数的循环
    fun <T> loopWithMaxTimes(maxTimes: Int, intervalMs: Long = 100, action: (Int) -> T?): T?
}
```

## 线程池监控

```kotlin
logger.info("""
    ThreadPool: Active=${executor.activeCount}, Pool=${executor.poolSize},
    Queue=${executor.queue.size}, Completed=${executor.completedTaskCount}
""")
```

---

## Checklist

- [ ] 异步任务使用 ThreadPoolUtil 保持 MDC
- [ ] 根据业务场景配置线程池参数
- [ ] 监控队列积压情况
- [ ] 使用 shutdown() 优雅关闭

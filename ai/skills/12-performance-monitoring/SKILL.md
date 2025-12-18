---
name: 12-performance-monitoring
description: 性能监控指南
---

# 性能监控

性能监控指南.

## 触发条件

当用户需要实现方法耗时监控、性能指标采集时，使用此 Skill。

## @BkTimed 注解

```kotlin
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class BkTimed(
    val value: String = "",
    val percentiles: DoubleArray = [0.5, 0.9, 0.95, 0.99],
    val histogram: Boolean = false
)
```

### 使用示例

```kotlin
@Service
class BuildService {
    
    @BkTimed("build_start_duration")
    fun startBuild(pipelineId: String): BuildId {
        // 自动记录方法执行时间到 Prometheus
        return doBuild(pipelineId)
    }
}
```

## Watcher 耗时监控

```kotlin
class Watcher(
    val id: String,
    private val createTime: Long = System.currentTimeMillis()
) {
    fun start(name: String)
    fun stop()
    fun elapsed(): Long
    fun totalTimeMillis(): Long
}
```

### 使用示例

```kotlin
fun processTask(taskId: String) {
    val watcher = Watcher(id = "TASK|Process|$taskId")
    
    try {
        watcher.start("validate")
        validateTask(taskId)
        watcher.stop()
        
        watcher.start("execute")
        executeTask(taskId)
        watcher.stop()
        
        watcher.start("notify")
        notifyResult(taskId)
        watcher.stop()
    } finally {
        watcher.stop()
        LogUtils.printCostTimeWE(watcher = watcher)
    }
}

// 输出: watcher|TASK|Process|xxx|total=150|elapsed=160|validate=20|execute=100|notify=30
```

## 日志格式

```kotlin
// 标准格式
watcher|{id}|total={total}|elapsed={elapsed}|{step1}={time1}|{step2}={time2}

// 示例
watcher|ENGINE|BuildStart|b-123|total=150|elapsed=160|init=20|dispatch=100|notify=30
```

## 监控指标

| 指标 | 说明 |
|------|------|
| `total` | 所有步骤耗时总和 |
| `elapsed` | 实际经过时间 |
| `{step}` | 各步骤耗时 |

## 最佳实践

1. **关键路径监控**：对核心业务流程添加监控
2. **合理命名**：使用 `模块|操作|资源ID` 格式
3. **finally 释放**：确保 watcher 在 finally 中 stop
4. **阈值告警**：对超时操作设置告警

## 相关文件

- `common-service/src/main/kotlin/com/tencent/devops/common/service/prometheus/BkTimed.kt`
- `common-api/src/main/kotlin/com/tencent/devops/common/api/util/Watcher.kt`

---
name: 12-performance-monitoring
description: 性能监控指南，涵盖 Micrometer 指标采集、Prometheus 集成、性能埋点、慢查询监控、JVM 监控。当用户添加性能监控、配置指标采集、分析性能瓶颈或实现可观测性时使用。
core_files:
  - "src/backend/ci/core/common/common-service/src/main/kotlin/com/tencent/devops/common/service/prometheus/BkTimed.kt"
  - "src/backend/ci/core/common/common-api/src/main/kotlin/com/tencent/devops/common/api/util/Watcher.kt"
related_skills:
  - 14-i18n-and-logging
token_estimate: 1200
---

# 性能监控

## Quick Reference

```
方法耗时注解：@BkTimed("metric_name")
手动监控：Watcher(id) → start(step) → stop() → LogUtils.printCostTimeWE(watcher)
日志格式：watcher|{id}|total={total}|elapsed={elapsed}|{step}={time}
```

### 最简示例

```kotlin
// 方式1：注解方式
@Service
class BuildService {
    @BkTimed("build_start_duration")
    fun startBuild(pipelineId: String): BuildId {
        return doBuild(pipelineId)
    }
}

// 方式2：手动 Watcher
fun processTask(taskId: String) {
    val watcher = Watcher(id = "TASK|Process|$taskId")
    try {
        watcher.start("validate")
        validateTask(taskId)
        watcher.stop()
        
        watcher.start("execute")
        executeTask(taskId)
        watcher.stop()
    } finally {
        LogUtils.printCostTimeWE(watcher)
    }
}
// 输出: watcher|TASK|Process|xxx|total=150|elapsed=160|validate=20|execute=100
```

## When to Use

- 方法耗时监控
- 关键路径性能分析
- 慢查询定位

## When NOT to Use

- 简单快速操作
- 高频调用的热点代码（注意性能开销）

---

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

## Watcher 手动监控

```kotlin
val watcher = Watcher(id = "模块|操作|资源ID")
watcher.start("步骤名")
// ... 业务逻辑
watcher.stop()
LogUtils.printCostTimeWE(watcher)
```

---

## Checklist

添加监控前确认：
- [ ] 关键业务流程添加监控
- [ ] 使用 `模块|操作|资源ID` 格式命名
- [ ] watcher 在 finally 中 stop
- [ ] 设置合理的告警阈值

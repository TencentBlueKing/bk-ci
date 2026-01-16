---
name: 08-event-driven-architecture
description: 事件驱动架构指南，涵盖 MQ 消息队列使用、事件发布订阅、异步处理模式、事件溯源、最终一致性。当用户实现异步通信、发布事件、订阅消息队列或设计松耦合架构时使用。
core_files:
  - "src/backend/ci/core/common/common-event/"
  - "src/backend/ci/core/common/common-event/src/main/kotlin/com/tencent/devops/common/event/pojo/IEvent.kt"
related_skills:
  - 29-5-process-event-driven
  - 11-service-communication
token_estimate: 1800
---

# 事件驱动架构

## Quick Reference

```
事件基类：IEvent（含 delayMills、retryTime、mqResend）
发布方式：event.sendTo(streamBridge, "destination")
消费注解：@EventConsumer + ScsConsumerBuilder.build<EventType>
广播事件：*BroadCastEvent（多消费者）
```

### 最简示例

```kotlin
// 1. 定义事件
data class PipelineBuildStartEvent(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    override var delayMills: Int = 0
) : IEvent()

// 2. 发布事件
@Component
class PipelineEventDispatcher(private val streamBridge: StreamBridge) {
    fun dispatch(event: PipelineBuildStartEvent) {
        event.sendTo(streamBridge, "pipeline-build-start")
    }
}

// 3. 消费事件
@Configuration
class EventConfig {
    @EventConsumer
    fun buildStartConsumer(@Autowired listener: BuildListener) =
        ScsConsumerBuilder.build<PipelineBuildStartEvent> { listener.onStart(it) }
}
```

## When to Use

- 实现异步处理
- 微服务间解耦通信
- 发布/订阅模式
- 事件溯源

## When NOT to Use

- 需要同步响应 → 使用 `11-service-communication`
- 简单方法调用 → 直接依赖注入

---

## IEvent 事件基类

```kotlin
open class IEvent(
    open var delayMills: Int = 0,      // 延迟发送（毫秒）
    open var retryTime: Int = 1,        // 重试次数
    open var mqResend: Int = 0,         // 当前重投次数
    open val mqResendMax: Int = 2       // 最大重投次数
) {
    fun sendTo(bridge: StreamBridge, destination: String? = null)
    fun canResend() = mqResend < mqResendMax
}
```

## 常用事件类型

| 事件类型 | 说明 | 消费模式 |
|---------|------|---------|
| `PipelineBuildStartEvent` | 构建开始 | 单消费者 |
| `PipelineBuildFinishEvent` | 构建结束 | 单消费者 |
| `*BroadCastEvent` | 广播事件 | 多消费者 |
| `PipelineAgentStartupEvent` | Agent 启动 | 单消费者 |

## 消息队列配置

```yaml
spring:
  cloud:
    stream:
      bindings:
        pipeline-build-start:
          destination: pipeline.build.start
          group: ${spring.application.name}
```

---

## Checklist

实现事件驱动前确认：
- [ ] 事件类继承 IEvent
- [ ] 消费者必须支持幂等（重复消费）
- [ ] 使用 delayMills 实现延迟处理
- [ ] 跨服务通知使用 BroadCast 事件

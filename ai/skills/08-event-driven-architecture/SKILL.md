---
name: 08-event-driven-architecture
description: 事件驱动架构指南，涵盖 MQ 消息队列使用、事件发布订阅、异步处理模式、事件溯源、最终一致性。当用户实现异步通信、发布事件、订阅消息队列或设计松耦合架构时使用。
---

# 事件驱动架构

事件驱动架构指南.

## 触发条件

当用户需要实现异步处理、消息队列、事件发布订阅时，使用此 Skill。

## 核心组件

### 1. IEvent 事件基类

```kotlin
open class IEvent(
    open var delayMills: Int = 0,        // 延迟发送（毫秒）
    open var retryTime: Int = 1,          // 重试次数
    open var mqResend: Int = 0,           // 当前重投次数
    open val mqResendMax: Int = 2         // 最大重投次数
) {
    // 发送到指定目标
    fun sendTo(bridge: StreamBridge, destination: String? = null)
    
    // 是否可以重发
    fun canResend() = mqResend < mqResendMax
}
```

### 2. 事件定义示例

```kotlin
// 流水线构建开始事件
data class PipelineBuildStartEvent(
    val projectId: String,
    val pipelineId: String,
    val buildId: String,
    val userId: String,
    override var delayMills: Int = 0
) : IEvent()

// 广播事件（多个消费者）
data class PipelineBuildStartBroadCastEvent(
    val projectId: String,
    val pipelineId: String,
    val buildId: String
) : IEvent()
```

### 3. 事件发布

```kotlin
@Component
class PipelineEventDispatcher(
    private val streamBridge: StreamBridge
) {
    fun dispatch(event: PipelineBuildStartEvent) {
        event.sendTo(streamBridge, "pipeline-build-start")
    }
}
```

### 4. 事件消费

```kotlin
@Configuration
class DispatchMQConfiguration {
    
    @EventConsumer
    fun thirdAgentDispatchStartConsumer(
        @Autowired thirdPartyAgentListener: ThirdPartyBuildListener
    ) = ScsConsumerBuilder.build<PipelineAgentStartupEvent> { 
        thirdPartyAgentListener.handleStartup(it) 
    }
    
    @EventConsumer
    fun buildFinishConsumer(
        @Autowired listener: BuildFinishListener
    ) = ScsConsumerBuilder.build<PipelineBuildFinishEvent> {
        listener.onBuildFinish(it)
    }
}
```

## 事件类型

| 事件类型 | 说明 | 消费模式 |
|---------|------|---------|
| `PipelineBuildStartEvent` | 构建开始 | 单消费者 |
| `PipelineBuildFinishEvent` | 构建结束 | 单消费者 |
| `PipelineBuildStartBroadCastEvent` | 构建开始广播 | 多消费者 |
| `PipelineBuildFinishBroadCastEvent` | 构建结束广播 | 多消费者 |
| `PipelineAgentStartupEvent` | Agent 启动 | 单消费者 |

## 消息队列配置

```yaml
# application.yml
spring:
  cloud:
    stream:
      bindings:
        pipeline-build-start:
          destination: pipeline.build.start
          group: ${spring.application.name}
        pipeline-build-finish:
          destination: pipeline.build.finish
          group: ${spring.application.name}
```

## 最佳实践

1. **幂等性**：消费者必须支持重复消费
2. **延迟发送**：使用 `delayMills` 实现延迟处理
3. **重试机制**：利用 `retryTime` 和 `canResend()` 处理失败
4. **广播事件**：跨服务通知使用 BroadCast 事件

## 相关文件

- `common-event/src/main/kotlin/com/tencent/devops/common/event/pojo/IEvent.kt`
- `common-event/src/main/kotlin/com/tencent/devops/common/event/annotation/EventConsumer.kt`

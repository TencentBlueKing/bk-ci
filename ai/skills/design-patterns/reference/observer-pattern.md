# 观察者模式（Observer Pattern）— Spring Event 实现

## 流水线事件监听

**事件定义**：
```kotlin
data class ProjectBroadCastEvent(
    val projectId: String,
    val eventType: EventType,
    val userId: String
) : ApplicationEvent(projectId)
```

**监听器接口**：
```kotlin
interface ProjectEventListener : EventListener<ProjectBroadCastEvent> {
    override fun execute(event: ProjectBroadCastEvent)
}
```

**具体监听器**：
```kotlin
@Component
class SampleProjectEventListener : ProjectEventListener {
    override fun execute(event: ProjectBroadCastEvent) {
        logger.info("Received project event: ${event.eventType} for ${event.projectId}")
        when (event.eventType) {
            EventType.CREATE -> handleProjectCreate(event)
            EventType.UPDATE -> handleProjectUpdate(event)
            EventType.DELETE -> handleProjectDelete(event)
        }
    }
}
```

**发布事件**：
```kotlin
@Service
class ProjectService(
    private val applicationEventPublisher: ApplicationEventPublisher
) {
    fun createProject(projectId: String, userId: String) {
        // ... 创建项目逻辑
        applicationEventPublisher.publishEvent(
            ProjectBroadCastEvent(
                projectId = projectId,
                eventType = EventType.CREATE,
                userId = userId
            )
        )
    }
}
```

## 其他监听器

| 监听器 | 用途 | 位置 |
|--------|------|------|
| `PipelineBuildQualityListener` | 流水线构建质量检查 | `quality/biz-quality/` |
| `WebhookEventListener` | Webhook 事件处理 | `process/biz-process/webhook/` |
| `PipelineTimerBuildListener` | 定时触发构建 | `process/biz-process/plugin/trigger/timer/` |
| `PipelineBuildNotifyListener` | 构建通知 | `process/biz-process/notify/` |
| `PipelineWebSocketListener` | WebSocket 消息推送 | `process/biz-process/websocket/` |

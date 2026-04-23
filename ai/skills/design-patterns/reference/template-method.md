# 模板方法模式（Template Method Pattern）

## 流水线版本创建后置处理器

**位置**：`process/biz-process/src/main/kotlin/com/tencent/devops/process/service/pipeline/version/processor/`

```kotlin
interface PipelineVersionCreatePostProcessor {
    fun postProcessBeforeVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) { /* 默认空实现 */ }

    fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) { /* 默认空实现 */ }
}
```

**具体实现**：
```kotlin
@Service
class PipelineOperateLogVersionPostProcessor : PipelineVersionCreatePostProcessor {
    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) {
        pipelineOperateLogService.save(
            projectId = context.projectId,
            pipelineId = context.pipelineId,
            versionId = context.versionId,
            operateType = "CREATE",
            userId = context.userId
        )
    }
}

@Service
class PipelineEventVersionPostProcessor : PipelineVersionCreatePostProcessor {
    override fun postProcessAfterVersionCreate(
        context: PipelineVersionCreateContext,
        pipelineModel: Model,
        pipelineSetting: PipelineSetting
    ) {
        applicationEventPublisher.publishEvent(
            PipelineVersionCreateEvent(
                projectId = context.projectId,
                pipelineId = context.pipelineId,
                versionId = context.versionId
            )
        )
    }
}
```

**处理器编排**：
```kotlin
@Service
class PipelineVersionCreateService(
    private val postProcessors: List<PipelineVersionCreatePostProcessor>
) {
    fun createVersion(context: PipelineVersionCreateContext, model: Model, setting: PipelineSetting) {
        postProcessors.forEach { it.postProcessBeforeVersionCreate(context, model, setting) }
        val versionId = doCreateVersion(context, model, setting)
        context.versionId = versionId
        postProcessors.forEach { it.postProcessAfterVersionCreate(context, model, setting) }
    }
}
```

## 其他后置处理器

| 后置处理器 | 用途 |
|------------|------|
| `PipelineOperateLogVersionPostProcessor` | 记录操作日志 |
| `PipelineEventVersionPostProcessor` | 发送版本事件 |
| `PipelineModelTaskVersionPostProcessor` | 处理模型任务 |
| `PipelinePermissionVersionPostProcessor` | 处理权限 |
| `PipelineTemplateRelationVersionPostProcessor` | 处理模板关系 |
| `SubPipelineVersionPostProcessor` | 处理子流水线 |
| `PipelineDebugVersionPostProcessor` | 处理调试流水线 |

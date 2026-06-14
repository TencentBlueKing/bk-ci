package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线批量任务明细错误信息-依赖资源创建失败")
data class PipelineBatchTaskDependencyFailed(
    val details: List<PipelineBatchTaskDependencyFailedGroup>
) : PipelineBatchTaskErrorMessage {
    companion object {
        const val classType = "dependencyFailed"
    }
}

@Schema(title = "流水线批量任务依赖失败分组")
data class PipelineBatchTaskDependencyFailedGroup(
    @get:Schema(description = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "失败资源列表", required = true)
    val resources: List<PipelineBatchTaskDependencyFailedResource>
)

@Schema(title = "流水线批量任务依赖失败资源")
data class PipelineBatchTaskDependencyFailedResource(
    @get:Schema(description = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "资源名称", required = true)
    val resourceName: String,
    @get:Schema(description = "错误信息")
    val errorMessage: PipelineBatchTaskErrorMessage? = null
)

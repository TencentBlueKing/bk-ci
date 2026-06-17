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

    override fun errorMessageText(): Any {
        return details.map { it.toVo() }
    }

    private fun PipelineBatchTaskDependencyFailedGroup.toVo(): PipelineBatchTaskDependencyFailedGroupVo {
        return PipelineBatchTaskDependencyFailedGroupVo(
            resourceType = resourceType,
            resourceTypeName = resourceType.getI18nName(),
            resources = resources.map { resource ->
                PipelineBatchTaskDependencyFailedResourceVo(
                    resourceId = resource.resourceId,
                    resourceName = resource.resourceName,
                    errorMessageText = resource.errorMessage?.errorMessageText()
                )
            }
        )
    }
}

@Schema(title = "流水线批量任务依赖失败分组")
data class PipelineBatchTaskDependencyFailedGroup(
    @get:Schema(description = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "失败资源列表", required = true)
    val resources: List<PipelineBatchTaskDependencyFailedResource>
)

@Schema(title = "流水线批量任务依赖失败分组展示")
data class PipelineBatchTaskDependencyFailedGroupVo(
    @get:Schema(description = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "资源类型名称", required = true)
    val resourceTypeName: String,
    @get:Schema(description = "失败资源列表", required = true)
    val resources: List<PipelineBatchTaskDependencyFailedResourceVo>
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

@Schema(title = "流水线批量任务依赖失败资源展示")
data class PipelineBatchTaskDependencyFailedResourceVo(
    @get:Schema(description = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "资源名称", required = true)
    val resourceName: String,
    @get:Schema(description = "错误信息")
    val errorMessageText: Any? = null
)

package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源保存请求")
data class PipelineCopyTaskSaveResourceRequest(
    @get:Schema(description = "流水线标签复制策略")
    val pipelineLabelCopyStrategy: PipelineCopyStrategy? = null,
    @get:Schema(description = "流水线组复制策略")
    val pipelineGroupCopyStrategy: PipelineCopyStrategy? = null,
    @get:Schema(description = "资源明细", required = true)
    val resources: List<PipelineCopyTaskSaveResource>
) {
    fun getCopyStrategy(resource: PipelineCopyTaskSaveResource): PipelineCopyStrategy? {
        return when (resource.resourceType) {
            PipelineDependentResourceType.PIPELINE_LABEL -> pipelineLabelCopyStrategy
            PipelineDependentResourceType.PIPELINE_GROUP -> pipelineGroupCopyStrategy
            else -> resource.copyStrategy
        }
    }
}

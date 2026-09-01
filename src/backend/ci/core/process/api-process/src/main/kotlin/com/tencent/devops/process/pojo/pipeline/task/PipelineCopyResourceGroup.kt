package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源分组")
data class PipelineCopyResourceGroup(
    @get:Schema(description = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "资源总数", required = true)
    val totalCount: Int,
    @get:Schema(description = "未处理的资源数", required = true)
    val unprocessedCount: Int = 0,
    @get:Schema(description = "资源信息", required = true)
    val resources: List<PipelineCopyTaskResource>
)

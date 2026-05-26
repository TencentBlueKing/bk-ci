package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源基础信息分组")
data class PipelineCopyResourceBasicInfoGroup(
    @get:Schema(description = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "资源数量", required = true)
    val resourceCount: Long,
    @get:Schema(description = "源项目被引用的数量", required = true)
    val sourceProjectReferCount: Long,
    @get:Schema(description = "资源基础信息", required = true)
    val resources: List<PipelineCopyResourceBasicInfo>
)

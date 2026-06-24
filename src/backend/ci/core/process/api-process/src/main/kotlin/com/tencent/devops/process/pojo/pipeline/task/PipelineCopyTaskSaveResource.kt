package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源保存明细")
data class PipelineCopyTaskSaveResource(
    @get:Schema(description = "源资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "源资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "源资源名", required = true)
    val resourceName: String,
    @get:Schema(description = "源资源属性")
    val resourceProperties: PipelineCopyResourceProp? = null,
    @get:Schema(description = "复制策略")
    val copyStrategy: PipelineCopyStrategy? = null,
    @get:Schema(description = "目标资源ID")
    val targetResourceId: String? = null,
    @get:Schema(description = "目标资源名")
    val targetResourceName: String? = null
)

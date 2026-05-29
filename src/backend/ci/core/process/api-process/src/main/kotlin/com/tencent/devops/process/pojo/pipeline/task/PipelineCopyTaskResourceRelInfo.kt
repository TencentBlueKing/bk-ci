package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务资源关系")
data class PipelineCopyTaskResourceRelInfo(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(description = "流水线名称", required = true)
    val pipelineName: String,
    @get:Schema(description = "源资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "源资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "源资源名", required = true)
    val resourceName: String,
    @get:Schema(description = "创建时间")
    val createTime: Long? = null,
    @get:Schema(description = "更新时间")
    val updateTime: Long? = null
)

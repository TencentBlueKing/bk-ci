package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制资源配置信息")
data class PipelineCopyResourceDetail(
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "源资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "源资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "源资源名", required = true)
    val resourceName: String,
    @get:Schema(description = "源资源属性")
    val resourceProperties: PipelineCopyResourceProperties? = null,
    @get:Schema(description = "复制策略")
    val copyStrategy: PipelineCopyStrategy? = null,
    @get:Schema(description = "目标项目ID")
    val targetProjectId: String? = null,
    @get:Schema(description = "目标资源类型")
    val targetResourceType: PipelineDependentResourceType? = null,
    @get:Schema(description = "目标资源ID")
    val targetResourceId: String? = null,
    @get:Schema(description = "目标资源名")
    val targetResourceName: String? = null,
    @get:Schema(description = "目标资源属性")
    val targetResourceProperties: PipelineCopyResourceProperties? = null,
    @get:Schema(description = "资源状态", required = true)
    val status: PipelineCopyTaskResourceStatus = PipelineCopyTaskResourceStatus.UNPROCESSED,
    @get:Schema(description = "是否高危资源", required = true)
    val highRisk: Boolean = false,
    @get:Schema(description = "目标项目是否存在同名资源", required = true)
    val targetNameExists: Boolean = false,
    @get:Schema(description = "目标ID是否存在", required = true)
    val targetIdExists: Boolean = false
)

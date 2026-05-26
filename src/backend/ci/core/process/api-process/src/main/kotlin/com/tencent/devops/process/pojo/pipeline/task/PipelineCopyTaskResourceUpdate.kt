package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务资源更新")
data class PipelineCopyTaskResourceUpdate(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "资源类型", required = true)
    val resourceType: PipelineDependentResourceType,
    @get:Schema(description = "资源ID", required = true)
    val resourceId: String,
    @get:Schema(description = "目标资源类型")
    val targetResourceType: PipelineDependentResourceType? = null,
    @get:Schema(description = "目标资源ID")
    val targetResourceId: String? = null,
    @get:Schema(description = "目标资源名")
    val targetResourceName: String? = null,
    @get:Schema(description = "目标资源属性")
    val targetResourceProperties: PipelineCopyResourceProperties? = null,
    @get:Schema(description = "资源状态")
    val status: PipelineCopyTaskResourceStatus? = null,
    @get:Schema(description = "错误信息")
    val errorMessage: String? = null,
    @get:Schema(description = "目标项目是否存在同名资源")
    val targetNameExists: Boolean? = null,
    @get:Schema(description = "目标ID是否存在")
    val targetIdExists: Boolean? = null,
    @get:Schema(description = "是否自动完成")
    val autoFinish: Boolean? = null,
    @get:Schema(description = "资源是否需要补齐")
    val needCompletion: Boolean? = null,
    @get:Schema(description = "资源是否需要迁移")
    val needTransfer: Boolean? = null,
    @get:Schema(description = "用户是否已确认处理完成")
    val confirmed: Boolean? = null
)

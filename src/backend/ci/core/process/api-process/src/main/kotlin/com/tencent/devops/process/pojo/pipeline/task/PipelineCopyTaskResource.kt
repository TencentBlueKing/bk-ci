package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyAction
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyTaskResourceStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineDependentResourceType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务资源")
data class PipelineCopyTaskResource(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
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
    @get:Schema(description = "资源复制动作", required = true)
    val copyAction: PipelineCopyAction = PipelineCopyAction.AUTO_FINISH,
    @get:Schema(description = "目标项目ID")
    val targetProjectId: String? = null,
    @get:Schema(description = "目标资源类型")
    val targetResourceType: PipelineDependentResourceType? = null,
    @get:Schema(description = "目标资源ID")
    val targetResourceId: String? = null,
    @get:Schema(description = "目标资源名")
    val targetResourceName: String? = null,
    @get:Schema(description = "目标资源属性")
    val targetResourceProp: PipelineCopyResourceProp? = null,
    @get:Schema(description = "资源状态", required = true)
    val status: PipelineCopyTaskResourceStatus = PipelineCopyTaskResourceStatus.UNPROCESSED,
    @get:Schema(description = "错误信息")
    val errorMessage: String? = null,
    @get:Schema(description = "是否高危资源", required = true)
    val highRisk: Boolean = false,
    @get:Schema(description = "目标项目是否存在同名资源", required = true)
    val targetNameExists: Boolean = false,
    @get:Schema(description = "目标ID是否存在", required = true)
    val targetIdExists: Boolean = false,
    @get:Schema(description = "用户是否已确认处理完成", required = true)
    val confirmed: Boolean = false,
    @get:Schema(description = "流水线引用数", required = true)
    val pipelineReferCount: Int = 0,
    @get:Schema(description = "资源被源项目引用的数量", required = true)
    val sourceProjectReferCount: Int = 0,
    @get:Schema(description = "创建时间")
    val createTime: Long? = null,
    @get:Schema(description = "更新时间")
    val updateTime: Long? = null
)

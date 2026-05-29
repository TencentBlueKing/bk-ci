package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务信息更新")
data class PipelineCopyTaskUpdate(
    @get:Schema(description = "任务ID", required = true)
    val taskId: String,
    @get:Schema(description = "项目ID", required = true)
    val projectId: String,
    @get:Schema(description = "任务名称")
    val taskName: String? = null,
    @get:Schema(description = "目标项目ID")
    val targetProjectId: String? = null,
    @get:Schema(description = "流水线ID处理策略")
    val pipelineCopyStrategy: PipelineCopyStrategy? = null,
    @get:Schema(description = "复制状态")
    val status: PipelineBatchTaskStatus? = null,
    @get:Schema(description = "流水线复制数量")
    val pipelineCount: Int? = null,
    @get:Schema(description = "自动添加的子流水线数量")
    val subPipelineCount: Int? = null,
    @get:Schema(description = "自动排除的PAC数量")
    val pacCount: Int? = null,
    @get:Schema(description = "未处理的资源数")
    val unprocessedCount: Int? = null,
    @get:Schema(description = "高风险资源数")
    val highRiskCount: Int? = null,
    @get:Schema(description = "自动完成的资源数量")
    val autoFinishCount: Int? = null
)

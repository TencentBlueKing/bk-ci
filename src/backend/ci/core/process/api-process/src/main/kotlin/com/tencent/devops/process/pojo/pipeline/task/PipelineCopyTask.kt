package com.tencent.devops.process.pojo.pipeline.task

import com.tencent.devops.process.pojo.pipeline.enums.PipelineBatchTaskStatus
import com.tencent.devops.process.pojo.pipeline.enums.PipelineCopyStrategy
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "流水线复制任务信息")
data class PipelineCopyTask(
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
    @get:Schema(description = "复制状态", required = true)
    val status: PipelineBatchTaskStatus,
    @get:Schema(description = "错误信息")
    val errorMessage: String? = null,
    @get:Schema(description = "流水线复制数量", required = true)
    val pipelineCount: Int = 0,
    @get:Schema(description = "自动添加的子流水线数量", required = true)
    val subPipelineCount: Int = 0,
    @get:Schema(description = "自动排除的PAC数量", required = true)
    val pacCount: Int = 0,
    @get:Schema(description = "未处理的资源数", required = true)
    val unprocessedCount: Int = 0,
    @get:Schema(description = "高风险资源数", required = true)
    val highRiskCount: Int = 0,
    @get:Schema(description = "自动完成的资源数量", required = true)
    val autoFinishCount: Int = 0,
    @get:Schema(description = "创建人", required = true)
    val creator: String,
    @get:Schema(description = "创建时间")
    val createTime: Long? = null,
    @get:Schema(description = "更新时间")
    val updateTime: Long? = null
)

package com.tencent.devops.dispatch.pojo.thirdpartyagent

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "容器级别构建任务")
data class AgentPipelineContainerBuild(
    @get:Schema(title = "构建ID", required = true)
    val buildId: String,
    @get:Schema(title = "项目ID", required = true)
    val projectId: String,
    @get:Schema(title = "流水线ID", required = true)
    val pipelineId: String,
    @get:Schema(title = "作业容器ID", required = true)
    val containerId: String,
    @get:Schema(title = "执行次数", required = true)
    val executeCount: Int,
    @get:Schema(title = "构建状态", required = false)
    var status: String? = null,
    @get:Schema(title = "开始时间", required = true)
    var startTime: LocalDateTime? = null,
    @get:Schema(title = "结束时间", required = true)
    var endTime: LocalDateTime? = null,
    @get:Schema(title = "构建号", required = true)
    val buildNum: Int,
    @get:Schema(title = "触发人", required = true)
    val creator: String,
    @get:Schema(title = "构建包含的任务列表", required = false)
    val tasks: List<AgentPipelineBuildTask>? = null
)

@Schema(title = "构建包含的任务")
data class AgentPipelineBuildTask(
    @get:Schema(title = "任务名称", required = false)
    val taskName: String?,
    @get:Schema(title = "作业容器ID", required = false)
    val vmSeqId: String?,
    @get:Schema(title = "stageId", required = false)
    val stageId: String?,
    @get:Schema(title = "stageNumb")
    var stageNumb: String?
)


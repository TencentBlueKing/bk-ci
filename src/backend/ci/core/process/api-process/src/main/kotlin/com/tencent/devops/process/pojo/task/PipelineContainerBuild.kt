package com.tencent.devops.process.pojo.task

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "容器级别构建任务")
data class PipelineContainerBuild(
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
    val buildNo: Int,
    @get:Schema(title = "触发人", required = true)
    val creator: String
)

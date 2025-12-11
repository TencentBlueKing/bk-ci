package com.tencent.devops.dispatch.pojo.thirdpartyagent

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "第三方构建任务详情")
data class TPAPipelineBuild(
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    @get:Schema(title = "流水线名称")
    val pipelineName: String,
    @get:Schema(title = "jobId")
    val jobId: String?,
    @get:Schema(title = "job名称")
    val jobName: String?,
    @get:Schema(title = "这个job的构建次数")
    val buildCount: Int,
    @get:Schema(title = "最后构建时间")
    val lastBuildTime: LocalDateTime,
    @get:Schema(title = "首次构建时间")
    val firstBuildTime: LocalDateTime,
    @get:Schema(title = "平均耗时")
    val avgTimeInterval: Long?,
    @get:Schema(title = "最后一次构建的containerId")
    val lastContainerId: Long?
)
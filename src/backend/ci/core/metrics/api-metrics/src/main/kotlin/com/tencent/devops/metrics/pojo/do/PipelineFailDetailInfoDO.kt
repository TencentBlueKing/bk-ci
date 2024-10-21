package com.tencent.devops.metrics.pojo.`do`

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "流水线失败详情信息")
data class PipelineFailDetailInfoDO(
    @get:Schema(title = "流水线构建信息")
    val pipelineBuildInfo: PipelineBuildInfoDO,
    @get:Schema(title = "启动用户")
    val startUser: String,
    @get:Schema(title = "启动时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val startTime: LocalDateTime?,
    @get:Schema(title = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val endTime: LocalDateTime?,
    @get:Schema(title = "错误信息")
    val errorInfo: ErrorCodeInfoDO?
)

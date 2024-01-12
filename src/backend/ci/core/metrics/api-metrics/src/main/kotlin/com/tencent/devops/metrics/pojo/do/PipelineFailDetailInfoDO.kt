package com.tencent.devops.metrics.pojo.`do`

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(name = "流水线失败详情信息")
data class PipelineFailDetailInfoDO(
    @Schema(name = "流水线构建信息")
    val pipelineBuildInfo: PipelineBuildInfoDO,
    @Schema(name = "启动用户")
    val startUser: String,
    @Schema(name = "启动时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val startTime: LocalDateTime?,
    @Schema(name = "结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val endTime: LocalDateTime?,
    @Schema(name = "错误信息")
    val errorInfo: ErrorCodeInfoDO?
)

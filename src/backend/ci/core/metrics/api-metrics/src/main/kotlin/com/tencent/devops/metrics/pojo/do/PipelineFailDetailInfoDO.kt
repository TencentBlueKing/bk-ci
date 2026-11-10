package com.tencent.devops.metrics.pojo.`do`

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "流水线失败详情信息")
data class PipelineFailDetailInfoDO(
    @get:Schema(title = "流水线构建信息")
    val pipelineBuildInfo: PipelineBuildInfoDO,
    @get:Schema(title = "启动用户")
    val startUser: String,
    @get:Schema(title = "启动时间（毫秒时间戳）")
    val startTime: Long?,
    @get:Schema(title = "结束时间（毫秒时间戳）")
    val endTime: Long?,
    @get:Schema(title = "错误信息")
    val errorInfo: ErrorCodeInfoDO?
)

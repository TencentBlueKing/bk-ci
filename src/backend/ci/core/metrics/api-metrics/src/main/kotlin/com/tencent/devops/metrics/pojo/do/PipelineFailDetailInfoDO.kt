package com.tencent.devops.metrics.pojo.`do`

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("流水线失败详情信息")
data class PipelineFailDetailInfoDO(
    @ApiModelProperty("流水线构建信息")
    val pipelineBuildInfo: PipelineBuildInfoDO,
    @ApiModelProperty("启动用户")
    val startUser: String,
    @ApiModelProperty("启动时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val startTime: LocalDateTime?,
    @ApiModelProperty("结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    val endTime: LocalDateTime?,
    @ApiModelProperty("错误信息")
    val errorInfo: ErrorCodeInfoDO?
)

package com.tencent.devops.dockerhost.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DockerLogsResponse")
data class DockerLogsResponse(
    @ApiModelProperty("是否结束", required = true)
    val isRunning: Boolean,
    @ApiModelProperty("退出码", required = true)
    val exitCode: Int?,
    @ApiModelProperty("日志", required = true)
    val logs: List<String>
)
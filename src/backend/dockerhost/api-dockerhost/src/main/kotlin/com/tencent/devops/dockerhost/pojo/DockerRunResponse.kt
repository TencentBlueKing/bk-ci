package com.tencent.devops.dockerhost.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("DockerRunResponse")
data class DockerRunResponse(
    @ApiModelProperty("容器Id", required = true)
    val containerId: String,
    @ApiModelProperty("容器启动时间戳", required = true)
    val startTimeStamp: Int
)
package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class FileDestination(
    @ApiModelProperty(value = "目标路径")
    val path: String,
    @ApiModelProperty(value = "执行账号")
    val account: Account,
    @ApiModelProperty(value = "分发目标机器")
    val server: VariableServer
)
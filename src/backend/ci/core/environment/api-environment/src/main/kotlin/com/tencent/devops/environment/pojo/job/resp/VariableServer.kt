package com.tencent.devops.environment.pojo.job.resp

import io.swagger.annotations.ApiModelProperty

data class VariableServer(
    @ApiModelProperty(value = "引用的全局变量名称")
    val variable: String,
    @ApiModelProperty(value = "机器信息")
    val server: Server
)
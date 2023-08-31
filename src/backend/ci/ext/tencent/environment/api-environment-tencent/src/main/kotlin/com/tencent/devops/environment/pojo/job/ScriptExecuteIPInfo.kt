package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("IP结构")
data class ScriptExecuteIPInfo(
    @ApiModelProperty("云区域ID", required = true)
    val bkCloudId: Int,
    @ApiModelProperty("ip地址", required = true)
    val ip: String
)
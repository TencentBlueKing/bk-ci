package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import com.tencent.devops.common.api.pojo.OS

@ApiModel("启动构建参数")
data class StartUpReq(
    @ApiModelProperty("workspace", required = true)
    val workspace: String,
    @ApiModelProperty("yaml", required = true)
    val yaml: String,
    @ApiModelProperty("os", required = true)
    val os: OS,
    @ApiModelProperty("ip", required = true)
    val ip: String,
    @ApiModelProperty("hostname", required = true)
    val hostname: String
)

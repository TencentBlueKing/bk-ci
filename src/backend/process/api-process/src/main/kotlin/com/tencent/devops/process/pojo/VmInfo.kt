package com.tencent.devops.process.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Vm信息")
data class VmInfo(
    @ApiModelProperty("IP", required = false)
    val ip: String?,
    @ApiModelProperty("名称", required = true)
    val name: String
)
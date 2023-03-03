package com.tencent.devops.store.pojo.container

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("操作系统信息")
data class ContainerOsInfo(
    @ApiModelProperty("OS", required = true)
    val os: String,
    @ApiModelProperty("NAME", required = true)
    val name: String
)

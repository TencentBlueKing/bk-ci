package com.tencent.devops.store.pojo.container

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("操作系统")
data class ContainerInfo (
    @ApiModelProperty("OS", required = true)
    var os: String,
    @ApiModelProperty("NAME", required = true)
    val name: String

)
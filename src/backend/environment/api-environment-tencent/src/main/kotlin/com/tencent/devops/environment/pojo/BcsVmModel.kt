package com.tencent.devops.environment.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("BCS容器机型")
data class BcsVmModel(
    @ApiModelProperty("moduleId", required = true)
    val moduleId: String,
    @ApiModelProperty("镜像名称", required = true)
    val moduleName: String,
    @ApiModelProperty("CPU", required = true)
    val resCpu: String,
    @ApiModelProperty("Memory", required = true)
    val resMemory: String
)
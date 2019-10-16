package com.tencent.devops.process.pojo.pipeline

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("子流水线参数键值对")
data class StartUpInfo(
    @ApiModelProperty("子流水线参数名", required = true)
    val id: String,
    @ApiModelProperty("子流水线参数值", required = true)
    val name: Any
)
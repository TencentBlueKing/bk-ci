package com.tencent.devops.plugin.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("动态参数键值对")
data class ParameterListInfo(
    @ApiModelProperty("参数名", required = true)
    val id: String,
    @ApiModelProperty("参数值", required = true)
    val name: String
)
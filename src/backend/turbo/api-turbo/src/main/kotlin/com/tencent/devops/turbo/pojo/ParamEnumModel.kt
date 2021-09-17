package com.tencent.devops.turbo.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("参数枚举模型")
data class ParamEnumModel(
    @ApiModelProperty("参数值")
    val paramValue: Any,
    @ApiModelProperty("参数名")
    val paramName: String,
    @ApiModelProperty("可见范围")
    val visualRange: List<String> = listOf()
)

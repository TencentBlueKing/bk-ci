package com.tencent.devops.turbo.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("参数简易枚举模型")
data class ParamEnumSimpleModel(
    @ApiModelProperty("参数名")
    val paramName: String,
    @ApiModelProperty("可见范围")
    val visualRange: List<String> = listOf()
)

package com.tencent.devops.plugin.pojo.stke

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("更新congfigMap的自定义参数")
data class ConfigMapData(
    @ApiModelProperty("参数名称", required = true)
    val key: String,
    @ApiModelProperty("参数值", required = true)
    val value: String
)

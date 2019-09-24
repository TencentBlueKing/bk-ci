package com.tencent.devops.artifactory.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("版本仓库-元数据")
data class Property(
    @ApiModelProperty("元数据键", required = true)
    val key: String,
    @ApiModelProperty("元数据值", required = true)
    val value: String
)
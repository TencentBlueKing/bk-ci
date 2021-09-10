package com.tencent.devops.turbo.vo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("字段展示视图")
data class TurboDisplayFieldVO(
    @ApiModelProperty("字段名")
    val fieldName: String,
    @ApiModelProperty("字段值")
    val fieldValue: Any?,
    @ApiModelProperty("是否链接")
    val link: Boolean? = false,
    @ApiModelProperty("链接具体值")
    val linkAddress: String? = null
)

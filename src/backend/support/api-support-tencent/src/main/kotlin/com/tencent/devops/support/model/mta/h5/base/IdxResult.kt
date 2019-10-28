package com.tencent.devops.support.model.mta.h5.base

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("指标列表结果")
data class IdxResult(
    @ApiModelProperty("pv")
    val pv: String,
    @ApiModelProperty("uv")
    val uv: String,
    @ApiModelProperty("vv")
    val vv: String,
    @ApiModelProperty("iv")
    val iv: String
)
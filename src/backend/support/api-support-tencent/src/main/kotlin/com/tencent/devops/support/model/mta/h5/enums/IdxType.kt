package com.tencent.devops.support.model.mta.h5.enums

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("指标列表")
enum class IdxType(private val type: String) {
    @ApiModelProperty("浏览量")
    pv("pv"),
    @ApiModelProperty("独立访客")
    uv("uv"),
    @ApiModelProperty("访问次数")
    vv("vv"),
    @ApiModelProperty("独立IP")
    iv("iv");
}
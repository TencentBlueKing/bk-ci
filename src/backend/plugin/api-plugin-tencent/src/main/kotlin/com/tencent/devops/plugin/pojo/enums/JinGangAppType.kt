package com.tencent.devops.plugin.pojo.enums

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("金刚app扫描类型")
enum class JinGangAppType(private val type: Int) {
    @ApiModelProperty("android")
    ANDROID(0),
    @ApiModelProperty("ios")
    IOS(1)
}
package com.tencent.devops.support.model.wechatwork.enums

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("接受者类型")
enum class ReceiverType(private val type: String) {
    @ApiModelProperty("个人")
    single("single"),
    @ApiModelProperty("企业微信群")
    group("group"),
}
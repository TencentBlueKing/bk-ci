package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceItemRelUpdateInfo(
    @ApiModelProperty("服务功能项ID")
    val itemId: String,
    @ApiModelProperty("修改用户")
    val modifierUser: String
)
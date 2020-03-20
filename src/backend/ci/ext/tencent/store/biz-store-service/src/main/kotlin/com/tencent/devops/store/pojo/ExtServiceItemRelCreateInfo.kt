package com.tencent.devops.store.pojo

import io.swagger.annotations.ApiModelProperty

data class ExtServiceItemRelCreateInfo(
    @ApiModelProperty("扩展服务ID")
    val serviceId: String,
    @ApiModelProperty("服务功能项ID")
    val itemId: String,
    @ApiModelProperty("bkServiceId")
    val bkServiceId: Long,
    @ApiModelProperty("添加用户")
    val creatorUser: String,
    @ApiModelProperty("修改用户")
    val modifierUser: String
)
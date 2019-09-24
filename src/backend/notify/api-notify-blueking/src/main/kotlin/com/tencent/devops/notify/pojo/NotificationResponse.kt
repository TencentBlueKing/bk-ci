package com.tencent.devops.notify.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("通用消息类型")
data class NotificationResponse<out T> (
    val id: String,
    @ApiModelProperty("是否成功")
    val success: Boolean,
    @ApiModelProperty("创建时间")
    val createdTime: Long?,
    @ApiModelProperty("更新时间")
    val updatedTime: Long?,
    @ApiModelProperty("总数")
    val contentMD5: String,
    @ApiModelProperty("通知数据")
    val notificationMessage: T
)
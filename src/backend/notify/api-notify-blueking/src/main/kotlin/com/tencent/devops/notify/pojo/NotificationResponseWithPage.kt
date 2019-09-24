package com.tencent.devops.notify.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("通用消息类型-分页")
open class NotificationResponseWithPage<out T>(
    @ApiModelProperty("总数")
    val count: Int,
    @ApiModelProperty("页数")
    val page: Int,
    @ApiModelProperty("每页条数")
    val pageSize: Int,
    @ApiModelProperty("通知列表")
    val data: List<NotificationResponse<T>>
)
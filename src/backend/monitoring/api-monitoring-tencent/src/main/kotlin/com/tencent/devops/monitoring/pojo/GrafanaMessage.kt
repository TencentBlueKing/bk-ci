package com.tencent.devops.monitoring.pojo

import com.tencent.devops.monitoring.pojo.enums.GrafanaNotifyTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("grafana消息")
data class GrafanaMessage(
    @ApiModelProperty("通知类型", required = false)
    val notifyType: GrafanaNotifyTypeEnum?,
    @ApiModelProperty("接收者", required = false)
    val notifyReceivers: MutableSet<String>?,
    @ApiModelProperty("消息内容", required = true)
    val notifyMessage: String
)
package com.tencent.devops.notify.model

import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("消息通知新增请求报文体")
data class NotifyTemplateMessageRequest(
    @ApiModelProperty("模板代码", required = true)
    val templateCode: String,
    @ApiModelProperty("模板名称", required = true)
    val templateName: String,
    @ApiModelProperty("优先级别（-1:低 0:普通 1:高）", allowableValues = "-1,0,1", dataType = "String", required = true)
    val priority: EnumNotifyPriority,
    @ApiModelProperty("通知来源（0:本地业务 1:操作）", allowableValues = "0,1", dataType = "int", required = true)
    val source: EnumNotifySource,
    @ApiModelProperty("消息模板详细信息集合", required = true)
    val msg: List<NotifyTemplateMessage>
)
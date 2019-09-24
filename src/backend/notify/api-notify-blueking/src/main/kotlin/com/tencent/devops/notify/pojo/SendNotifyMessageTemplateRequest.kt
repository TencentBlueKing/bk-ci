package com.tencent.devops.notify.model

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("使用模板发送消息通知请求报文体")
data class SendNotifyMessageTemplateRequest(
    @ApiModelProperty("通知模板代码", required = true)
    val templateCode: String,
    @ApiModelProperty("发送者", required = true)
    val sender: String,
    @ApiModelProperty("通知接收者", required = true)
    val receivers: MutableSet<String> = mutableSetOf(),
    @ApiModelProperty("标题动态参数", required = false)
    val titleParams: Map<String, String>? = null,
    @ApiModelProperty("内容动态参数", required = false)
    val bodyParams: Map<String, String>? = null,
    @ApiModelProperty("邮件抄送接收者", required = false)
    val cc: MutableSet<String>? = null,
    @ApiModelProperty("消息内容", required = false)
    val bcc: MutableSet<String>? = null
)
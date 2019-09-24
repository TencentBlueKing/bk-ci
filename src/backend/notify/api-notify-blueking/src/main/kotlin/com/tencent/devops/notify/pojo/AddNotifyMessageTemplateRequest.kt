package com.tencent.devops.notify.pojo

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.EnumNotifyPriority
import com.tencent.devops.common.notify.enums.EnumNotifySource
import com.tencent.devops.common.notify.enums.NotifyTypeEnum
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("消息通知新增请求报文体")
data class AddNotifyMessageTemplateRequest(
    @ApiModelProperty("模板代码", required = true)
    val templateCode: String,
    @ApiModelProperty("模板名称", required = true)
    val templateName: String,
    @ApiModelProperty("适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）", required = true)
    val notifyTypeScope: ArrayList<NotifyTypeEnum>,
    @ApiModelProperty("标题（邮件和RTX方式必填）", required = false)
    val title: String? = "",
    @ApiModelProperty("消息内容", required = true)
    val body: String,
    @ApiModelProperty("优先级别（-1:低 0:普通 1:高）", allowableValues = "-1,0,1", dataType = "String", required = true)
    val priority: EnumNotifyPriority,
    @ApiModelProperty("通知来源（0:本地业务 1:操作）", allowableValues = "0,1", dataType = "int", required = true)
    val source: EnumNotifySource,
    @ApiModelProperty("邮件格式（邮件方式必填 0:文本 1:html网页）", allowableValues = "0,1", dataType = "int", required = false)
    val bodyFormat: EnumEmailFormat?,
    @ApiModelProperty("邮件类型（邮件方式必填 0:外部邮件 1:内部邮件）", allowableValues = "0,1", dataType = "int", required = false)
    val emailType: EnumEmailType?
)
package com.tencent.devops.notify.model

import com.tencent.devops.common.notify.enums.EnumEmailFormat
import com.tencent.devops.common.notify.enums.EnumEmailType
import com.tencent.devops.common.notify.enums.NotifyTypeEnum
import io.swagger.annotations.ApiModelProperty

data class NotifyTemplateMessage(
    @ApiModelProperty("适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）", required = true)
    val notifyTypeScope: List<NotifyTypeEnum>,
    @ApiModelProperty("标题（邮件和RTX方式必填）", required = false)
    val title: String? = "",
    @ApiModelProperty("消息内容", required = true)
    val body: String,
    @ApiModelProperty("邮件格式（邮件方式必填 0:文本 1:html网页）", allowableValues = "0,1", dataType = "int", required = false)
    val bodyFormat: EnumEmailFormat?,
    @ApiModelProperty("邮件类型（邮件方式必填 0:外部邮件 1:内部邮件）", allowableValues = "0,1", dataType = "int", required = false)
    val emailType: EnumEmailType?
)
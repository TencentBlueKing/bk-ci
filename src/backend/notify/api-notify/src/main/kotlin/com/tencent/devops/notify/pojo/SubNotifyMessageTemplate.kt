package com.tencent.devops.notify.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("消息模板信息")
data class SubNotifyMessageTemplate(
    @ApiModelProperty("适用的通知类型（EMAIL:邮件 RTX:企业微信 WECHAT:微信 SMS:短信）", required = true)
    val notifyTypeScope: List<String>,
    @ApiModelProperty("标题（邮件和RTX方式必填）", required = false)
    val title: String? = "",
    @ApiModelProperty("消息内容", required = true)
    val body: String,
    @ApiModelProperty("邮件格式（邮件方式必填）", required = false)
    val bodyFormat: Int? = null,
    @ApiModelProperty("邮件类型（邮件方式必填）", required = false)
    val emailType: Int? = null,
    @ApiModelProperty("创建人", required = true)
    val creator: String,
    @ApiModelProperty("修改人", required = true)
    val modifier: String,
    @ApiModelProperty("创建日期", required = true)
    val createTime: Long = 0,
    @ApiModelProperty("更新日期", required = true)
    val updateTime: Long = 0
)
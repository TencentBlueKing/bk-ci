package com.tencent.devops.notify.tencentcloud.pojo

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

data class EmailBody(
    @JsonProperty("Destination")
    @get:Schema(title = "收件者信息")
    val destination: List<String>,
    @JsonProperty("FromEmailAddress")
    @get:Schema(title = "发送者信息")
    val fromEmailAddress: String, // QCLOUDTEAM <noreply@mail.qcloud.com>
    @JsonProperty("ReplyToAddresses")
    @get:Schema(title = "回复地址")
    val replyToAddresses: String? = null, // qcloud@tencent.com
    @JsonProperty("Template")
    @get:Schema(title = "邮件模板内容")
    val template: Template,
    @JsonProperty("Subject")
    @get:Schema(title = "主题")
    val subject: String // YourTestSubject
)

data class Template(
    @JsonProperty("TemplateID")
    val templateID: Int? = null, // 100091
    @JsonProperty("TemplateData")
    val templateData: String? = null // {\"code\":\"1234\"}
)

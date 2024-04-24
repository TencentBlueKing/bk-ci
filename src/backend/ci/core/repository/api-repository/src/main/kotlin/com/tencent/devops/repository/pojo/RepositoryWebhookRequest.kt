package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(title = "代码库webhook请求")
data class RepositoryWebhookRequest(
    @get:Schema(title = "请求ID")
    var requestId: String,
    @get:Schema(title = "事件源,工蜂-工蜂ID,github-github id,svn-svn path,p4-p4port")
    val externalId: String,
    @get:Schema(title = "事件类型")
    val eventType: String,
    @get:Schema(title = "代码库类型")
    val repositoryType: String,
    @get:Schema(title = "触发人")
    val triggerUser: String,
    @get:Schema(title = "事件信息")
    val eventMessage: String,
    @get:Schema(title = "请求头")
    val requestHeader: Map<String, String>? = null,
    @get:Schema(title = "请求参数")
    val requestParam: Map<String, String>? = null,
    @get:Schema(title = "请求体")
    val requestBody: String,
    @get:Schema(title = "事件时间")
    val createTime: LocalDateTime
)

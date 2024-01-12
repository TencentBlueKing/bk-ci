package com.tencent.devops.repository.pojo

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "代码库webhook请求")
data class RepositoryWebhookRequest(
    @Schema(description = "请求ID")
    var requestId: String,
    @Schema(description = "事件源,工蜂-工蜂ID,github-github id,svn-svn path,p4-p4port")
    val externalId: String,
    @Schema(description = "事件类型")
    val eventType: String,
    @Schema(description = "代码库类型")
    val repositoryType: String,
    @Schema(description = "触发人")
    val triggerUser: String,
    @Schema(description = "事件信息")
    val eventMessage: String,
    @Schema(description = "请求头")
    val requestHeader: Map<String, String>? = null,
    @Schema(description = "请求参数")
    val requestParam: Map<String, String>? = null,
    @Schema(description = "请求体")
    val requestBody: String,
    @Schema(description = "事件时间")
    val createTime: LocalDateTime
)

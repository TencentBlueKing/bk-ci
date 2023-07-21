package com.tencent.devops.repository.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.LocalDateTime

@ApiModel("代码库webhook请求")
data class RepositoryWebhookRequest(
    @ApiModelProperty("事件Id，网关traceId")
    var requestId: Long? = null,
    @ApiModelProperty("事件源,工蜂-工蜂ID,github-github id,svn-svn path,p4-p4port")
    val externalId: String,
    @ApiModelProperty("事件类型")
    val eventType: String,
    @ApiModelProperty("代码库类型")
    val repositoryType: String,
    @ApiModelProperty("触发人")
    val triggerUser: String,
    @ApiModelProperty("事件信息")
    val eventMessage: String,
    @ApiModelProperty("请求头")
    val requestHeader: Map<String, String>? = null,
    @ApiModelProperty("请求参数")
    val requestParam:Map<String, String>? = null,
    @ApiModelProperty("请求体")
    val requestBody: String,
    @ApiModelProperty("事件时间")
    val createTime: LocalDateTime
)

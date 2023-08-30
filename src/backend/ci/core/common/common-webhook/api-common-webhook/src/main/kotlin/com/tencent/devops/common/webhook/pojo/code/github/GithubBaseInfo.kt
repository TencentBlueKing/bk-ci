package com.tencent.devops.common.webhook.pojo.code.github

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("Github 基础信息")
abstract class GithubBaseInfo(
    @ApiModelProperty("ID")
    open val id: Long,
    @ApiModelProperty("链接[API链接]")
    open val url: String = "",
    @JsonProperty("html_url")
    @ApiModelProperty("链接[网页链接]")
    open val htmlUrl: String,
    @JsonProperty("node_id")
    open val nodeId: String,
    @JsonProperty("created_at")
    open val createdAt: String, // 2022-06-21T08:45:41Z
    @JsonProperty("updated_at")
    open val updatedAt: String
)
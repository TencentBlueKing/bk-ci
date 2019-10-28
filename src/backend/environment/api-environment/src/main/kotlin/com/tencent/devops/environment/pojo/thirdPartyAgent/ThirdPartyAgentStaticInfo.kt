package com.tencent.devops.environment.pojo.thirdPartyAgent

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("第三方接入机-信息")
data class ThirdPartyAgentStaticInfo(
    @ApiModelProperty("Agent Hash ID", required = true)
    val agentId: String,
    @ApiModelProperty("项目ID", required = true)
    val projectId: String,
    @ApiModelProperty("操作系统", required = true)
    val os: String,
    @ApiModelProperty("密钥", required = true)
    val secretKey: String,
    @ApiModelProperty("创建人", required = true)
    val createdUser: String,
    @ApiModelProperty("gateway", required = false)
    val gateway: String?,
    @ApiModelProperty("link", required = true)
    val link: String,
    @ApiModelProperty("script", required = true)
    val script: String,
    @ApiModelProperty("ip", required = true)
    val ip: String
)
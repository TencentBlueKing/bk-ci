package com.tencent.devops.prebuild.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("WebIDE实例状态")
data class IDEInfo(
    @ApiModelProperty("Web IDE实例状态")
    val ideInstanceStatus: Int,
    @ApiModelProperty("Agent实例状态")
    var agentInstanceStatus: Int,
    @ApiModelProperty("机器IP地址")
    val ip: String,
    @ApiModelProperty("ide实例的http服务url")
    val ideURL: String,
    @ApiModelProperty("web ide 版本")
    val ideVersion: String,
    @ApiModelProperty("机器类型")
    val serverType: String,
    @ApiModelProperty("服务器创建时间")
    val serverCreateTime: Long
)

@ApiModel("IDEAgent请求")
data class IDEAgentReq(
    @ApiModelProperty("项目名称", required = true)
    val projectId: String,
    @ApiModelProperty("devcloud服务器IP", required = true)
    val ip: String
)
package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModelProperty

data class NodeAgent(
    @ApiModelProperty(value = "节点IP", required = true)
    val nodeIp: String,
    @ApiModelProperty(value = "节点agent状态", required = true)
    val nodesAgentStatus: Boolean,
    @ApiModelProperty(value = "节点agent版本", required = true)
    val nodesAgentVersion: String
)
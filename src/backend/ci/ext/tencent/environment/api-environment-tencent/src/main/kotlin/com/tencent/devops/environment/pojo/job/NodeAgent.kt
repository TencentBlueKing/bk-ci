package com.tencent.devops.environment.pojo.job

import io.swagger.v3.oas.annotations.media.Schema

data class NodeAgent(
    @get:Schema(title = "节点IP", required = true)
    val nodeIp: String,
    @get:Schema(title = "节点agent状态", description = "0-异常，1-正常，2-未安装", required = true)
    val nodesAgentStatus: Int,
    @get:Schema(title = "节点agent版本")
    val nodesAgentVersion: String? = null
)
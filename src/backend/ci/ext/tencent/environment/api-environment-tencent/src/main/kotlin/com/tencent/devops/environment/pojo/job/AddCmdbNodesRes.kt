package com.tencent.devops.environment.pojo.job

import io.swagger.v3.oas.annotations.media.Schema

data class AddCmdbNodesRes(
    @get:Schema(title = "节点导入状态")
    val nodeStatus: Boolean? = false,
    @get:Schema(title = "节点agent状态列表")
    val nodesAgentList: List<NodeAgent>? = null,
    @get:Schema(title = "agent状态异常节点数")
    val agentAbnormalNodesCount: Int? = 0,
    @get:Schema(title = "未安装agent节点数")
    val agentNotInstallNodesCount: Int? = 0
)
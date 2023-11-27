package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModelProperty

data class AddCmdbNodesRes(
    @ApiModelProperty(value = "节点导入状态")
    val nodeStatus: Boolean? = false,
    @ApiModelProperty(value = "节点agent状态列表")
    val nodesAgentList: List<NodeAgent>? = null,
    @ApiModelProperty(value = "节点导入状态")
    val agentAbnormalNodesCount: Int? = 0
)
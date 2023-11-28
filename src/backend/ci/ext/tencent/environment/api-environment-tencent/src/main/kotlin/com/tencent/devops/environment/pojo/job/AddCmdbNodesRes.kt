package com.tencent.devops.environment.pojo.job

import io.swagger.annotations.ApiModelProperty

data class AddCmdbNodesRes(
    @ApiModelProperty(value = "节点导入状态")
    val nodeStatus: Boolean? = false,
    @ApiModelProperty(value = "节点agent状态列表")
    val nodesAgentList: List<NodeAgent>? = null,
    @ApiModelProperty(value = "agent状态异常节点数")
    val agentAbnormalNodesCount: Int? = 0,
    @ApiModelProperty(value = "未安装agent节点数")
    val agentNotInstallNodesCount: Int? = 0
)
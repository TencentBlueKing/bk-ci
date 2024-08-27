package com.tencent.devops.environment.pojo.job

import io.swagger.v3.oas.annotations.media.Schema

data class AddCmdbNodesRes(
    @get:Schema(title = "节点导入任务状态")
    val nodeStatus: Boolean? = false,
    @get:Schema(title = "节点agent状态列表")
    val nodesAgentList: List<NodeAgent>? = null,
    @get:Schema(title = "导入成功的agent状态异常节点数")
    val agentAbnormalNodesCount: Int? = 0,
    @get:Schema(title = "导入成功的未安装agent节点数")
    val agentNotInstallNodesCount: Int? = 0,
    @get:Schema(title = "导入成功的所有节点数")
    val successfullyImportedNodeCount: Int? = null,
    @get:Schema(title = "导入失败的所有节点数")
    val unsuccessfullyImportedNodeCount: Int? = null
)

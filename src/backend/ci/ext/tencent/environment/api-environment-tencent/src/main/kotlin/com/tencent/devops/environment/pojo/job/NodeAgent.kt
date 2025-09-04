package com.tencent.devops.environment.pojo.job

import io.swagger.v3.oas.annotations.media.Schema

data class NodeAgent(
    @get:Schema(title = "节点IP")
    val nodeIp: String? = null,
    @get:Schema(title = "节点serverId")
    val nodeServerId: Long? = null,
    @get:Schema(title = "节点导入状态", description = "1-已正常导入，2-未导入-互娱机器的二级业务不为CC的业务[或]多ip机器已经有ip在CC中")
    val importStatus: Int? = null,
    @get:Schema(title = "节点agent状态", description = "0-异常，1-正常，2-未安装")
    val nodesAgentStatus: Int? = null,
    @get:Schema(title = "节点agent版本")
    val nodesAgentVersion: String? = null
)

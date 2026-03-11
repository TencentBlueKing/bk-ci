package com.tencent.devops.environment.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "CMDB节点状态信息")
data class CmdbNodeStatusDTO(
    var nodeId: Long,
    var nodeIp: String,
    var serverId: Long? = null,
    var nodeStatus: String
)

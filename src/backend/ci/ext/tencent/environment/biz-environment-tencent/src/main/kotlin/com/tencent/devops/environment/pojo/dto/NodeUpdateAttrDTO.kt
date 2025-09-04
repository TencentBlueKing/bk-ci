package com.tencent.devops.environment.pojo.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "节点中需要更新的部分字段信息")
data class NodeUpdateAttrDTO(
    var nodeIp: String,
    var serverId: Long?,
    var operator: String?,
    var bakOperator: String?,
    var osName: String?
)

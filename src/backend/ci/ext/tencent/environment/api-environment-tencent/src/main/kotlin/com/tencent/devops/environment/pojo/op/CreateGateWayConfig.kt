package com.tencent.devops.environment.pojo.op

import com.fasterxml.jackson.annotation.JsonIgnoreProperties


// 内部创作流节点安装时的网关名称对应
@JsonIgnoreProperties(ignoreUnknown = true)
data class CreateGateWayConfig(
    val devxGateway: String,
    val devnetGateway: String,
    val enableProjects: List<String>
)

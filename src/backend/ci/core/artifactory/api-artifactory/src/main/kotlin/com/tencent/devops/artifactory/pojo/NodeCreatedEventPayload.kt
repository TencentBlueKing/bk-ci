package com.tencent.devops.artifactory.pojo

import com.tencent.bkrepo.repository.pojo.node.NodeDetail
import io.swagger.annotations.ApiModel

@ApiModel("节点创建回调数据")
data class NodeCreatedEventPayload(
    val user: Map<String, String>,
    val node: NodeDetail
)

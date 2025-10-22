package com.tencent.devops.environment.pojo

import io.swagger.v3.oas.annotations.media.Schema

// 节点列表查询信息
@Schema(title = "节点列表查询信息")
data class NodeFetchReq(
    @get:Schema(title = "查询标签列表")
    val tags: List<NodeTagFetchReq>?
)

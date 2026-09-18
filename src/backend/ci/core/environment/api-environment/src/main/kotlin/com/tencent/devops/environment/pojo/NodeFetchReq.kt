package com.tencent.devops.environment.pojo

import io.swagger.v3.oas.annotations.media.Schema

// 节点列表查询信息
@Schema(title = "节点列表查询信息")
data class NodeFetchReq(
    @get:Schema(title = "查询标签列表")
    val tags: List<NodeTagFetchReq>?,
    @get:Schema(title = "大IP查询 （空格、中英文逗号/分号、换行、| 分隔）时，支持解析出 IP 列表")
    val ipListSearch: String? = null
)

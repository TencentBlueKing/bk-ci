package com.tencent.devops.environment.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "添加节点到环境数据")
data class EnvAddNodesData(
    @get:Schema(title = "节点哈希列表", required = false)
    val nodeHashIds: List<String>?,
    @get:Schema(title = "标签列表", required = false)
    val tags: List<NodeTagAddOrDeleteTagItem>?
)

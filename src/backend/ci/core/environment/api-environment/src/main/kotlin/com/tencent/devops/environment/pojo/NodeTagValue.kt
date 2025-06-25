package com.tencent.devops.environment.pojo

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "节点标签")
data class NodeTag(
    @get:Schema(title = "节点标签名ID")
    val tagKeyId: Long,
    @get:Schema(title = "节点标签名")
    val tagKeyName: String,
    @get:Schema(title = "当前节点标签是否支持一个节点多个标签值")
    val tagAllowMulValue: Boolean,
    @get:Schema(title = "节点标签值")
    val tagValues: MutableList<NodeTagValue>
)

@Schema(title = "节点标签值")
data class NodeTagValue(
    @get:Schema(title = "节点值ID")
    val tagValueId: Long,
    @get:Schema(title = "节点值名")
    val tagValueName: String,
    @get:Schema(title = "标签包含的节点数量")
    val nodeCount: Int?
)
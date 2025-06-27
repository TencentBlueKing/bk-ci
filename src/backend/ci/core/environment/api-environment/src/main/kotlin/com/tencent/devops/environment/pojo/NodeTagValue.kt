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

@Schema(title = "标签搜索参数")
data class NodeTagFetchReq(
    @get:Schema(title = "节点标签名ID")
    val tagKeyId: Long,
    @get:Schema(title = "节点标签值列表")
    val tagValues: List<Long>?
)

@Schema(title = "添加或删除节点上标签")
data class UpdateNodeTag(
    @get:Schema(title = "需要操作的节点")
    val nodeId: Long,
    @get:Schema(title = "标签列表")
    val tags: List<NodeTagAddOrDeleteTagItem>
)

data class NodeTagAddOrDeleteTagItem(
    @get:Schema(title = "节点标签名ID")
    val tagKeyId: Long,
    @get:Schema(title = "节点标签值列表")
    val tagValueId: Long
)

@Schema(title = "创建节点标签请求")
data class NodeTagReq(
    @get:Schema(title = "节点标签名")
    val tagKeyName: String,
    @get:Schema(title = "当前节点标签是否支持一个节点多个标签值")
    val tagAllowMulValue: Boolean?,
    @get:Schema(title = "节点标签值")
    val tagValues: List<String>
)

@Schema(title = "修改节点标签请求")
data class UpdateTagReq(
    @get:Schema(title = "被修改的名称")
    val name: String?
)
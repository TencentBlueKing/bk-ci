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
    @get:Schema(title = "是否可以修改")
    var canUpdate: NodeTagCanUpdateType?,
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
    var nodeCount: Int?,
    @get:Schema(title = "是否可以修改")
    var canUpdate: NodeTagCanUpdateType?
)

@Schema(title = "标签是否可以修改区分")
enum class NodeTagCanUpdateType {
    // 内部标签不能修改
    INTERNAL,

    // 可以修改
    TRUE,

    // 不可以修改
    FALSE;

    companion object {
        fun from(internal: Boolean, nodeCount: Int?): NodeTagCanUpdateType =
            if (internal) {
                INTERNAL
            } else {
                if ((nodeCount ?: 0) == 0) {
                    TRUE
                } else {
                    FALSE
                }
            }
    }
}

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
data class NodeTagUpdateReq(
    @get:Schema(title = "节点标签ID")
    val tagKeyId: Long,
    @get:Schema(title = "节点标签名,如果未修改则为空")
    val tagKeyName: String,
    @get:Schema(title = "节点标签值")
    val tagValues: List<NodeTagUpdateReqValue>?
)

@Schema(title = "节点标签值")
data class NodeTagUpdateReqValue(
    @get:Schema(title = "节点值ID,如果是修改，则不为空")
    val tagValueId: Long?,
    @get:Schema(title = "节点值名")
    val tagValueName: String
)
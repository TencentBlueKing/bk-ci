package com.tencent.devops.environment.utils

import com.tencent.devops.environment.pojo.enums.NodeOperatorStatus
import com.tencent.devops.environment.pojo.enums.NodeType

object CmdbNodeUtils {

    private val OPERATOR_STATUS_NODE_TYPES = setOf(NodeType.CMDB.name)

    /**
     * 根据节点导入人 / 主负责人 / 备份负责人计算节点的操作人状态。
     *
     * - 仅 [NodeType.CMDB] 类型节点参与判定，其它类型返回 null。
     * - CMDB 节点的 `bakOperator` 用 `;` 分隔；CC 节点的 `bakOperator` 视为单值。两者差异在此函数内消化。
     * - 当 `createdUser == operator` 或 `createdUser ∈ baks` 时认为合规（[NodeOperatorStatus.NORMAL]），
     *   否则视为"负责人已变更"（[NodeOperatorStatus.OPERATOR_CHANGED]）。
     */
    fun calcOperatorStatus(
        nodeType: String,
        createdUser: String,
        operator: String?,
        bakOperator: String?
    ): NodeOperatorStatus? {
        if (nodeType !in OPERATOR_STATUS_NODE_TYPES) {
            return null
        }
        val bakOperators = bakOperator?.split(";")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

        val ok = createdUser == operator || createdUser in bakOperators
        return if (ok) NodeOperatorStatus.NORMAL else NodeOperatorStatus.OPERATOR_CHANGED
    }

}
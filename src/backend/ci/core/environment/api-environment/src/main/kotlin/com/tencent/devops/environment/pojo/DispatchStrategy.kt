package com.tencent.devops.environment.pojo

import com.tencent.devops.environment.pojo.enums.DefaultStrategyCode
import com.tencent.devops.environment.pojo.enums.LabelOp
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.enums.StrategyType

data class LabelSelector(
    val tagKeyId: Long,
    val op: LabelOp,
    val tagValueIds: Set<Long>
)

data class DispatchStrategyConfig(
    val id: Long?,
    val projectId: String,
    val envId: Long,
    val strategyType: StrategyType,
    val defaultStrategyCode: DefaultStrategyCode?,
    val strategyName: String,
    val scope: StrategyScope,
    val nodeRule: NodeRule,
    val labelSelector: List<LabelSelector>?,
    val enabled: Boolean,
    val priority: Int,
    val createdUser: String,
    val updatedUser: String
) {
    companion object {
        fun buildDefaults(projectId: String, envId: Long, userId: String): List<DispatchStrategyConfig> {
            return DefaultStrategyCode.entries.toTypedArray().mapIndexed { index, code ->
                DispatchStrategyConfig(
                    id = null,
                    projectId = projectId,
                    envId = envId,
                    strategyType = StrategyType.DEFAULT,
                    defaultStrategyCode = code,
                    strategyName = code.displayName,
                    scope = code.scope,
                    nodeRule = code.nodeRule,
                    labelSelector = null,
                    enabled = true,
                    priority = index,
                    createdUser = userId,
                    updatedUser = userId
                )
            }
        }
    }
}

data class EnabledStrategiesWithTags(
    val strategies: List<DispatchStrategyConfig>,
    /** nodeId -> (tagKeyId -> NodeTag) */
    val nodeTagValues: Map<Long, Map<Long, NodeTag>>
)

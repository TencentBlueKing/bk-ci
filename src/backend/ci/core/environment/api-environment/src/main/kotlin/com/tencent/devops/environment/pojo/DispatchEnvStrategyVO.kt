package com.tencent.devops.environment.pojo

import com.tencent.devops.environment.pojo.enums.DefaultStrategyCode
import com.tencent.devops.environment.pojo.enums.LabelOp
import com.tencent.devops.environment.pojo.enums.NodeRule
import com.tencent.devops.environment.pojo.enums.StrategyScope
import com.tencent.devops.environment.pojo.enums.StrategyType
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "标签选择器条件")
data class LabelSelectorVO(
    @get:Schema(title = "标签KeyID", required = true)
    val tagKeyId: Long,
    @get:Schema(title = "操作符", required = true)
    val op: LabelOp,
    @get:Schema(title = "标签ID列表", required = true)
    val tagValueIds: Set<Long>
)

@Schema(title = "调度策略详情")
data class DispatchEnvStrategyVO(
    @get:Schema(title = "策略ID")
    val id: Long,
    @get:Schema(title = "项目ID")
    val projectId: String,
    @get:Schema(title = "环境ID")
    val envId: Long,
    @get:Schema(title = "策略类型")
    val strategyType: StrategyType,
    @get:Schema(title = "默认策略标识")
    val defaultStrategyCode: DefaultStrategyCode?,
    @get:Schema(title = "策略名称")
    val strategyName: String,
    @get:Schema(title = "Agent范围")
    val scope: StrategyScope,
    @get:Schema(title = "节点规则")
    val nodeRule: NodeRule,
    @get:Schema(title = "标签选择器")
    val labelSelector: List<LabelSelectorVO>?,
    @get:Schema(title = "是否启用")
    val enabled: Boolean,
    @get:Schema(title = "优先级")
    val priority: Int,
    @get:Schema(title = "创建者")
    val createdUser: String,
    @get:Schema(title = "修改者")
    val updatedUser: String
)

@Schema(title = "创建自定义调度策略请求")
data class DispatchEnvStrategyCreateReq(
    @get:Schema(title = "策略名称", required = true)
    val strategyName: String,
    @get:Schema(title = "Agent范围", required = true)
    val scope: StrategyScope,
    @get:Schema(title = "节点规则", required = true)
    val nodeRule: NodeRule,
    @get:Schema(title = "标签选择器", required = false)
    val labelSelector: List<LabelSelectorVO>?
)

@Schema(title = "更新调度策略请求")
data class DispatchEnvStrategyUpdateReq(
    @get:Schema(title = "策略名称", required = false)
    val strategyName: String? = null,
    @get:Schema(title = "Agent范围", required = false)
    val scope: StrategyScope? = null,
    @get:Schema(title = "节点规则", required = false)
    val nodeRule: NodeRule? = null,
    @get:Schema(title = "标签选择器", required = false)
    val labelSelector: List<LabelSelectorVO>? = null,
    @get:Schema(title = "是否启用", required = false)
    val enabled: Boolean? = null
)

@Schema(title = "策略排序请求")
data class DispatchEnvStrategyReorderReq(
    @get:Schema(title = "按优先级排列的策略ID列表", required = true)
    val orderedIds: List<Long>
)

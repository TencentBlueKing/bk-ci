package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.api.v2.pojo.ControlPointPosition
import com.tencent.devops.quality.api.v2.pojo.enums.QualityOperation
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-匹配拦截规则原子v2")
data class QualityRuleMatchTask(
    @ApiModelProperty("原子ID", required = true)
    val taskId: String,
    @ApiModelProperty("原子名称", required = true)
    val taskName: String,
    @ApiModelProperty("原子控制阶段", required = true)
    val controlStage: ControlPointPosition,
    @ApiModelProperty("规则列表", required = true)
    val ruleList: List<RuleMatchRule>,
    @ApiModelProperty("阈值列表", required = true)
    val thresholdList: List<RuleThreshold>,
    @ApiModelProperty("审核用户列表", required = true)
    val auditUserList: Set<String>
) {
    @ApiModel("质量红线-拦截规则v2")
    data class RuleMatchRule(
        @ApiModelProperty("规则ID", required = true)
        val ruleHashId: String,
        @ApiModelProperty("规则名称", required = true)
        val ruleName: String,
        @ApiModelProperty("红线匹配的id", required = false)
        val gatewayId: String?
    )

    @ApiModel("质量红线-拦截规则阈值v2")
    data class RuleThreshold(
        @ApiModelProperty("指标ID", required = true)
        val indicatorId: String,
        @ApiModelProperty("指标名称", required = true)
        val indicatorName: String,
        @ApiModelProperty("元数据DATA_ID", required = true)
        val metadataIds: List<String>,
        @ApiModelProperty("关系", required = true)
        val operation: QualityOperation,
        @ApiModelProperty("阈值值大小", required = true)
        val value: String
    )
}
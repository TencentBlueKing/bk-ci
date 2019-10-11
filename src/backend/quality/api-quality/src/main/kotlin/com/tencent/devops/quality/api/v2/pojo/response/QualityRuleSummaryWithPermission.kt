package com.tencent.devops.quality.api.v2.pojo.response

import com.tencent.devops.quality.pojo.RulePermission
import com.tencent.devops.quality.pojo.enum.RuleRange
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-规则简要信息v2")
data class QualityRuleSummaryWithPermission(
    @ApiModelProperty("规则HashId", required = true)
    val ruleHashId: String,
    @ApiModelProperty("规则名称", required = true)
    val name: String,
    @ApiModelProperty("控制点", required = true)
    val controlPoint: RuleSummaryControlPoint,
    @ApiModelProperty("指标列表", required = true)
    val indicatorList: List<RuleSummaryIndicator>,
    @ApiModelProperty("生效范围", required = true)
    val range: RuleRange,
    @ApiModelProperty("包含模板和流水线的生效范围（新）", required = true)
    val rangeSummary: List<RuleRangeSummary>,
    @ApiModelProperty("流水线个数", required = true)
    val pipelineCount: Int,
    @ApiModelProperty("生效流水线执次数", required = true)
    val pipelineExecuteCount: Int,
    @ApiModelProperty("拦截次数", required = true)
    val interceptTimes: Int,
    @ApiModelProperty("是否启用", required = true)
    val enable: Boolean,
    @ApiModelProperty("规则权限", required = true)
    val permissions: RulePermission
) {
        data class RuleSummaryControlPoint(
            val hashId: String,
            val name: String,
            val cnName: String
        )
        data class RuleSummaryIndicator(
            val hashId: String,
            val name: String,
            val cnName: String,
            val operation: String,
            val threshold: String
        )
        data class RuleRangeSummary(
            val id: String,
            val name: String, // 流水线或者模板名称
            val type: String, // 类型，PIPELINE，TEMPLATE
            val lackElements: Collection<String> // 缺少的控制点
        )
}

package com.tencent.devops.quality.api.v2.pojo

import com.tencent.devops.quality.pojo.enum.RuleInterceptResult
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-规则拦截数")
data class QualityRuleIntercept(
    @ApiModelProperty("流水线ID", required = true)
    val pipelineId: String,
    @ApiModelProperty("流水线名称", required = true)
    val pipelineName: String,
    @ApiModelProperty("构建ID", required = true)
    val buildId: String,
    @ApiModelProperty("规则ID", required = true)
    val ruleHashId: String,
    @ApiModelProperty("规则名称", required = true)
    val ruleName: String,
    @ApiModelProperty("拦截时间", required = true)
    val interceptTime: Long,
    @ApiModelProperty("拦截结果", required = true)
    val result: RuleInterceptResult,
    @ApiModelProperty("拦截结果信息列表", required = true)
    val resultMsg: List<QualityRuleInterceptRecord>
)
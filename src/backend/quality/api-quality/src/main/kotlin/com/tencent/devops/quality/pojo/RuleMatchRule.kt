package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-匹配拦截规则")
data class RuleMatchRule(
    @ApiModelProperty("规则ID", required = true)
    val ruleHashId: String,
    @ApiModelProperty("规则名称", required = true)
    val ruleName: String
)
package com.tencent.devops.quality.pojo

import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("质量红线-单条规则拦截结果")
data class RuleCheckSingleResult(
    @ApiModelProperty("规则名称", required = true)
    val ruleName: String,
    @ApiModelProperty("失败信息", required = true)
    val messagePairs: List<Pair<String, String/*detail*/>>
)
package com.tencent.devops.quality.api.v2.pojo.response

import io.swagger.annotations.ApiModel

@ApiModel("匹配单个插件响应")
data class AtomRuleResponse(
    val isControlPoint: Boolean,
    val ruleList: List<QualityRuleMatchTask>
)
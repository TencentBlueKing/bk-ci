package com.tencent.devops.quality.api.v2.pojo.response

import io.swagger.annotations.ApiModel

@ApiModel("质量红线-总览-最近执行历史")
class SummaryInterceptItem(
    val pipelineId: String,
    val pipelineName: String,
    val ruleId: String,
    val ruleName: String,
    val detail: String,
    val interceptTime: String
)
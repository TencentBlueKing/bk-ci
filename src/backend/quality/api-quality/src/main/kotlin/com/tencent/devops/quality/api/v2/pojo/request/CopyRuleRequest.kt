package com.tencent.devops.quality.api.v2.pojo.request

data class CopyRuleRequest(
    val sourceProjectId: String,
    val sourceTemplateId: String,
    val targetProjectId: String,
    val targetTemplateId: String,
    val userId: String
)